package edu.university.equipmentlending;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.withSettings;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.university.equipmentlending.equipment.EquipmentController;
import edu.university.equipmentlending.equipment.EquipmentService;
import edu.university.equipmentlending.equipment.persistence.EquipmentRepository;
import edu.university.equipmentlending.loan.persistence.LoanRepository;
import edu.university.equipmentlending.shared.error.ApiExceptionHandler;
import edu.university.equipmentlending.student.StudentController;
import edu.university.equipmentlending.student.StudentService;
import edu.university.equipmentlending.student.persistence.StudentRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockMakers;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class StrictWriteValidationTest {
    @ParameterizedTest
    @MethodSource("invalidWrites")
    void rejectsInvalidTextBeforePersistence(String feature, String method, String field, Object value)
            throws Exception {
        var equipment = mock(EquipmentRepository.class, withSettings().mockMaker(MockMakers.PROXY));
        var loans = mock(LoanRepository.class, withSettings().mockMaker(MockMakers.PROXY));
        var students = mock(StudentRepository.class, withSettings().mockMaker(MockMakers.PROXY));
        var transactions = mock(PlatformTransactionManager.class, withSettings().mockMaker(MockMakers.PROXY));
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var mvc = MockMvcBuilders.standaloneSetup(
                            new EquipmentController(new EquipmentService(equipment, loans)),
                            new StudentController(new StudentService(students, validator, transactions)))
                    .setControllerAdvice(new ApiExceptionHandler(Clock.systemUTC()))
                    .setValidator(validator).build();
            Map<String, Object> body = new LinkedHashMap<>(feature.equals("students")
                    ? Map.of("name", "Valid Student", "email", "valid@example.test")
                    : Map.of("inventoryCode", "VALID-001", "name", "Valid Camera",
                            "category", "Camera", "description", "Detail"));
            body.put(field, value);
            String path = "/api/" + feature + (method.equals("PUT") ? "/1" : "");
            mvc.perform(request(HttpMethod.valueOf(method), path)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
            verifyNoInteractions(equipment, loans, students, transactions);
        }
    }

    static Stream<Arguments> invalidWrites() {
        return Stream.of("students", "equipment").flatMap(feature ->
                Stream.of("POST", "PUT").flatMap(method ->
                        (feature.equals("students") ? Stream.of("name", "email")
                                : Stream.of("inventoryCode", "name", "category", "description"))
                                .flatMap(field -> Stream.of(123, true, "Bad\0Value")
                                        .map(value -> Arguments.of(feature, method, field, value)))));
    }
}
