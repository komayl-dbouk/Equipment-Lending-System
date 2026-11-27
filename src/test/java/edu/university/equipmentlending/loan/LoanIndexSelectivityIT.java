package edu.university.equipmentlending.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Supplement the preserved borrowing tests: vary each reference independently. */
@SpringBootTest
class LoanIndexSelectivityIT {
    @Autowired private JdbcTemplate jdbc;
    private long studentOne;
    private long studentTwo;
    private long equipmentOne;
    private long equipmentTwo;
    private long originalLoan;

    @BeforeEach
    void fixtures() {
        cleanup();
        studentOne = student("one@els39-index.example.test");
        studentTwo = student("two@els39-index.example.test");
        equipmentOne = equipment("ELS39-INDEX-ONE");
        equipmentTwo = equipment("ELS39-INDEX-TWO");
        originalLoan = insertLoan(studentOne, equipmentOne);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM loan WHERE equipment_id IN "
                + "(SELECT id FROM equipment WHERE inventory_code LIKE 'ELS39-INDEX-%')");
        jdbc.update("DELETE FROM equipment WHERE inventory_code LIKE 'ELS39-INDEX-%'");
        jdbc.update("DELETE FROM student WHERE email LIKE '%@els39-index.example.test'");
    }

    @Test
    void oneStudentCanHoldTwoDistinctEquipmentItemsWithoutAnInventedQuota() {
        var original = row(originalLoan);
        long second = assertDoesNotThrow(() -> insertLoan(studentOne, equipmentTwo),
                "The active-loan limit belongs to equipment, not to a student");

        assertThat(second).isNotEqualTo(originalLoan);
        assertThat(row(originalLoan)).isEqualTo(original);
        assertThat(row(second)).containsEntry("student_id", studentOne).containsEntry("equipment_id", equipmentTwo)
                .containsEntry("borrowing_date", Date.valueOf("2030-06-10"))
                .containsEntry("due_date", Date.valueOf("2030-06-20")).containsEntry("return_date", null);
        assertThat(ownedLoans()).hasSize(2);
        assertThat(activeCount(equipmentOne)).isEqualTo(1);
        assertThat(activeCount(equipmentTwo)).isEqualTo(1);
    }

    @Test
    void differentStudentCannotCreateASecondActiveLoanForTheSameEquipment() {
        var before = ownedLoans();
        assertThatThrownBy(() -> insertLoan(studentTwo, equipmentOne))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class, failure ->
                        assertThat(failure.getMostSpecificCause()).isInstanceOfSatisfying(PSQLException.class, cause -> {
                            assertThat(cause.getSQLState()).isEqualTo("23505");
                            assertThat(cause.getServerErrorMessage()).isNotNull();
                            assertThat(cause.getServerErrorMessage().getConstraint()).isEqualTo("ux_loan_active_equipment");
                        }));
        assertThat(ownedLoans()).isEqualTo(before);
        assertThat(activeCount(equipmentOne)).isEqualTo(1);
        assertThat(activeCount(equipmentTwo)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM loan WHERE student_id = ?",
                Long.class, studentTwo)).isZero();
    }

    private long student(String email) {
        return jdbc.queryForObject("INSERT INTO student(name,email) VALUES ('Index Student',?) RETURNING id", Long.class, email);
    }
    private long equipment(String code) {
        return jdbc.queryForObject("INSERT INTO equipment(inventory_code,name,category) VALUES (?,'Index Camera','Camera') RETURNING id",
                Long.class, code);
    }
    private long insertLoan(long student, long equipment) {
        return jdbc.queryForObject("INSERT INTO loan(student_id,equipment_id,borrowing_date,due_date) "
                + "VALUES (?,?,DATE '2030-06-10',DATE '2030-06-20') RETURNING id", Long.class, student, equipment);
    }
    private Map<String, Object> row(long id) { return jdbc.queryForMap("SELECT * FROM loan WHERE id = ?", id); }
    private List<Map<String, Object>> ownedLoans() {
        return jdbc.queryForList("SELECT * FROM loan WHERE equipment_id IN "
                + "(SELECT id FROM equipment WHERE inventory_code LIKE 'ELS39-INDEX-%') ORDER BY id");
    }
    private long activeCount(long equipment) {
        return jdbc.queryForObject("SELECT count(*) FROM loan WHERE equipment_id = ? AND return_date IS NULL", Long.class, equipment);
    }
}
