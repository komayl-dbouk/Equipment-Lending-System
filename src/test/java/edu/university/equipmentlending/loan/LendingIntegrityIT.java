package edu.university.equipmentlending.loan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Missing cross-feature races and database bypass invariants; no production transaction wraps a test. */
@SpringBootTest
@AutoConfigureMockMvc
class LendingIntegrityIT {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DataSource dataSource;
    private long studentId;
    private long equipmentId;

    @BeforeEach
    void fixtures() {
        cleanup();
        studentId = jdbc.queryForObject("INSERT INTO student(name,email) VALUES (?,?) RETURNING id",
                Long.class, "Integrity Student", "integrity@els36.example.test");
        equipmentId = jdbc.queryForObject("INSERT INTO equipment(inventory_code,name,category) VALUES (?,?,?) RETURNING id",
                Long.class, "ELS36-INTEGRITY", "Integrity Camera", "Camera");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM loan WHERE equipment_id IN (SELECT id FROM equipment WHERE inventory_code = 'ELS36-INTEGRITY')");
        jdbc.update("DELETE FROM equipment WHERE inventory_code = 'ELS36-INTEGRITY'");
        jdbc.update("DELETE FROM student WHERE email = 'integrity@els36.example.test'");
    }

    @Test
    void borrowVersusReturnAcceptsEitherSerialOrderAndPreservesHistory() throws Exception {
        long originalId = seedLoan(null);
        var original = row(originalId);
        var student = jdbc.queryForMap("SELECT * FROM student WHERE id = ?", studentId);
        var equipment = jdbc.queryForMap("SELECT * FROM equipment WHERE id = ?", equipmentId);

        List<MvcResult> results = contend(this::borrow, () -> mvc.perform(post("/api/loans/{id}/return", originalId)
                .contentType("application/json").content("{\"returnDate\":\"2030-06-15\"}")).andReturn());

        MvcResult borrowed = results.get(0);
        assertThat(results.get(1).getResponse().getStatus()).isEqualTo(200);
        assertThat(borrowed.getResponse().getStatus()).isIn(201, 409);
        original.put("return_date", Date.valueOf("2030-06-15"));
        assertThat(row(originalId)).isEqualTo(original);
        boolean created = borrowed.getResponse().getStatus() == 201;
        if (created) {
            long newId = json(borrowed).path("id").longValue();
            assertThat(newId).isNotEqualTo(originalId);
            assertActiveRow(newId);
        } else {
            assertError(borrowed, "EQUIPMENT_UNAVAILABLE");
        }
        assertThat(rows()).hasSize(created ? 2 : 1);
        assertThat(activeCount()).isEqualTo(created ? 1 : 0);
        assertAvailable(!created);
        assertThat(jdbc.queryForMap("SELECT * FROM student WHERE id = ?", studentId)).isEqualTo(student);
        assertThat(jdbc.queryForMap("SELECT * FROM equipment WHERE id = ?", equipmentId)).isEqualTo(equipment);
        assertNoOrphans();
        System.out.println("ELS36_RACE borrow-return borrow=" + borrowed.getResponse().getStatus()
                + " return=200 active=" + activeCount() + " history=" + rows().size());
    }

    @Test
    void borrowVersusDeleteAcceptsEitherSerialOrderWithoutOrphans() throws Exception {
        var student = jdbc.queryForMap("SELECT * FROM student WHERE id = ?", studentId);
        List<MvcResult> results = contend(this::borrow,
                () -> mvc.perform(delete("/api/equipment/{id}", equipmentId)).andReturn());
        MvcResult borrowed = results.get(0);
        MvcResult deleted = results.get(1);
        if (borrowed.getResponse().getStatus() == 201) {
            assertThat(deleted.getResponse().getStatus()).isEqualTo(409);
            assertError(deleted, "EQUIPMENT_HAS_HISTORY");
            assertThat(rows()).hasSize(1);
            assertActiveRow(json(borrowed).path("id").longValue());
            assertThat(activeCount()).isEqualTo(1);
            assertAvailable(false);
        } else {
            assertThat(borrowed.getResponse().getStatus()).isEqualTo(404);
            assertError(borrowed, "EQUIPMENT_NOT_FOUND");
            assertThat(deleted.getResponse().getStatus()).isEqualTo(204);
            assertThat(deleted.getResponse().getContentAsString()).isEmpty();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM equipment WHERE id = ?", Long.class, equipmentId)).isZero();
            assertThat(rows()).isEmpty();
        }
        assertThat(jdbc.queryForMap("SELECT * FROM student WHERE id = ?", studentId)).isEqualTo(student);
        assertNoOrphans();
        System.out.println("ELS36_RACE borrow-delete borrow=" + borrowed.getResponse().getStatus()
                + " delete=" + deleted.getResponse().getStatus() + " active=" + activeCount());
    }

    @Test
    void databaseUniquenessCannotBeBypassedAndReturnedHistoryAllowsNewActiveRow() {
        long first = seedLoan(null);
        var before = rows();
        assertSqlState("23505", () -> seedLoan(null));
        assertThat(rows()).isEqualTo(before);
        jdbc.update("UPDATE loan SET return_date = DATE '2030-06-15' WHERE id = ?", first);
        var history = row(first);
        long second = seedLoan(null);
        assertThat(second).isNotEqualTo(first);
        assertThat(row(first)).isEqualTo(history);
        assertThat(rows()).hasSize(2);
        assertThat(activeCount()).isEqualTo(1);
    }

