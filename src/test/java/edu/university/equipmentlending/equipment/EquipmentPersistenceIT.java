package edu.university.equipmentlending.equipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.university.equipmentlending.equipment.dto.EquipmentWrite;
import edu.university.equipmentlending.shared.error.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class EquipmentPersistenceIT {
    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
