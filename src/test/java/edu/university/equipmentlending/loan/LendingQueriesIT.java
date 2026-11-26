package edu.university.equipmentlending.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(LendingQueriesIT.FixedClockConfiguration.class)
class LendingQueriesIT {
    private static final LocalDate TODAY = LocalDate.of(2030, 6, 15);

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;

    private long studentOne;
    private long studentTwo;
    private long availableEquipment;
    private long overdueEquipment;
    private long dueTodayEquipment;
    private long returnedEquipment;
    private long overdueLoan;
    private long dueTodayLoan;
    private long returnedLoan;

    @BeforeEach
    void seedFixtures() {
        cleanFixtures();
        studentOne = insertStudent("ELS35 Student One", "one@els35.example.test");
        studentTwo = insertStudent("ELS35 Student Two", "two@els35.example.test");
        availableEquipment = insertEquipment("ELS35-AVAILABLE");
        overdueEquipment = insertEquipment("ELS35-OVERDUE");
        dueTodayEquipment = insertEquipment("ELS35-TODAY");
        returnedEquipment = insertEquipment("ELS35-RETURNED");

        overdueLoan = insertLoan(studentOne, overdueEquipment,
                TODAY.minusDays(7), TODAY.minusDays(1), null);
        dueTodayLoan = insertLoan(studentTwo, dueTodayEquipment, TODAY.minusDays(2), TODAY, null);
        returnedLoan = insertLoan(studentOne, returnedEquipment,
                TODAY.minusDays(9), TODAY.minusDays(3), TODAY.minusDays(2));
    }

    @AfterEach
    void cleanFixtures() {
        jdbc.update("DELETE FROM loan WHERE equipment_id IN "
                + "(SELECT id FROM equipment WHERE inventory_code LIKE 'ELS35-%') "
                + "OR student_id IN (SELECT id FROM student WHERE email LIKE '%@els35.example.test')");
        jdbc.update("DELETE FROM equipment WHERE inventory_code LIKE 'ELS35-%'");
        jdbc.update("DELETE FROM student WHERE email LIKE '%@els35.example.test'");
    }

    private void cleanAllRows() {
        jdbc.update("DELETE FROM loan");
        jdbc.update("DELETE FROM equipment");
        jdbc.update("DELETE FROM student");
    }

    @Test
    void equipmentAvailabilityFiltersReturnDerivedValuesInAscendingIdOrder() throws Exception {
        JsonNode all = array(performGet("/api/equipment"));
        JsonNode available = array(performGet("/api/equipment?available=true"));
        JsonNode unavailable = array(performGet("/api/equipment?available=false"));

        assertAscending(all);
        assertThat(ids(available)).contains(availableEquipment, returnedEquipment)
                .doesNotContain(overdueEquipment, dueTodayEquipment);
        assertThat(ids(unavailable)).containsExactlyInAnyOrder(overdueEquipment, dueTodayEquipment);
        assertEquipmentShape(findById(all, overdueEquipment), false);
        assertEquipmentShape(findById(all, returnedEquipment), true);
    }

    @Test
    void historyFiltersCombineWithAndAndReturnExactDtos() throws Exception {
        JsonNode byStudent = array(performGet("/api/loans?studentId=" + studentOne));
        JsonNode byEquipment = array(performGet("/api/loans?equipmentId=" + overdueEquipment));
        JsonNode combined = array(performGet("/api/loans?studentId=" + studentOne
                + "&equipmentId=" + overdueEquipment));
        JsonNode otherStudent = array(performGet("/api/loans?studentId=" + studentTwo));

        assertAscending(byStudent);
        assertThat(ids(byStudent)).containsExactly(overdueLoan, returnedLoan);
        assertThat(ids(otherStudent)).containsExactly(dueTodayLoan);
        assertThat(ids(byEquipment)).containsExactly(overdueLoan);
        assertThat(ids(combined)).containsExactly(overdueLoan);
        assertLoanShape(byStudent.get(0), overdueLoan, studentOne, overdueEquipment,
                TODAY.minusDays(7), TODAY.minusDays(1), null);
        assertLoanShape(byStudent.get(1), returnedLoan, studentOne, returnedEquipment,
                TODAY.minusDays(9), TODAY.minusDays(3), TODAY.minusDays(2));
        assertLoanShape(combined.get(0), overdueLoan, studentOne, overdueEquipment,
                TODAY.minusDays(7), TODAY.minusDays(1), null);
    }