    @Test
    void restrictiveReferencesAndDateChecksRejectDirectWritesWithoutPartialChanges() {
        long id = seedLoan("2030-06-15");
        var before = rows();
        assertSqlState("23503", () -> jdbc.update("DELETE FROM equipment WHERE id = ?", equipmentId));
        assertSqlState("23503", () -> jdbc.update("DELETE FROM student WHERE id = ?", studentId));
        assertSqlState("23503", () -> jdbc.update("INSERT INTO loan(student_id,equipment_id,borrowing_date,due_date) "
                + "VALUES (?, ?, DATE '2030-06-10', DATE '2030-06-16')", studentId, Long.MAX_VALUE));
        assertSqlState("23503", () -> jdbc.update("INSERT INTO loan(student_id,equipment_id,borrowing_date,due_date) "
                + "VALUES (?, ?, DATE '2030-06-10', DATE '2030-06-16')", Long.MAX_VALUE, equipmentId));
        assertSqlState("23514", () -> jdbc.update("UPDATE loan SET due_date = borrowing_date WHERE id = ?", id));
        assertSqlState("23514", () -> jdbc.update("UPDATE loan SET return_date = borrowing_date - 1 WHERE id = ?", id));
        assertThat(rows()).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM student WHERE id = ?", Long.class, studentId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM equipment WHERE id = ?", Long.class, equipmentId)).isEqualTo(1);
        assertNoOrphans();
    }

    private List<MvcResult> contend(Callable<MvcResult> first, Callable<MvcResult> second) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        var observer = Executors.newSingleThreadScheduledExecutor();
        try (Connection blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            try (var lock = blocker.prepareStatement("SELECT id FROM equipment WHERE id = ? FOR UPDATE")) {
                lock.setLong(1, equipmentId);
                lock.executeQuery().close();
            }
            Callable<MvcResult> requestOne = () -> { ready.countDown(); assertThat(start.await(10, TimeUnit.SECONDS)).isTrue(); return first.call(); };
            Callable<MvcResult> requestTwo = () -> { ready.countDown(); assertThat(start.await(10, TimeUnit.SECONDS)).isTrue(); return second.call(); };
            var one = executor.submit(requestOne);
            var two = executor.submit(requestTwo);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            var bothWaiting = new java.util.concurrent.CompletableFuture<Void>();
            observer.scheduleWithFixedDelay(() -> {
                try {
                    int count = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() "
                            + "AND pid <> pg_backend_pid() AND wait_event_type = 'Lock' AND query ILIKE '%equipment%'", Integer.class);
                    if (count >= 2) bothWaiting.complete(null);
                } catch (Exception failure) { bothWaiting.completeExceptionally(failure); }
            }, 0, 25, TimeUnit.MILLISECONDS);
            start.countDown();
            bothWaiting.get(10, TimeUnit.SECONDS);
            blocker.commit();
            return List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            observer.shutdownNow();
            executor.shutdownNow();
            assertThat(observer.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private long seedLoan(String returned) {
        return jdbc.queryForObject("INSERT INTO loan(student_id,equipment_id,borrowing_date,due_date,return_date) "
                + "VALUES (?, ?, DATE '2030-06-10', DATE '2030-06-14', ?) RETURNING id", Long.class,
                studentId, equipmentId, returned == null ? null : Date.valueOf(returned));
    }

    private MvcResult borrow() throws Exception {
        return mvc.perform(post("/api/loans").contentType("application/json").content(mapper.writeValueAsString(Map.of(
                "studentId", studentId, "equipmentId", equipmentId, "borrowingDate", "2030-06-15", "dueDate", "2030-06-20")))).andReturn();
    }

    private void assertActiveRow(long id) {
        assertThat(row(id)).containsEntry("student_id", studentId).containsEntry("equipment_id", equipmentId)
                .containsEntry("borrowing_date", Date.valueOf("2030-06-15"))
                .containsEntry("due_date", Date.valueOf("2030-06-20")).containsEntry("return_date", null);
    }
    private Map<String, Object> row(long id) { return jdbc.queryForMap("SELECT * FROM loan WHERE id = ?", id); }
    private List<Map<String, Object>> rows() { return jdbc.queryForList("SELECT * FROM loan WHERE equipment_id = ? ORDER BY id", equipmentId); }
    private long activeCount() { return jdbc.queryForObject("SELECT count(*) FROM loan WHERE equipment_id = ? AND return_date IS NULL", Long.class, equipmentId); }
    private void assertNoOrphans() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM loan l LEFT JOIN equipment e ON e.id=l.equipment_id "
                + "LEFT JOIN student s ON s.id=l.student_id WHERE e.id IS NULL OR s.id IS NULL", Long.class)).isZero();
    }
    private JsonNode json(MvcResult result) throws Exception { return mapper.readTree(result.getResponse().getContentAsString()); }
    private void assertError(MvcResult result, String code) throws Exception {
        assertThat(json(result).path("code").textValue()).isEqualTo(code);
        assertThat(json(result).path("path").textValue()).isEqualTo(result.getRequest().getRequestURI());
    }
    private void assertAvailable(boolean expected) throws Exception {
        MvcResult result = mvc.perform(get("/api/equipment?available=" + expected)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(result).findValues("id")).anySatisfy(id -> assertThat(id.longValue()).isEqualTo(equipmentId));
        for (JsonNode item : json(result)) assertThat(item.path("available").booleanValue()).isEqualTo(expected);
    }
    private void assertSqlState(String state, Runnable write) {
        assertThatThrownBy(write::run).isInstanceOfSatisfying(DataIntegrityViolationException.class, failure -> {
            Throwable cause = failure.getMostSpecificCause();
            assertThat(cause).isInstanceOf(SQLException.class);
            assertThat(((SQLException) cause).getSQLState()).isEqualTo(state);
        });
    }
}
