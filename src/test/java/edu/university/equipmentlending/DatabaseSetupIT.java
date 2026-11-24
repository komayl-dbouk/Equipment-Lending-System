package edu.university.equipmentlending;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DatabaseSetupIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migratedPostgreSqlSchemaContainsTheHistoryFoundation() {
        assertThat(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name IN ('student', 'equipment', 'loan') "
                        + "ORDER BY table_name",
                String.class);
        assertThat(tables).containsExactly("equipment", "loan", "student");

        List<String> indexes = jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND tablename = 'loan' ORDER BY indexname",
                String.class);
        assertThat(indexes).contains("ix_loan_equipment_id", "ix_loan_student_id", "ux_loan_active_equipment");
    }
}
