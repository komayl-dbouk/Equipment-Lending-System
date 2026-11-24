package edu.university.equipmentlending.equipment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.university.equipmentlending.equipment.persistence.EquipmentRepository;
import edu.university.equipmentlending.loan.persistence.LoanRepository;
import edu.university.equipmentlending.shared.error.ApiExceptionHandler;
import java.time.Clock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class EquipmentNormalizationTest {
    @ParameterizedTest
    @CsvSource({
            "POST, inventoryCode", "POST, name", "POST, category",
            "PUT, inventoryCode", "PUT, name", "PUT, category"
    })
    void rejectsRequiredFieldsThatBecomeBlankBeforeAccessingPersistence(
            String method, String field) throws Exception {
        var equipmentRepository = mock(EquipmentRepository.class);
        var loanRepository = mock(LoanRepository.class);
        var service = new EquipmentService(equipmentRepository, loanRepository);
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var mvc = MockMvcBuilders.standaloneSetup(new EquipmentController(service))
                    .setControllerAdvice(new ApiExceptionHandler(Clock.systemUTC()))
                    .setValidator(validator)
                    .build();
            String body = "{\"inventoryCode\":\"CAM-1\",\"name\":\"Camera\",\"category\":\"Camera\"}";
            String original = field.equals("inventoryCode") ? "CAM-1" : "Camera";
            body = body.replace("\"" + field + "\":\"" + original + "\"",
                    "\"" + field + "\":\"\u2003\"");
            String path = method.equals("POST") ? "/api/equipment" : "/api/equipment/1";

            mvc.perform(request(HttpMethod.valueOf(method), path)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
            verifyNoInteractions(equipmentRepository, loanRepository);
        }
    }
}
