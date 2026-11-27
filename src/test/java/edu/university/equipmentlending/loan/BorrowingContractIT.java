package edu.university.equipmentlending.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.university.equipmentlending.support.AuthenticatedMockMvcConfiguration;
import java.math.BigInteger;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthenticatedMockMvcConfiguration.class)
class BorrowingContractIT {
    private static final LocalDate BORROWED = LocalDate.of(2030, 1, 10);
    private static final LocalDate DUE = BORROWED.plusDays(3);

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;

    private long studentId;
    private long equipmentId;

    @BeforeEach
    void createFixtures() {
        cleanOwnedFixtures();
        studentId = jdbc.queryForObject("INSERT INTO student(name,email) VALUES (?,?) RETURNING id",
                Long.class, "ELS32 Borrower", "borrower@els32.example.test");
        equipmentId = jdbc.queryForObject(
                "INSERT INTO equipment(inventory_code,name,category) VALUES (?,?,?) RETURNING id",
                Long.class, "ELS32-CAMERA", "ELS32 Camera", "Camera");
    }

    @AfterEach
    void cleanOwnedFixtures() {
        jdbc.execute("DROP TRIGGER IF EXISTS els32_forced_write_failure ON loan");
        jdbc.execute("DROP FUNCTION IF EXISTS els32_fail_loan_write()");
        jdbc.execute("DROP SEQUENCE IF EXISTS els32_write_attempt");
        jdbc.update("DELETE FROM loan WHERE equipment_id IN "
                + "(SELECT id FROM equipment WHERE inventory_code LIKE 'ELS32-%') "
                + "OR student_id IN (SELECT id FROM student WHERE email LIKE '%@els32.example.test')");
        jdbc.update("DELETE FROM equipment WHERE inventory_code LIKE 'ELS32-%'");
        jdbc.update("DELETE FROM student WHERE email LIKE '%@els32.example.test'");
    }

