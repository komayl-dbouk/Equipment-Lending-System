package edu.university.equipmentlending.student;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.university.equipmentlending.shared.error.ApiExceptionHandler;
import edu.university.equipmentlending.student.persistence.StudentEntity;
import edu.university.equipmentlending.student.persistence.StudentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockMakers;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Exercises the real controller, service, Jakarta validation and shared advice without a database. */
class StudentControllerTest {
    private MockMvc mvc;
    private StudentRepository repository;
    private PlatformTransactionManager transactions;
    private LocalValidatorFactoryBean validator;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        repository = mock(StudentRepository.class, withSettings().mockMaker(MockMakers.PROXY));
        transactions = mock(PlatformTransactionManager.class, withSettings().mockMaker(MockMakers.PROXY));
        when(transactions.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mapper = Jackson2ObjectMapperBuilder.json()
                .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        mvc = MockMvcBuilders.standaloneSetup(new StudentController(
                        new StudentService(repository, validator, transactions)))
                .setControllerAdvice(new ApiExceptionHandler(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .setValidator(validator).build();
    }

    @AfterEach
    void closeValidation() { if (validator != null) validator.close(); }

    @Test
    void registrationReturnsCreatedLocationAndExactlyTheNormalizedDto() throws Exception {
        stubInsert(41);
        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON)
                        .content(body("  Test Student One \t", " Borrower.One@Example.test ")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/students/41"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.id").value(41))
                .andExpect(jsonPath("$.name").value("Test Student One"))
                .andExpect(jsonPath("$.email").value("borrower.one@example.test"));
        verify(repository).saveAndFlush(any());
        verify(transactions).commit(any());
    }

    @Test
    void getReturnsExactlyTheStoredDto() throws Exception {
        when(repository.findById(41L)).thenReturn(Optional.of(
                StudentServiceTest.student(41, "Stored Student", "stored@example.test")));
        mvc.perform(get("/api/students/41"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":41,\"name\":\"Stored Student\",\"email\":\"stored@example.test\"}",
                        JsonCompareMode.STRICT));
        verify(repository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"new@example.test", " STORED@EXAMPLE.TEST "})
    void putReplacesBothFieldsAndAllowsOwnEmail(String email) throws Exception {
        StudentEntity stored = StudentServiceTest.student(41, "Original Name", "stored@example.test");
        when(repository.findById(41L)).thenReturn(Optional.of(stored));
        when(repository.saveAndFlush(stored)).thenReturn(stored);
        String expectedEmail = email.startsWith(" ") ? "stored@example.test" : "new@example.test";

        mvc.perform(put("/api/students/41").contentType(MediaType.APPLICATION_JSON)
                        .content(body(" Updated Name ", email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.id").value(41))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value(expectedEmail));
        verify(transactions).commit(any());
    }

    @Test
    void exactRawLengthBoundariesAreAccepted() throws Exception {
        stubInsert(41);
        String email = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON)
                        .content(body("n".repeat(120), email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("n".repeat(120)))
                .andExpect(jsonPath("$.email").value(email));
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void invalidPostAndPutBodiesReturnSafe400WithoutPersistence(String body) throws Exception {
        assertError(mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON).content(body)),
                400, "VALIDATION_FAILED", "/api/students");
        assertError(mvc.perform(put("/api/students/41").contentType(MediaType.APPLICATION_JSON).content(body)),
                400, "VALIDATION_FAILED", "/api/students/41");
        verifyNoInteractions(repository, transactions);
    }

    static Stream<String> invalidBodies() {
        Stream<String> invalidValues = StudentServiceTest.invalidInputs().map(input -> {
            try {
                return new ObjectMapper().writeValueAsString(input);
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });
        return Stream.concat(invalidValues, Stream.of(
                "", "null", "{", "[]", "{}",
                "{\"name\":\"Test Student\"}",
                "{\"email\":\"one@example.test\"}",
                "{\"name\":123,\"email\":\"one@example.test\"}",
                "{\"name\":true,\"email\":\"one@example.test\"}",
                "{\"name\":[],\"email\":\"one@example.test\"}",
                "{\"name\":\"Test Student\",\"email\":123}",
                "{\"name\":\"Test Student\",\"email\":\"one@example.test\",\"id\":999}",
                "{\"name\":\"Test Student\",\"email\":\"one@example.test\",\"loans\":[]}",
                "{\"name\":\"Test Student\",\"email\":\"one@example.test\",\"extra\":\"private-value\"}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1", "1.5", "9223372036854775808"})
    void invalidIdsAreRejectedForGetAndPutBeforePersistence(String id) throws Exception {
        String path = "/api/students/" + id;
        assertError(mvc.perform(get(path)), 400, "VALIDATION_FAILED", path);
        assertError(mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON)
                .content(body("Test Student", "one@example.test"))), 400, "VALIDATION_FAILED", path);
        verifyNoInteractions(repository, transactions);
    }

    @Test
    void missingGetAndPutReturn404WithoutSaving() throws Exception {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertError(mvc.perform(get("/api/students/99")), 404, "STUDENT_NOT_FOUND", "/api/students/99");
        assertError(mvc.perform(put("/api/students/99").contentType(MediaType.APPLICATION_JSON)
                .content(body("Test Student", "one@example.test"))), 404, "STUDENT_NOT_FOUND", "/api/students/99");
        verify(repository, never()).saveAndFlush(any());
        verify(transactions, never()).commit(any());
    }

    @ParameterizedTest
    @CsvSource({"POST,/api/students", "PUT,/api/students/41"})
    void duplicateRegistrationOrAnotherStudentsEmailReturns409(String method, String path) throws Exception {
        if (method.equals("PUT")) when(repository.findById(41L)).thenReturn(Optional.of(
                StudentServiceTest.student(41, "Original Name", "original@example.test")));
        when(repository.saveAndFlush(any())).thenThrow(StudentServiceTest.constraintFailure("23505", "student_email_key"));
        var request = method.equals("POST") ? post(path) : put(path);
        assertError(mvc.perform(request.contentType(MediaType.APPLICATION_JSON)
                .content(body("Replacement Name", " USED@EXAMPLE.TEST "))), 409, "DUPLICATE_EMAIL", path)
                .andExpect(jsonPath("$.message").value("Email already registered"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
        verify(transactions).rollback(any());
        verify(transactions, never()).commit(any());
    }

    @Test
    void unrelatedDatabaseFailureReturnsSanitized500() throws Exception {
        when(repository.saveAndFlush(any())).thenThrow(StudentServiceTest.constraintFailure("23505", "student_pkey"));
        assertError(mvc.perform(post("/api/students").contentType(MediaType.APPLICATION_JSON)
                .content(body("Test Student", "one@example.test"))), 500, "INTERNAL_ERROR", "/api/students")
                .andExpect(jsonPath("$.message").value("Unexpected internal failure"));
        verify(transactions).rollback(any());
    }

    private void stubInsert(long id) {
        when(repository.saveAndFlush(any())).thenAnswer(call -> {
            StudentEntity entity = call.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", id);
            return entity;
        });
    }

    private String body(String name, String email) throws Exception {
        return mapper.writeValueAsString(Map.of("name", name, "email", email));
    }

    private ResultActions assertError(ResultActions result, int status, String code, String path) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(6)))
                .andExpect(jsonPath("$.timestamp").value("1970-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andExpect(content().string(not(containsString("private"))))
                .andExpect(content().string(not(containsString("example.test"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
    }
}
