package edu.university.equipmentlending.shared.config;

import static edu.university.equipmentlending.support.AuthenticatedMockMvcConfiguration.PASSWORD;
import static edu.university.equipmentlending.support.AuthenticatedMockMvcConfiguration.USERNAME;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.json.JsonCompareMode;

@SpringBootTest
@AutoConfigureMockMvc
class StaffAccessSecurityIT {
    private static final String EMAIL = "els46.authorized@example.test";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void cleanFixture() {
        jdbc.update("DELETE FROM student WHERE email = ?", EMAIL);
    }

    @Test
    void healthIsPublicAndSanitized() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.parseMediaType("application/*+json")))
                .andExpect(content().json("{\"status\":\"UP\"}", JsonCompareMode.STRICT));
    }

    @Test
    void missingCredentialCannotReadProtectedData() throws Exception {
        assertUnauthorized(mvc.perform(get("/api/equipment")), "/api/equipment");
    }

    @Test
    void wrongCredentialHasSameSanitizedFailure() throws Exception {
        assertUnauthorized(mvc.perform(get("/api/equipment")
                .with(httpBasic(USERNAME, "wrong-synthetic-credential"))), "/api/equipment");
    }

    @Test
    void rejectedMutationDoesNotWrite() throws Exception {
        Integer before = jdbc.queryForObject("SELECT count(*) FROM student", Integer.class);

        assertUnauthorized(mvc.perform(post("/api/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"ELS-46 Rejected Student","email":"els46.rejected@example.test"}
                        """)), "/api/students");

        Integer after = jdbc.queryForObject("SELECT count(*) FROM student", Integer.class);
        org.assertj.core.api.Assertions.assertThat(after).isEqualTo(before);
    }

    @Test
    void validCredentialPreservesAuthorizedBusinessContract() throws Exception {
        mvc.perform(post("/api/students")
                        .with(httpBasic(USERNAME, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ELS-46 Authorized Student","email":"els46.authorized@example.test"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ELS-46 Authorized Student"))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    private static void assertUnauthorized(
            org.springframework.test.web.servlet.ResultActions result, String path) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
                        "Basic realm=\"equipment-lending\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Staff credentials required"))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andExpect(jsonPath("$.fieldErrors.*", empty()))
                .andExpect(jsonPath("$.*", hasSize(6)))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(PASSWORD))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("els46.rejected@example.test"))));
    }
}
