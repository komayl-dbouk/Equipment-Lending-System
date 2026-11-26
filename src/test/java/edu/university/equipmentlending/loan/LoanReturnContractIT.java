package edu.university.equipmentlending.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Real MVC, transactions and independent PostgreSQL reads; fixtures belong only to ELS-34. */
@SpringBootTest
@AutoConfigureMockMvc
class LoanReturnContractIT {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    private long studentId;
    private long equipmentId;
    private long loanId;

    @BeforeEach
    void fixtures() {
        cleanup();
        studentId = jdbc.queryForObject("INSERT INTO student(name,email) VALUES (?,?) RETURNING id",
                Long.class, "Return Student", "return@els34.example.test");
        equipmentId = jdbc.queryForObject(
                "INSERT INTO equipment(inventory_code,name,category) VALUES (?,?,?) RETURNING id",
                Long.class, "ELS34-CAMERA", "Return Camera", "Camera");
        loanId = jdbc.queryForObject("INSERT INTO loan(student_id,equipment_id,borrowing_date,due_date) "
                        + "VALUES (?,?,DATE '2030-01-10',DATE '2030-01-13') RETURNING id",
                Long.class, studentId, equipmentId);
    }

    @AfterEach
    void cleanup() {
        jdbc.execute("DROP TRIGGER IF EXISTS els34_return_failure ON loan");
        jdbc.execute("DROP FUNCTION IF EXISTS els34_fail_return()");
        jdbc.execute("DROP SEQUENCE IF EXISTS els34_return_attempt");
        jdbc.update("DELETE FROM loan WHERE equipment_id IN "
                + "(SELECT id FROM equipment WHERE inventory_code LIKE 'ELS34-%')");
        jdbc.update("DELETE FROM equipment WHERE inventory_code LIKE 'ELS34-%'");
        jdbc.update("DELETE FROM student WHERE email = 'return@els34.example.test'");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2030-01-10", "2030-01-11", "2030-01-13", "2030-01-20", "2032-02-29", "9999-12-31"})
    void equalLaterLeapAndFutureDatesCommitExactDtoAndRestoreAvailability(String date) throws Exception {
        var before = row();
        var student = jdbc.queryForMap("SELECT * FROM student WHERE id = ?", studentId);
        var item = jdbc.queryForMap("SELECT * FROM equipment WHERE id = ?", equipmentId);
        assertAvailable(false);
        MvcResult result = send(loanId, Map.of("returnDate", date));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = json(result);
        assertKeys(body, "id", "studentId", "equipmentId", "borrowingDate", "dueDate", "returnDate");
        assertThat(body.path("id").longValue()).isEqualTo(loanId);
        assertThat(body.path("studentId").longValue()).isEqualTo(studentId);
        assertThat(body.path("equipmentId").longValue()).isEqualTo(equipmentId);
        assertThat(body.path("borrowingDate").textValue()).isEqualTo("2030-01-10");
        assertThat(body.path("dueDate").textValue()).isEqualTo("2030-01-13");
        assertThat(body.path("returnDate").textValue()).isEqualTo(date);
        before.put("return_date", Date.valueOf(date));
        assertThat(row()).isEqualTo(before);
        assertThat(rows()).hasSize(1);
        assertThat(jdbc.queryForMap("SELECT * FROM student WHERE id = ?", studentId)).isEqualTo(student);
        assertThat(jdbc.queryForMap("SELECT * FROM equipment WHERE id = ?", equipmentId)).isEqualTo(item);
        assertAvailable(true);
    }

    @Test
    void earlierDateIsBusiness400AndPreservesActiveLoan() throws Exception {
        var before = row();
        assertError(send(loanId, Map.of("returnDate", "2030-01-09")), 400, "INVALID_RETURN_DATE");
        assertThat(row()).isEqualTo(before);
        assertAvailable(false);
    }

    @Test
    void missingLoanIs404() throws Exception {
        long absent = jdbc.queryForObject("SELECT max(id)+1 FROM loan", Long.class);
        var before = rows();
        assertError(send(absent, Map.of("returnDate", "2030-01-10")), 404, "LOAN_NOT_FOUND");
        assertThat(rows()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2030-01-10", "2030-01-20"})
    void repeatReturnNeverOverwritesOriginalDate(String secondDate) throws Exception {
        assertThat(send(loanId, Map.of("returnDate", "2030-01-11")).getResponse().getStatus()).isEqualTo(200);
        var before = row();
        assertError(send(loanId, Map.of("returnDate", secondDate)), 409, "LOAN_ALREADY_RETURNED");
        assertThat(row()).isEqualTo(before);
        assertAvailable(true);
    }

    @Test
    void dateOrderValidationPrecedesReturnedStateConflict() throws Exception {
        jdbc.update("UPDATE loan SET return_date = DATE '2030-01-11' WHERE id = ?", loanId);
        var before = row();
        assertError(send(loanId, Map.of("returnDate", "2030-01-09")), 400, "INVALID_RETURN_DATE");
        assertThat(row()).isEqualTo(before);
    }

    @ParameterizedTest
    @MethodSource("invalidDates")
    void malformedDateValuesNeverWrite(Object value) throws Exception {
        var before = row();
        assertError(send(loanId, Map.of("returnDate", value)), 400, "VALIDATION_FAILED");
        assertThat(row()).isEqualTo(before);
        assertAvailable(false);
    }

    static Stream<Object> invalidDates() {
        return Stream.of("", "invalid", "2030-02-29", "2030-02-30", "2030-13-01", "2030-1-10",
                "2030-01-1", "2030-01-10T00:00:00", " 2030-01-10", "2030-01-10 ",
                "+10000-01-01", "+10000000-01-01", "+999999999-12-31", "-0001-01-01",
                "+2030-01-10", "10000-01-01", 20300110, 1.5, true, List.of(2030, 1, 10), Map.of("year", 2030));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "null", "[]", "{", "{}", "{\"returnDate\":null}",
            "{\"returnDate\":\"2030-01-10\",\"id\":1}",
            "{\"returnDate\":\"2030-01-10\",\"equipmentId\":1}",
            "{\"returnDate\":\"2030-01-10\",\"studentId\":1}",
            "{\"returnDate\":\"2030-01-10\",\"borrowingDate\":\"2030-01-10\"}",
            "{\"returnDate\":\"2030-01-10\",\"dueDate\":\"2030-01-13\"}",
            "{\"returnDate\":\"2030-01-10\",\"available\":true}",
            "{\"returnDate\":\"2030-01-10\",\"loan\":{}}"})
    void missingMalformedOrUnknownFieldsAreRejected(String body) throws Exception {
        var before = row();
        assertError(raw(Long.toString(loanId), body), 400, "VALIDATION_FAILED");
        assertThat(row()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "1e2", "9223372036854775808", "-9223372036854775809"})
    void invalidPathIdsAre400(String id) throws Exception {
        var before = rows();
        assertError(raw(id, "{\"returnDate\":\"2030-01-10\"}"), 400, "VALIDATION_FAILED");
        assertThat(rows()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failedFlushOrDeferredCommitRollsBackAndDoesNotClaimSuccess(boolean deferred) throws Exception {
        jdbc.execute("CREATE SEQUENCE els34_return_attempt");
        jdbc.execute("CREATE FUNCTION els34_fail_return() RETURNS trigger LANGUAGE plpgsql AS $$ "
                + "BEGIN PERFORM nextval('els34_return_attempt'); "
                + "RAISE EXCEPTION USING ERRCODE = 'XX000', MESSAGE = 'els34 forced failure'; RETURN NEW; END; $$");
        jdbc.execute((deferred ? "CREATE CONSTRAINT TRIGGER " : "CREATE TRIGGER ")
                + "els34_return_failure AFTER UPDATE ON loan "
                + (deferred ? "DEFERRABLE INITIALLY DEFERRED " : "")
                + "FOR EACH ROW WHEN (NEW.id = " + loanId + ") EXECUTE FUNCTION els34_fail_return()");
        var before = row();
        assertError(send(loanId, Map.of("returnDate", "2030-01-11")), 500, "INTERNAL_ERROR");
        assertThat(jdbc.queryForObject("SELECT is_called FROM els34_return_attempt", Boolean.class)).isTrue();
        assertThat(row()).isEqualTo(before);
        assertAvailable(false);
        jdbc.execute("DROP TRIGGER els34_return_failure ON loan");
        assertThat(send(loanId, Map.of("returnDate", "2030-01-11")).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void borrowReturnReborrowKeepsHistoryAndDeletionProtection() throws Exception {
        jdbc.update("DELETE FROM loan WHERE id = ?", loanId);
        MvcResult first = borrow();
        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        loanId = json(first).path("id").longValue();
        assertThat(send(loanId, Map.of("returnDate", "2030-01-10")).getResponse().getStatus()).isEqualTo(200);
        var history = row();
        assertAvailable(true);
        assertError(mvc.perform(delete("/api/equipment/{id}", equipmentId)).andReturn(),
                409, "EQUIPMENT_HAS_HISTORY");
        MvcResult second = borrow();
        assertThat(second.getResponse().getStatus()).isEqualTo(201);
        assertThat(json(second).path("id").longValue()).isNotEqualTo(loanId);
        assertThat(row()).isEqualTo(history);
        assertThat(rows()).hasSize(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM loan WHERE equipment_id = ? AND return_date IS NULL",
                Long.class, equipmentId)).isEqualTo(1);
        assertAvailable(false);
        assertError(send(loanId, Map.of("returnDate", "2030-01-12")), 409, "LOAN_ALREADY_RETURNED");
        assertAvailable(false);
    }

    @Test
    void twoReturnsWaitingOnEquipmentYieldOneCommittedSuccessAndOneConflict() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        try (Connection blocker = lockEquipment()) {
            var first = executor.submit(() -> { ready.countDown(); return send(loanId, Map.of("returnDate", "2030-01-11")); });
            var second = executor.submit(() -> { ready.countDown(); return send(loanId, Map.of("returnDate", "2030-01-12")); });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            awaitEquipmentWaiters(2);
            blocker.commit();
            var results = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertThat(results).extracting(r -> r.getResponse().getStatus()).containsExactlyInAnyOrder(200, 409);
            MvcResult winner = results.stream().filter(r -> r.getResponse().getStatus() == 200).findFirst().orElseThrow();
            MvcResult loser = results.stream().filter(r -> r.getResponse().getStatus() == 409).findFirst().orElseThrow();
            assertError(loser, 409, "LOAN_ALREADY_RETURNED");
            assertThat(row().get("return_date")).isEqualTo(Date.valueOf(json(winner).path("returnDate").textValue()));
            assertThat(rows()).hasSize(1);
            assertAvailable(true);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void equipmentIsLockedBeforeLoanAndStateIsRereadAfterWaiting() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (Connection blocker = lockEquipment()) {
            var result = executor.submit(() -> send(loanId, Map.of("returnDate", "2030-01-12")));
            awaitEquipmentWaiters(1);
            // The return request must not hold a loan lock while waiting for equipment.
            try (var statement = blocker.createStatement()) {
                statement.execute("SET LOCAL lock_timeout = '2s'");
                statement.executeUpdate("UPDATE loan SET return_date = DATE '2030-01-11' WHERE id = " + loanId);
            }
            blocker.commit();
            assertError(result.get(20, TimeUnit.SECONDS), 409, "LOAN_ALREADY_RETURNED");
            assertThat(row().get("return_date")).isEqualTo(Date.valueOf("2030-01-11"));
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Connection lockEquipment() throws Exception {
        Connection connection = dataSource.getConnection();
        try {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("SELECT id FROM equipment WHERE id = ? FOR UPDATE")) {
                statement.setLong(1, equipmentId);
                statement.executeQuery().close();
            }
            return connection;
        } catch (Exception failure) {
            connection.close();
            throw failure;
        }
    }

    private void awaitEquipmentWaiters(int expected) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        do {
            int waiting = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() "
                    + "AND pid <> pg_backend_pid() AND wait_event_type = 'Lock' AND query ILIKE '%equipment%'", Integer.class);
            if (waiting >= expected) return;
            Thread.sleep(25);
        } while (Instant.now().isBefore(deadline));
        throw new AssertionError("Return requests did not wait for equipment locks");
    }

    private MvcResult borrow() throws Exception {
        return mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(
                Map.of("studentId", studentId, "equipmentId", equipmentId, "borrowingDate", "2030-01-10", "dueDate", "2030-01-13")))).andReturn();
    }

    private MvcResult send(long id, Map<String, Object> body) throws Exception {
        return raw(Long.toString(id), mapper.writeValueAsString(body));
    }

    private MvcResult raw(String id, String body) throws Exception {
        return mvc.perform(post("/api/loans/{id}/return", id).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
    }

    private Map<String, Object> row() { return jdbc.queryForMap("SELECT * FROM loan WHERE id = ?", loanId); }
    private List<Map<String, Object>> rows() {
        return jdbc.queryForList("SELECT * FROM loan WHERE equipment_id = ? ORDER BY id", equipmentId);
    }

    private JsonNode json(MvcResult result) throws Exception {
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private void assertError(MvcResult result, int status, String code) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        JsonNode body = json(result);
        assertKeys(body, "timestamp", "status", "code", "message", "path", "fieldErrors");
        assertThat(body.path("status").intValue()).isEqualTo(status);
        assertThat(body.path("code").textValue()).isEqualTo(code);
        assertThat(body.path("path").textValue()).isEqualTo(result.getRequest().getRequestURI());
        assertThat(Instant.parse(body.path("timestamp").textValue())).isNotNull();
        assertThat(body.path("fieldErrors").isObject()).isTrue();
        assertThat(body.toString()).doesNotContain("els34 forced failure", "SQLException", "org.hibernate", "stackTrace");
    }

    private void assertKeys(JsonNode body, String... expected) {
        Set<String> keys = new HashSet<>();
        body.fieldNames().forEachRemaining(keys::add);
        assertThat(keys).containsExactlyInAnyOrder(expected);
    }

    private void assertAvailable(boolean expected) throws Exception {
        MvcResult result = mvc.perform(get("/api/equipment")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode item = null;
        for (JsonNode candidate : json(result)) if (candidate.path("id").longValue() == equipmentId) item = candidate;
        assertThat(item).isNotNull();
        assertThat(item.path("available").booleanValue()).isEqualTo(expected);
    }
}
