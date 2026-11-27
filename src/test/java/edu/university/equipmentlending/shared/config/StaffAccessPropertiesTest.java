package edu.university.equipmentlending.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;

class StaffAccessPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfiguration.class)
            .withInitializer(context -> context.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME));

    @Test
    void missingCredentialFailsConfiguration() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void blankCredentialFailsConfiguration() {
        runner.withPropertyValues(
                        "app.security.staff.username= ",
                        "app.security.staff.password= ")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void nonBlankCredentialBinds() {
        runner.withPropertyValues(
                        "app.security.staff.username=synthetic-staff",
                        "app.security.staff.password=synthetic-credential")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(StaffAccessProperties.class).username())
                            .isEqualTo("synthetic-staff");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(StaffAccessProperties.class)
    static class PropertiesConfiguration {
    }
}
