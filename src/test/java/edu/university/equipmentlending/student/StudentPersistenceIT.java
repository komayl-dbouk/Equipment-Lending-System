package edu.university.equipmentlending.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.university.equipmentlending.shared.error.BusinessConflictException;
import edu.university.equipmentlending.student.dto.StudentWrite;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Spring MVC stack and committed PostgreSQL state; run with -Pdatabase-test verify. */
@SpringBootTest
@AutoConfigureMockMvc
class StudentPersistenceIT {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StudentService service;
    @Autowired private PlatformTransactionManager transactionManager;

    // No test-wide transaction: independent SQL reads must observe actual commits/rollbacks.
    @BeforeEach
    @AfterEach
    void cleanOwnedFixtures() {
        jdbc.update("DELETE FROM student WHERE email LIKE '%@els29.example.test'");
    }

    @Test
    void registerRetrieveAndReplacePersistTheExpectedValuesAndStableId() throws Exception {
        MvcResult created = mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON)
                        .content(body("  Test Student One \t", " Borrower.One@ELS29.Example.Test ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.name").value("Test Student One"))
                .andExpect(jsonPath("$.email").value("borrower.one@els29.example.test"))
                .andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).get("id").longValue();
        assertThat(id).isPositive();
        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/api/students/" + id);
        assertRow(id, "Test Student One", "borrower.one@els29.example.test");

        mvc.perform(get("/api/students/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Test Student One"))
                .andExpect(jsonPath("$.email").value("borrower.one@els29.example.test"));

        mvc.perform(put("/api/students/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(body(" Replacement Name ", " NEW@ELS29.Example.Test ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Replacement Name"))
                .andExpect(jsonPath("$.email").value("new@els29.example.test"));
        assertRow(id, "Replacement Name", "new@els29.example.test");
        assertThat(ownedCount()).isEqualTo(1);
    }

    @Test
    void updatingWithOwnPaddedCaseVariedEmailSucceeds() throws Exception {
        long id = seed("Original Name", "one@els29.example.test");
        mvc.perform(put("/api/students/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(body(" Updated Name ", " ONE@ELS29.EXAMPLE.TEST ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("one@els29.example.test"));
        assertRow(id, "Updated Name", "one@els29.example.test");
        assertThat(ownedCount()).isEqualTo(1);
    }

    @Test
    void duplicateNormalizedRegistrationReturns409AndPreservesOriginalRow() throws Exception {
        long id = seed("Original Name", "one@els29.example.test");
        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Rejected Name", " ONE@ELS29.EXAMPLE.TEST ")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.status").value(409));
        assertRow(id, "Original Name", "one@els29.example.test");
        assertThat(ownedCount()).isEqualTo(1);
        // A subsequent read proves requests can still use the database after the failed write.
        mvc.perform(get("/api/students/" + id)).andExpect(status().isOk());
    }

    @Test
    void anotherStudentsEmailRollsBackTheEntireReplacement() throws Exception {
        long target = seed("Original Name", "original@els29.example.test");
        long owner = seed("Email Owner", "used@els29.example.test");
        mvc.perform(put("/api/students/" + target).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Must Not Persist", " USED@ELS29.EXAMPLE.TEST ")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
        assertRow(target, "Original Name", "original@els29.example.test");
        assertRow(owner, "Email Owner", "used@els29.example.test");
        assertThat(ownedCount()).isEqualTo(2);
    }

    @Test
    void postgresqlConstraintRejectsDuplicateCanonicalEmailFromAnIndependentWriter() {
        long id = seed("Original Name", "one@els29.example.test");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO student (name, email) VALUES (?, ?)",
                "Rejected Name", "one@els29.example.test"))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class, exception ->
                        assertThat(exception.getRootCause()).isInstanceOfSatisfying(SQLException.class,
                                sql -> assertThat(sql.getSQLState()).isEqualTo("23505")));
        assertRow(id, "Original Name", "one@els29.example.test");
        assertThat(ownedCount()).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT conname FROM pg_constraint "
                + "WHERE conrelid = 'student'::regclass AND contype = 'u'", String.class))
                .contains("student_email_key");
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void rejectedBodiesLeavePersistedRowsUnchanged(String body) throws Exception {
        long id = seed("Original Name", "original@els29.example.test");
        long before = jdbc.queryForObject("SELECT count(*) FROM student", Long.class);
        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(put("/api/students/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertRow(id, "Original Name", "original@els29.example.test");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM student", Long.class)).isEqualTo(before);
    }

    static Stream<String> invalidBodies() {
        return Stream.of(
                "{\"name\":\"Bad\\u0000Name\",\"email\":\"one@els29.example.test\"}",
                "{\"name\":\"Test Student\",\"email\":\"one\\u0000@els29.example.test\"}",
                "{\"name\":\"\u2003\",\"email\":\"one@els29.example.test\"}",
                "{\"name\":\" \",\"email\":\"one@els29.example.test\"}",
                "{\"name\":\"Test Student\",\"email\":\"not-an-email\"}",
                "{\"name\":\" " + "n".repeat(120) + "\",\"email\":\"one@els29.example.test\"}",
                "{\"name\":\"Test Student\",\"email\":\"" + " ".repeat(233) + "one@els29.example.test\"}",
                "{\"name\":\"Test Student\",\"email\":\"one@els29.example.test\",\"id\":999}",
                "{\"name\":\"Test Student\",\"email\":\"one@els29.example.test\",\"loans\":[]}",
                "{\"name\":123,\"email\":\"one@els29.example.test\"}",
                "{\"name\":\"Test Student\"}",
                "{\"email\":\"one@els29.example.test\"}", "null");
    }

    @Test
    void missingAndInvalidIdsDoNotCreateOrAlterStudents() throws Exception {
        long id = seed("Original Name", "original@els29.example.test");
        for (String invalid : List.of("0", "-1", "bad-id", "9223372036854775808")) {
            mvc.perform(get("/api/students/" + invalid)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
            mvc.perform(put("/api/students/" + invalid).contentType(MediaType.APPLICATION_JSON)
                            .content(body("Rejected Name", "rejected@els29.example.test")))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
        long missing = jdbc.queryForObject("SELECT coalesce(max(id), 0) + 1 FROM student", Long.class);
        mvc.perform(get("/api/students/" + missing)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
        mvc.perform(put("/api/students/" + missing).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Rejected Name", "rejected@els29.example.test")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
        assertRow(id, "Original Name", "original@els29.example.test");
        assertThat(ownedCount()).isEqualTo(1);
    }

    @Test
    void aDuplicateWriteDoesNotAbortAnExistingCallerTransaction() {
        long id = seed("Original Name", "one@els29.example.test");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThatThrownBy(() -> service.register(new StudentWrite("Rejected Name", " ONE@ELS29.EXAMPLE.TEST ")))
                    .isInstanceOf(BusinessConflictException.class);
            assertRow(id, "Original Name", "one@els29.example.test");
            assertThat(status.isRollbackOnly()).isFalse();
        });
        assertThat(ownedCount()).isEqualTo(1);
    }

    @Test
    void competingRegistrationsYieldOneCreatedOneConflictAndOneCanonicalRow() throws Exception {
        List<MvcResult> results = race(
                () -> mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Test Student One", " RACE@ELS29.EXAMPLE.TEST "))).andReturn(),
                () -> mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON)
                        .content(body("Test Student One", "race@els29.example.test"))).andReturn());

        assertThat(results).extracting(result -> result.getResponse().getStatus()).containsExactlyInAnyOrder(201, 409);
        assertConflict(results.stream().filter(result -> result.getResponse().getStatus() == 409).findFirst().orElseThrow());
        assertThat(jdbc.queryForList("SELECT name, email FROM student WHERE email = ?", "race@els29.example.test"))
                .containsExactly(Map.of("name", "Test Student One", "email", "race@els29.example.test"));
        assertThat(ownedCount()).isEqualTo(1);
    }

    @Test
    void competingUpdatesPreserveTheLosingStudentsOriginalDetails() throws Exception {
        long first = seed("Original One", "first@els29.example.test");
        long second = seed("Original Two", "second@els29.example.test");
        List<MvcResult> results = race(
                () -> mvc.perform(put("/api/students/" + first).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Changed One", " SHARED@ELS29.EXAMPLE.TEST "))).andReturn(),
                () -> mvc.perform(put("/api/students/" + second).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Changed Two", "shared@els29.example.test"))).andReturn());

        assertThat(results).extracting(result -> result.getResponse().getStatus()).containsExactlyInAnyOrder(200, 409);
        if (results.get(0).getResponse().getStatus() == 200) {
            assertRow(first, "Changed One", "shared@els29.example.test");
            assertRow(second, "Original Two", "second@els29.example.test");
            assertConflict(results.get(1));
        } else {
            assertRow(first, "Original One", "first@els29.example.test");
            assertRow(second, "Changed Two", "shared@els29.example.test");
            assertConflict(results.get(0));
        }
        assertThat(ownedCount()).isEqualTo(2);
    }

    private List<MvcResult> race(Callable<MvcResult> first, Callable<MvcResult> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var one = executor.submit(coordinated(first, ready, start));
            var two = executor.submit(coordinated(second, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Callable<MvcResult> coordinated(Callable<MvcResult> request, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Requests did not start together");
            return request.call();
        };
    }

    private void assertConflict(MvcResult result) throws Exception {
        var error = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(error.size()).isEqualTo(6);
        assertThat(error.get("status").intValue()).isEqualTo(409);
        assertThat(error.get("code").textValue()).isEqualTo("DUPLICATE_EMAIL");
        assertThat(error.get("message").textValue()).isEqualTo("Email already registered");
    }

    private long seed(String name, String email) {
        return jdbc.queryForObject("INSERT INTO student (name, email) VALUES (?, ?) RETURNING id", Long.class, name, email);
    }

    private void assertRow(long id, String name, String email) {
        assertThat(jdbc.queryForMap("SELECT id, name, email FROM student WHERE id = ?", id))
                .containsExactlyInAnyOrderEntriesOf(Map.of("id", id, "name", name, "email", email));
    }

    private long ownedCount() {
        return jdbc.queryForObject("SELECT count(*) FROM student WHERE email LIKE '%@els29.example.test'", Long.class);
    }

    private String body(String name, String email) throws Exception {
        return mapper.writeValueAsString(Map.of("name", name, "email", email));
    }
}