    @Test
    void unmatchedFiltersAndEmptyDataReturnEmptyArrays() throws Exception {
        assertThat(array(performGet("/api/loans?studentId=" + Long.MAX_VALUE)).size()).isZero();
        assertThat(array(performGet("/api/loans?equipmentId=" + Long.MAX_VALUE)).size()).isZero();
        assertThat(array(performGet("/api/loans?studentId=" + studentOne
                + "&equipmentId=" + dueTodayEquipment)).size()).isZero();

        cleanAllRows();
        assertThat(array(performGet("/api/equipment")).size()).isZero();
        assertThat(array(performGet("/api/loans")).size()).isZero();
        assertThat(array(performGet("/api/loans/overdue")).size()).isZero();
    }

    @Test
    void overdueIncludesOnlyUnreturnedLoansStrictlyBeforeFixedToday() throws Exception {
        JsonNode overdue = array(performGet("/api/loans/overdue"));

        assertThat(ids(overdue)).containsExactly(overdueLoan);
        assertLoanShape(overdue.get(0), overdueLoan, studentOne, overdueEquipment,
                TODAY.minusDays(7), TODAY.minusDays(1), null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "TRUE", "false ", "yes", "1"})
    void invalidAvailabilityTextReturnsValidationError(String value) throws Exception {
        assertValidationError(performGet("/api/equipment?available=" + value), "/api/equipment");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "-1", "+1", "not-an-id", "9223372036854775808"})
    void invalidHistoryIdsReturnValidationError(String value) throws Exception {
        assertValidationError(performGet("/api/loans?studentId=" + value), "/api/loans");
        assertValidationError(performGet("/api/loans?equipmentId=" + value), "/api/loans");
    }

    private long insertStudent(String name, String email) {
        return jdbc.queryForObject("INSERT INTO student(name,email) VALUES (?,?) RETURNING id",
                Long.class, name, email);
    }

    private long insertEquipment(String inventoryCode) {
        return jdbc.queryForObject(
                "INSERT INTO equipment(inventory_code,name,category) VALUES (?,?,?) RETURNING id",
                Long.class, inventoryCode, inventoryCode + " item", "Camera");
    }

    private long insertLoan(long studentId, long equipmentId, LocalDate borrowed,
            LocalDate due, LocalDate returned) {
        return jdbc.queryForObject("INSERT INTO loan(student_id,equipment_id,borrowing_date,due_date,return_date) "
                        + "VALUES (?,?,?,?,?) RETURNING id", Long.class, studentId, equipmentId,
                Date.valueOf(borrowed), Date.valueOf(due), returned == null ? null : Date.valueOf(returned));
    }

    private MvcResult performGet(String path) throws Exception {
        return mvc.perform(get(path)).andReturn();
    }

    private JsonNode array(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.isArray()).isTrue();
        return body;
    }

    private void assertValidationError(MvcResult result, String path) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        JsonNode body = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("code").textValue()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.path("path").textValue()).isEqualTo(path);
    }

    private static List<Long> ids(JsonNode array) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .map(item -> item.path("id").longValue()).toList();
    }

    private static void assertAscending(JsonNode array) {
        assertThat(ids(array)).isSorted();
    }

    private static JsonNode findById(JsonNode array, long id) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .filter(item -> item.path("id").longValue() == id).findFirst().orElseThrow();
    }

    private static void assertEquipmentShape(JsonNode body, boolean available) {
        assertKeys(body, "id", "inventoryCode", "name", "category", "description", "available");
        assertThat(body.path("available").booleanValue()).isEqualTo(available);
    }

    private static void assertLoanShape(JsonNode body, long id, long studentId, long equipmentId,
            LocalDate borrowingDate, LocalDate dueDate, LocalDate returnDate) {
        assertKeys(body, "id", "studentId", "equipmentId", "borrowingDate", "dueDate", "returnDate");
        assertThat(body.path("id").longValue()).isEqualTo(id);
        assertThat(body.path("studentId").longValue()).isEqualTo(studentId);
        assertThat(body.path("equipmentId").longValue()).isEqualTo(equipmentId);
        assertThat(body.path("borrowingDate").textValue()).isEqualTo(borrowingDate.toString());
        assertThat(body.path("dueDate").textValue()).isEqualTo(dueDate.toString());
        if (returnDate == null) assertThat(body.path("returnDate").isNull()).isTrue();
        else assertThat(body.path("returnDate").textValue()).isEqualTo(returnDate.toString());
    }

    private static void assertKeys(JsonNode body, String... expected) {
        Set<String> actual = new HashSet<>();
        body.fieldNames().forEachRemaining(actual::add);
        assertThat(actual).containsExactlyInAnyOrder(expected);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedQueryClock() {
            // UTC is still the previous day; replacing the Beirut date with UTC must fail.
            return Clock.fixed(Instant.parse("2030-06-14T21:30:00Z"), ZoneId.of("Asia/Beirut"));
        }
    }
}
