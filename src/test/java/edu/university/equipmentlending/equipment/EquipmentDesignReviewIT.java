package edu.university.equipmentlending.equipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
class EquipmentDesignReviewIT {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private DataSource dataSource;

    @BeforeEach
    @AfterEach
    void cleanup() {
        jdbc.execute("DROP INDEX IF EXISTS els41_review_name_key");
        jdbc.update("DELETE FROM loan WHERE equipment_id IN (SELECT id FROM equipment WHERE inventory_code LIKE 'ELS41-%')");
        jdbc.update("DELETE FROM equipment WHERE inventory_code LIKE 'ELS41-%'");
        jdbc.update("DELETE FROM student WHERE email = 'review@els41.example.test'");
    }

    static Stream<Arguments> listCases() {
        return Stream.of(1, 24).flatMap(size -> Stream.of("all", "true", "false")
                .map(filter -> Arguments.of(size, filter)));
    }

    @ParameterizedTest
    @MethodSource("listCases")
    void listingPreservesExactSqlValuesAndUsesBoundedQueries(int size, String filter) throws Exception {
        long student = jdbc.queryForObject("INSERT INTO student(name,email) VALUES ('Review Student',?) RETURNING id",
                Long.class, "review@els41.example.test");
        for (int i = 0; i < size; i++) {
            long equipment = insert("ELS41-LIST-" + i, "Review Camera " + i);
            if (i % 3 != 2) {
                jdbc.update("INSERT INTO loan(student_id,equipment_id,borrowing_date,due_date,return_date) "
                        + "VALUES (?, ?, DATE '2030-06-10', DATE '2030-06-20', ?::date)",
                        student, equipment, i % 3 == 0 ? null : "2030-06-15");
                if (i % 3 == 1) {
                    jdbc.update("INSERT INTO loan(student_id,equipment_id,borrowing_date,due_date,return_date) "
                            + "VALUES (?, ?, DATE '2030-06-01', DATE '2030-06-05', DATE '2030-06-05')", student, equipment);
                }
            }
        }
        var before = snapshot();
        var historyBefore = jdbc.queryForList("SELECT * FROM loan WHERE equipment_id IN "
                + "(SELECT id FROM equipment WHERE inventory_code LIKE 'ELS41-%') ORDER BY id");
        String availability = "NOT EXISTS (SELECT 1 FROM loan l WHERE l.equipment_id=e.id AND l.return_date IS NULL)";
        String predicate = filter.equals("all") ? "" : " WHERE (" + availability + ") = " + filter;
        var expected = jdbc.queryForList("SELECT e.id, e.inventory_code AS \"inventoryCode\", e.name, e.category, "
                + "e.description, " + availability + " AS available FROM equipment e" + predicate + " ORDER BY e.id");
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        String path = "/api/equipment" + (filter.equals("all") ? "" : "?available=" + filter);
        var response = mvc.perform(get(path)).andReturn().getResponse();
        long statements = statistics.getPrepareStatementCount();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(mapper.readTree(response.getContentAsString()))
                .isEqualTo(mapper.readTree(mapper.writeValueAsString(expected)));
        assertThat(snapshot()).isEqualTo(before);
        assertThat(jdbc.queryForList("SELECT * FROM loan WHERE equipment_id IN "
                + "(SELECT id FROM equipment WHERE inventory_code LIKE 'ELS41-%') ORDER BY id")).isEqualTo(historyBefore);
        System.out.println("ELS41_LIST size=" + size + " filter=" + filter + " statements=" + statements);
        assertThat(statements).as("availability must not require a database round trip per listed item")
                .isLessThanOrEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void unrelatedUniqueConstraintCannotMasqueradeAsInventoryConflict(String method) throws Exception {
        insert("ELS41-SEED", "equipment_inventory_code_key");
        long target = insert("ELS41-TARGET", "Original Name");
        jdbc.execute("CREATE UNIQUE INDEX els41_review_name_key ON equipment(name) WHERE inventory_code LIKE 'ELS41-%'");
        var before = snapshot();
        String payload = mapper.writeValueAsString(Map.of("inventoryCode", "ELS41-NEW",
                "name", "equipment_inventory_code_key", "category", "Camera"));
        var request = method.equals("POST") ? post("/api/equipment") : put("/api/equipment/{id}", target);

        var response = mvc.perform(request.contentType("application/json").content(payload))
                .andReturn().getResponse();

        assertThat(snapshot()).isEqualTo(before);
        System.out.println("ELS41_CONSTRAINT method=" + method + " status=" + response.getStatus());
        assertThat(response.getStatus()).isEqualTo(500);
        var error = mapper.readTree(response.getContentAsString());
        assertThat(error.path("code").textValue()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getContentAsString()).doesNotContain("els41_review_name_key", "equipment_inventory_code_key");
    }

    @Test
    void genuineConcurrentInventoryCollisionStillReturnsOneCreationAndOneConflict() throws Exception {
        var start = new CountDownLatch(1);
        var ready = new CountDownLatch(2);
        var executor = Executors.newFixedThreadPool(2);
        var observer = Executors.newSingleThreadScheduledExecutor();
        String payload = mapper.writeValueAsString(Map.of("inventoryCode", "ELS41-RACE",
                "name", "Race Camera", "category", "Camera"));
        try (var blocker = dataSource.getConnection()) {
            blocker.setAutoCommit(false);
            try (var statement = blocker.createStatement()) {
                statement.execute("LOCK TABLE equipment IN SHARE MODE");
            }
            java.util.concurrent.Callable<org.springframework.mock.web.MockHttpServletResponse> request = () -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return mvc.perform(post("/api/equipment").contentType("application/json").content(payload))
                        .andReturn().getResponse();
            };
            var first = executor.submit(request);
            var second = executor.submit(request);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            var bothWaiting = new java.util.concurrent.CompletableFuture<Void>();
            observer.scheduleWithFixedDelay(() -> {
                try {
                    int waiting = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity "
                            + "WHERE datname=current_database() AND pid<>pg_backend_pid() "
                            + "AND wait_event_type='Lock' AND query ILIKE 'insert into equipment%'", Integer.class);
                    if (waiting >= 2) bothWaiting.complete(null);
                } catch (Exception failure) {
                    bothWaiting.completeExceptionally(failure);
                }
            }, 0, 25, TimeUnit.MILLISECONDS);
            start.countDown();
            bothWaiting.get(10, TimeUnit.SECONDS);
            blocker.commit();
            var results = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertThat(results).extracting(org.springframework.mock.web.MockHttpServletResponse::getStatus)
                    .containsExactlyInAnyOrder(201, 409);
            for (var response : results) {
                var body = mapper.readTree(response.getContentAsString());
                if (response.getStatus() == 409) {
                    assertThat(body.path("code").textValue()).isEqualTo("DUPLICATE_INVENTORY_CODE");
                } else {
                    assertThat(body.path("inventoryCode").textValue()).isEqualTo("ELS41-RACE");
                    assertThat(body.path("available").booleanValue()).isTrue();
                    assertThat(jdbc.queryForObject("SELECT id FROM equipment WHERE inventory_code='ELS41-RACE'", Long.class))
                            .isEqualTo(body.path("id").longValue());
                }
            }
            assertThat(snapshot()).hasSize(1);
            System.out.println("ELS41_GENUINE_RACE statuses=201,409 committed=1 observedInsertWaiters=2");
        } finally {
            start.countDown();
            observer.shutdownNow();
            executor.shutdownNow();
            assertThat(observer.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private long insert(String code, String name) {
        return jdbc.queryForObject("INSERT INTO equipment(inventory_code,name,category,description) "
                + "VALUES (?, ?, 'Camera', NULL) RETURNING id", Long.class, code, name);
    }

    private List<Map<String, Object>> snapshot() {
        return jdbc.queryForList("SELECT * FROM equipment WHERE inventory_code LIKE 'ELS41-%' ORDER BY id");
    }
}
