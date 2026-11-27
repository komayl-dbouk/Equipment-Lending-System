package edu.university.equipmentlending.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;

class StaffAccessPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfiguration.class)
            .withInitializer(context -> {
                context.getEnvironment().getPropertySources()
                        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                context.getEnvironment().getPropertySources()
                        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            })
            .withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    void missingCredentialFailsConfiguration() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void blankCredentialFailsConfiguration() {
        runner.withPropertyValues(
                        "ELS_STAFF_USERNAME= ",
                        "ELS_STAFF_PASSWORD= ")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void nonBlankCredentialBinds() {
        runner.withPropertyValues(
                        "ELS_STAFF_USERNAME=synthetic-staff",
                        "ELS_STAFF_PASSWORD=synthetic-credential")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(StaffAccessProperties.class).username())
                            .isEqualTo("synthetic-staff");
                });
    }

    @Test
    void missingPasswordFailsConfiguration() {
        runner.withPropertyValues("ELS_STAFF_USERNAME=synthetic-staff")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void missingUsernameFailsConfiguration() {
        runner.withPropertyValues("ELS_STAFF_PASSWORD=synthetic-credential")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(StaffAccessProperties.class)
    static class PropertiesConfiguration {
    }
}
