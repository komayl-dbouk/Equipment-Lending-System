package edu.university.equipmentlending;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class DatabaseTestSafetyProcessorTest {
    private final DatabaseTestSafetyProcessor processor = new DatabaseTestSafetyProcessor();

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "", "jdbc:h2:mem:equipment_test", "jdbc:postgresql://localhost/equipment_lending",
            "jdbc:postgresql://localhost/", "jdbc:postgresql:equipment_test",
            "jdbc:postgresql://localhost/equipment_test/extra",
            "jdbc:postgresql://localhost/equipment_test?PGDBNAME=equipment_lending"
    })
    void rejectsUnsafeTargets(String url) {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("database-test");
        if (url != null) environment.setProperty("spring.datasource.url", url);
        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsApplicationDatabaseEvenWithDifferentHostAndQuery() {
        var environment = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/shared_test?sslmode=disable")
                .withProperty("ELS_DB_URL", "jdbc:postgresql://127.0.0.1:5432/shared_test");
        environment.setActiveProfiles("database-test");
        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://localhost:5433/equipment_lending_test",
            "jdbc:postgresql://localhost:5433/equipment_lending_test?%50%47%44%42%4e%41%4d%45=equipment_lending"
    })
    void acceptsSeparateTestDatabaseUsingTheDriversEffectiveName(String url) {
        var environment = new MockEnvironment()
                .withProperty("spring.datasource.url", url)
                .withProperty("ELS_DB_URL", "jdbc:postgresql://localhost:5432/equipment_lending");
        environment.setActiveProfiles("database-test");
        assertThatCode(() -> processor.postProcessEnvironment(environment, null)).doesNotThrowAnyException();
    }

    @Test
    void doesNotAffectDatabaseFreeTests() {
        assertThatCode(() -> processor.postProcessEnvironment(new MockEnvironment(), null))
                .doesNotThrowAnyException();
    }
}
