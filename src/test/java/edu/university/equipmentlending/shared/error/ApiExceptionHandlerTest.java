package edu.university.equipmentlending.shared.error;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.*;

class ApiExceptionHandlerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var mapper = Jackson2ObjectMapperBuilder.json()
                .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new Fixture())
                .setControllerAdvice(new ApiExceptionHandler(
                        Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .setValidator(validator).build();
    }

    @Test
    void fieldValidationUsesSafeMessagesAndStableShape() throws Exception {
        mvc.perform(post("/fixture").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.path").value("/fixture"))
                .andExpect(jsonPath("$.fieldErrors.name").value("Invalid value"))
                .andExpect(content().string(not(containsString("private annotation detail"))));
    }

    @ParameterizedTest
    @CsvSource({"'{'", "'{\"name\":\"ok\",\"extra\":\"secret-value\"}'"})
    void malformedOrUnknownPropertiesAreSanitized(String body) throws Exception {
        mvc.perform(post("/fixture").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andExpect(content().string(not(containsString("secret-value"))));
    }

    @ParameterizedTest
    @CsvSource({"/fixture/id/not-a-number", "/fixture/id/0", "/fixture/id/-1", "/fixture/query"})
    void conversionMissingParametersAndNonpositiveIdsAreRejected(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @ParameterizedTest
    @CsvSource({"missing,404,STUDENT_NOT_FOUND", "conflict,409,DUPLICATE_EMAIL",
                "dates,400,INVALID_LOAN_DATES"})
    void explicitDomainFailuresPreserveContract(String kind, int status, String code) throws Exception {
        mvc.perform(get("/fixture/failure/" + kind))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void unexpectedFailureDoesNotExposeDetailsOrQueryString() throws Exception {
        mvc.perform(get("/fixture/failure/unexpected").queryParam("secret", "private-value"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected internal failure"))
                .andExpect(jsonPath("$.path").value("/fixture/failure/unexpected"))
                .andExpect(content().string(not(containsString("private-value"))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
    }

    @Test
    void validInputStillReachesController() throws Exception {
        mvc.perform(post("/fixture").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ok\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("ok"));
    }

    @RestController
    static class Fixture {
        record Input(@NotBlank(message = "private annotation detail") String name) { }

        @PostMapping("/fixture")
        Input write(@Valid @RequestBody Input input) { return input; }

        @GetMapping("/fixture/id/{id}")
        long id(@PathVariable("id") @Positive long id) { return id; }

        @GetMapping("/fixture/query")
        String query(@RequestParam("name") String name) { return name; }

        @GetMapping("/fixture/failure/{kind}")
        String fail(@PathVariable("kind") String kind) {
            switch (kind) {
                case "missing" -> throw new ResourceNotFoundException("STUDENT_NOT_FOUND", "Student not found");
                case "conflict" -> throw new BusinessConflictException("DUPLICATE_EMAIL", "Email already registered");
                case "dates" -> throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LOAN_DATES", "Invalid loan dates");
                default -> throw new IllegalStateException("SQL password=private-value");
            }
        }
    }
}