    @Test
    void createsOneCommittedLoanWithExactDtoAndMakesEquipmentUnavailable() throws Exception {
        var studentBefore = jdbc.queryForMap("SELECT * FROM student WHERE id = ?", studentId);
        var equipmentBefore = jdbc.queryForMap("SELECT * FROM equipment WHERE id = ?", equipmentId);
        MvcResult result = borrow(validBody());
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        JsonNode body = json(result);
        assertKeys(body, "id", "studentId", "equipmentId", "borrowingDate", "dueDate", "returnDate");
        assertThat(body.path("id").isIntegralNumber()).isTrue();
        long id = body.path("id").longValue();
        assertThat(id).isPositive();
        assertThat(body.path("studentId").longValue()).isEqualTo(studentId);
        assertThat(body.path("equipmentId").longValue()).isEqualTo(equipmentId);
        assertThat(body.path("borrowingDate").textValue()).isEqualTo(BORROWED.toString());
        assertThat(body.path("dueDate").textValue()).isEqualTo(DUE.toString());
        assertThat(body.path("returnDate").isNull()).isTrue();
        var row = jdbc.queryForMap("SELECT * FROM loan WHERE id = ?", id);
        assertThat(row.get("student_id")).isEqualTo(studentId);
        assertThat(row.get("equipment_id")).isEqualTo(equipmentId);
        assertThat(row.get("borrowing_date")).isEqualTo(Date.valueOf(BORROWED));
        assertThat(row.get("due_date")).isEqualTo(Date.valueOf(DUE));
        assertThat(row.get("return_date")).isNull();
        assertThat(loanRows()).hasSize(1);
        assertThat(jdbc.queryForMap("SELECT * FROM student WHERE id = ?", studentId)).isEqualTo(studentBefore);
        assertThat(jdbc.queryForMap("SELECT * FROM equipment WHERE id = ?", equipmentId)).isEqualTo(equipmentBefore);
        assertAvailable(false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"studentId", "equipmentId"})
    void missingReferencesHaveBusiness404AndDoNotWrite(String field) throws Exception {
        var request = validBody();
        String table = field.equals("studentId") ? "student" : "equipment";
        request.put(field, jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM " + table, Long.class));
        assertError(borrow(request), 404,
                field.equals("studentId") ? "STUDENT_NOT_FOUND" : "EQUIPMENT_NOT_FOUND");
        assertThat(loanRows()).isEmpty();
        assertAvailable(true);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void equalOrReversedDatesAreRejectedWithoutWrites(int offset) throws Exception {
        var request = validBody();
        request.put("dueDate", BORROWED.plusDays(offset).toString());
        assertError(borrow(request), 400, "INVALID_LOAN_DATES");
        assertThat(loanRows()).isEmpty();
        assertAvailable(true);
    }

    @Test
    void activeLoanConflictPreservesOriginalHistory() throws Exception {
        seedLoan(BORROWED.minusDays(2), BORROWED.plusDays(1), null);
        var before = loanRows();
        assertError(borrow(validBody()), 409, "EQUIPMENT_UNAVAILABLE");
        assertThat(loanRows()).isEqualTo(before);
        assertAvailable(false);
    }

    @Test
    void returnedHistoryAllowsReborrowWithoutChangingHistory() throws Exception {
        long previous = seedLoan(BORROWED.minusDays(10), BORROWED.minusDays(7), BORROWED.minusDays(6));
        var before = jdbc.queryForMap("SELECT * FROM loan WHERE id = ?", previous);
        MvcResult result = borrow(validBody());
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(json(result).path("id").longValue()).isNotEqualTo(previous);
        assertThat(loanRows()).hasSize(2);
        assertThat(jdbc.queryForMap("SELECT * FROM loan WHERE id = ?", previous)).isEqualTo(before);
        assertThat(activeLoanCount()).isEqualTo(1);
        assertAvailable(false);
    }

    @Test
    void failedInsertRollsBackAndDoesNotClaimSuccessOrConsumeAvailability() throws Exception {
        jdbc.execute("CREATE SEQUENCE els32_write_attempt");
        jdbc.execute("CREATE FUNCTION els32_fail_loan_write() RETURNS trigger LANGUAGE plpgsql AS $$ "
                + "BEGIN PERFORM nextval('els32_write_attempt'); "
                + "RAISE EXCEPTION USING ERRCODE = 'XX000', MESSAGE = 'els32 forced failure'; "
                + "RETURN NEW; END; $$");
        jdbc.execute("CREATE TRIGGER els32_forced_write_failure AFTER INSERT ON loan "
                + "FOR EACH ROW WHEN (NEW.equipment_id = " + equipmentId + ") "
                + "EXECUTE FUNCTION els32_fail_loan_write()");
        var before = jdbc.queryForMap("SELECT * FROM equipment WHERE id = ?", equipmentId);
        assertError(borrow(validBody()), 500, "INTERNAL_ERROR");
        assertThat(jdbc.queryForObject("SELECT is_called FROM els32_write_attempt", Boolean.class)).isTrue();
        assertThat(loanRows()).isEmpty();
        assertThat(jdbc.queryForMap("SELECT * FROM equipment WHERE id = ?", equipmentId)).isEqualTo(before);
        assertAvailable(true);
    }

    @Test
    void competingRequestsProduceOneCreatedOneConflictAndOneActiveRow() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        Callable<MvcResult> request = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Request start barrier timed out");
            return borrow(validBody());
        };
        try {
            var first = executor.submit(request);
            var second = executor.submit(request);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var results = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertThat(results).extracting(result -> result.getResponse().getStatus())
                    .containsExactlyInAnyOrder(201, 409);
            MvcResult loser = results.stream().filter(r -> r.getResponse().getStatus() == 409)
                    .findFirst().orElseThrow();
            assertError(loser, 409, "EQUIPMENT_UNAVAILABLE");
            assertThat(loanRows()).hasSize(1);
            assertThat(activeLoanCount()).isEqualTo(1);
            assertAvailable(false);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void invalidFieldTypesValuesAndDatesDoNotWrite(String field, Object value) throws Exception {
        var request = validBody();
        request.put(field, value);
        assertError(borrow(request), 400, "VALIDATION_FAILED");
        assertThat(loanRows()).isEmpty();
        assertAvailable(true);
    }

    static Stream<Arguments> invalidFields() {
        Stream<Arguments> ids = Stream.of("studentId", "equipmentId").flatMap(field ->
                Stream.of(0, -1, "1", 1.5, true, new BigInteger("9223372036854775808"),
                        List.of(1), Map.of("id", 1)).map(value -> Arguments.of(field, value)));
        Stream<Arguments> dates = Stream.of("borrowingDate", "dueDate").flatMap(field ->
                Stream.of("invalid", "2030-02-30", "2030-1-10", "2030-01-10T12:00:00",
                        20300110, List.of(2030, 1, 10)).map(value -> Arguments.of(field, value)));
        return Stream.concat(ids, dates);
    }

    @ParameterizedTest
    @MethodSource("missingFields")
    void omittedAndNullFieldsDoNotWrite(String field, boolean omitted) throws Exception {
        var request = validBody();
        if (omitted) request.remove(field);
        else request.put(field, null);
        assertError(borrow(request), 400, "VALIDATION_FAILED");
        assertThat(loanRows()).isEmpty();
    }

    static Stream<Arguments> missingFields() {
        return Stream.of("studentId", "equipmentId", "borrowingDate", "dueDate")
                .flatMap(field -> Stream.of(true, false).map(omitted -> Arguments.of(field, omitted)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "returnDate", "student", "available"})
    void unknownWriteFieldsDoNotWrite(String field) throws Exception {
        var request = validBody();
        request.put(field, "private-els32-value");
        assertError(borrow(request), 400, "VALIDATION_FAILED");
        assertThat(loanRows()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "[]", "{"})
    void malformedOrAbsentBodiesDoNotWrite(String body) throws Exception {
        MvcResult result = mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON)
                .content(body)).andReturn();
        assertError(result, 400, "VALIDATION_FAILED");
        assertThat(loanRows()).isEmpty();
    }

    private Map<String, Object> validBody() {
        return new LinkedHashMap<>(Map.of("studentId", studentId, "equipmentId", equipmentId,
                "borrowingDate", BORROWED.toString(), "dueDate", DUE.toString()));
    }

    private MvcResult borrow(Map<String, Object> body) throws Exception {
        return mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body))).andReturn();
    }

    private JsonNode json(MvcResult result) throws Exception {
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private void assertError(MvcResult result, int expectedStatus, String expectedCode) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        JsonNode body = json(result);
        assertKeys(body, "timestamp", "status", "code", "message", "path", "fieldErrors");
        assertThat(body.path("status").intValue()).isEqualTo(expectedStatus);
        assertThat(body.path("code").textValue()).isEqualTo(expectedCode);
        assertThat(body.path("path").textValue()).isEqualTo("/api/loans");
        assertThat(Instant.parse(body.path("timestamp").textValue())).isNotNull();
        assertThat(body.path("fieldErrors").isObject()).isTrue();
        assertThat(body.toString()).doesNotContain("org.hibernate", "SQLException", "stackTrace",
                "els32 forced failure", "private-els32-value", "@els32.example.test");
    }

    private void assertKeys(JsonNode body, String... expected) {
        Set<String> actual = new HashSet<>();
        body.fieldNames().forEachRemaining(actual::add);
        assertThat(actual).containsExactlyInAnyOrder(expected);
    }

    private List<Map<String, Object>> loanRows() {
        return jdbc.queryForList("SELECT * FROM loan WHERE equipment_id = ? ORDER BY id", equipmentId);
    }

    private long activeLoanCount() {
        return jdbc.queryForObject("SELECT count(*) FROM loan WHERE equipment_id = ? AND return_date IS NULL",
                Long.class, equipmentId);
    }

    private long seedLoan(LocalDate borrowing, LocalDate due, LocalDate returned) {
        return jdbc.queryForObject("INSERT INTO loan(student_id,equipment_id,borrowing_date,due_date,return_date) "
                        + "VALUES (?,?,?,?,?) RETURNING id", Long.class, studentId, equipmentId,
                Date.valueOf(borrowing), Date.valueOf(due), returned == null ? null : Date.valueOf(returned));
    }

    private void assertAvailable(boolean expected) throws Exception {
        MvcResult result = mvc.perform(get("/api/equipment")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode items = json(result);
        assertThat(items.isArray()).isTrue();
        JsonNode item = null;
        for (JsonNode candidate : items) {
            if (candidate.path("id").longValue() == equipmentId) item = candidate;
        }
        assertThat(item).isNotNull();
        assertThat(item.path("available").booleanValue()).isEqualTo(expected);
    }
}
