package edu.university.equipmentlending.equipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.university.equipmentlending.equipment.dto.EquipmentWrite;
import edu.university.equipmentlending.shared.error.ApiException;
import edu.university.equipmentlending.support.AuthenticatedMockMvcConfiguration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthenticatedMockMvcConfiguration.class)
class EquipmentPersistenceIT {
    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @ParameterizedTest
    @CsvSource({
            "inventoryCode, number", "inventoryCode, boolean", "inventoryCode, nul",
            "name, number", "name, boolean", "name, nul",
            "category, number", "category, boolean", "category, nul",
            "description, number", "description, boolean", "description, nul"
    })
    void rejectedTextWritesPreserveStoredEquipment(String field, String kind) throws Exception {
        long id = equipmentService.create(write("ORIGINAL")).id();
        var before = jdbcTemplate.queryForMap("SELECT * FROM equipment WHERE id = ?", id);
        Map<String, Object> body = new LinkedHashMap<>(Map.of(
                "inventoryCode", "REPLACEMENT", "name", "Replacement", "category", "Camera",
                "description", "New description"));
        body.put(field, switch (kind) {
            case "number" -> 123;
            case "boolean" -> true;
            default -> "Bad\0Value";
        });
        String json = mapper.writeValueAsString(body);
        mvc.perform(post("/api/equipment").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(put("/api/equipment/" + id).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM equipment", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForMap("SELECT * FROM equipment WHERE id = ?", id)).isEqualTo(before);
    }

    @BeforeEach
    void clearFixtures() {
        jdbcTemplate.update("DELETE FROM loan");
        jdbcTemplate.update("DELETE FROM equipment");
        jdbcTemplate.update("DELETE FROM student");
    }

    @Test
    void databaseEnforcesNormalizedInventoryCodeUniqueness() {
        equipmentService.create(write(" cam-001 "));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO equipment (inventory_code, name, category) VALUES (?, ?, ?)",
                "CAM-001", "Second Camera", "Camera"))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM equipment WHERE inventory_code = 'CAM-001'", Long.class))
                .isEqualTo(1L);
    }

    @Test
    void activeLoanHistoryRejectsDeletionAndPreservesRows() {
        long equipmentId = insertEquipment("CAM-ACTIVE");
        long studentId = insertStudent("active@example.test");
        insertLoan(studentId, equipmentId, null);

        assertHistoryConflict(equipmentId);
        assertRowsPreserved(equipmentId, 1L);
        assertThat(equipmentService.list().get(0).available()).isFalse();
    }

    @Test
    void returnedLoanHistoryRejectsDeletionAndPreservesRows() {
        long equipmentId = insertEquipment("CAM-RETURNED");
        long studentId = insertStudent("returned@example.test");
        insertLoan(studentId, equipmentId, "2026-01-03");

        assertHistoryConflict(equipmentId);
        assertRowsPreserved(equipmentId, 1L);
        assertThat(equipmentService.list().get(0).available()).isTrue();
    }

    @Test
    void unreferencedEquipmentDeletionRemovesOnlyTheTarget() {
        long targetId = insertEquipment("CAM-DELETE");
        long retainedId = insertEquipment("CAM-KEEP");

        equipmentService.delete(targetId);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM equipment WHERE id = ?", Long.class, targetId))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM equipment WHERE id = ?", Long.class, retainedId))
                .isEqualTo(1L);
    }

    private long insertEquipment(String code) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO equipment (inventory_code, name, category) "
                        + "VALUES (?, 'Synthetic Camera', 'Camera') RETURNING id",
                Long.class, code);
    }

    private long insertStudent(String email) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO student (name, email) VALUES ('Synthetic Student', ?) RETURNING id",
                Long.class, email);
    }

    private void insertLoan(long studentId, long equipmentId, String returnDate) {
        jdbcTemplate.update(
                "INSERT INTO loan (student_id, equipment_id, borrowing_date, due_date, return_date) "
                        + "VALUES (?, ?, DATE '2026-01-01', DATE '2026-01-02', ?::date)",
                studentId, equipmentId, returnDate);
    }

    private void assertHistoryConflict(long equipmentId) {
        assertThatThrownBy(() -> equipmentService.delete(equipmentId))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(409);
                    assertThat(exception.getCode()).isEqualTo("EQUIPMENT_HAS_HISTORY");
                });
    }

    private void assertRowsPreserved(long equipmentId, long expectedLoans) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM equipment WHERE id = ?", Long.class, equipmentId))
                .isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM loan WHERE equipment_id = ?", Long.class, equipmentId))
                .isEqualTo(expectedLoans);
    }

    private static EquipmentWrite write(String code) {
        return new EquipmentWrite(code, "Synthetic Camera", "Camera", null);
    }
}
