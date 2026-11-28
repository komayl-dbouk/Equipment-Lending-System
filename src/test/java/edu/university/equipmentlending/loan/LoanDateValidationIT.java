package edu.university.equipmentlending.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.university.equipmentlending.support.AuthenticatedMockMvcConfiguration;
import java.sql.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthenticatedMockMvcConfiguration.class)
class LoanDateValidationIT {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;

    private long studentId;
    private long equipmentId;

    @BeforeEach
    void createFixtures() {
        cleanOwnedFixtures();
        studentId = jdbc.queryForObject("INSERT INTO student(name,email) VALUES (?,?) RETURNING id",
                Long.class, "Date Boundary Student", "date@els33.example.test");
        equipmentId = jdbc.queryForObject(
                "INSERT INTO equipment(inventory_code,name,category) VALUES (?,?,?) RETURNING id",
                Long.class, "ELS33-DATE-CAMERA", "Date Boundary Camera", "Camera");
    }

    @AfterEach
    void cleanOwnedFixtures() {
        jdbc.update("DELETE FROM loan WHERE equipment_id IN "
                + "(SELECT id FROM equipment WHERE inventory_code LIKE 'ELS33-DATE-%')");
        jdbc.update("DELETE FROM equipment WHERE inventory_code LIKE 'ELS33-DATE-%'");
        jdbc.update("DELETE FROM student WHERE email = 'date@els33.example.test'");
    }

    @ParameterizedTest
    @MethodSource("unsupportedDates")
    void rejectsSignedOrExpandedYearsBeforePersistence(String field, String value) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>(Map.of(
                "studentId", studentId, "equipmentId", equipmentId,
                "borrowingDate", "2030-01-10", "dueDate", "2030-01-13"));
        body.put(field, value);
        var before = jdbc.queryForMap("SELECT * FROM equipment WHERE id = ?", equipmentId);

        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM loan WHERE equipment_id = ?",
                Long.class, equipmentId)).isZero();
        assertThat(jdbc.queryForMap("SELECT * FROM equipment WHERE id = ?", equipmentId)).isEqualTo(before);
    }

    static Stream<Arguments> unsupportedDates() {
        return Stream.of("borrowingDate", "dueDate").flatMap(field ->
                Stream.of("+10000-01-01", "+10000000-01-01", "+999999999-12-31", "-0001-01-01")
                        .map(value -> Arguments.of(field, value)));
    }

    @Test
    void acceptsRepresentableFourDigitBoundaryWithoutAddingAFutureDateLimit() throws Exception {
        Map<String, Object> body = Map.of("studentId", studentId, "equipmentId", equipmentId,
                "borrowingDate", "9999-12-30", "dueDate", "9999-12-31");

        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.borrowingDate").value("9999-12-30"))
                .andExpect(jsonPath("$.dueDate").value("9999-12-31"));

        var row = jdbc.queryForMap("SELECT * FROM loan WHERE equipment_id = ?", equipmentId);
        assertThat(row.get("borrowing_date")).isEqualTo(Date.valueOf("9999-12-30"));
        assertThat(row.get("due_date")).isEqualTo(Date.valueOf("9999-12-31"));
    }
}
