package edu.university.equipmentlending.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Supplies the explicit synthetic staff identity for existing business-contract tests. */
@TestConfiguration(proxyBeanMethods = false)
public class AuthenticatedMockMvcConfiguration {
    public static final String USERNAME = "els-test-staff";
    public static final String PASSWORD = "synthetic-only-test-credential";

    @Bean
    MockMvcBuilderCustomizer authenticatedStaffRequest() {
        return builder -> builder.defaultRequest(get("/").with(httpBasic(USERNAME, PASSWORD)));
    }
}
