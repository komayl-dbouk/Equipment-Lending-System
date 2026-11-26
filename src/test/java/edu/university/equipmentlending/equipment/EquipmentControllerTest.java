package edu.university.equipmentlending.equipment;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import edu.university.equipmentlending.equipment.dto.EquipmentResponse;
import edu.university.equipmentlending.equipment.dto.EquipmentWrite;
import edu.university.equipmentlending.shared.error.BusinessConflictException;
import edu.university.equipmentlending.shared.error.ApiExceptionHandler;
import edu.university.equipmentlending.shared.error.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class EquipmentControllerTest {
    private static final String VALID_BODY = """
            {"inventoryCode":"cam-001","name":"Demo Camera","category":"Camera",
             "description":null}
            """;

    @Mock
    private EquipmentService service;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var mapper = Jackson2ObjectMapperBuilder.json()
                .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new EquipmentController(service))
                .setControllerAdvice(new ApiExceptionHandler(
                        Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void createReturnsDocumentedStatusAndDto() throws Exception {
        when(service.create(any())).thenReturn(response(1L, true));

        mvc.perform(post("/api/equipment").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.inventoryCode").value("CAM-001"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void listReturnsAscendingServiceResultsWithAvailability() throws Exception {
        when(service.list(null)).thenReturn(List.of(response(1L, false), response(2L, true)));

        mvc.perform(get("/api/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].available").value(false))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void availabilityFilterDelegatesExactBooleanValue() throws Exception {
        when(service.list(true)).thenReturn(List.of(response(2L, true)));

        mvc.perform(get("/api/equipment").queryParam("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].available").value(true));
        verify(service).list(true);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "TRUE", " false", "yes", "1"})
    void invalidAvailabilityFilterReturnsValidationError(String available) throws Exception {
        mvc.perform(get("/api/equipment").queryParam("available", available))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void updateReturnsReplacementDto() throws Exception {
        when(service.update(eq(4L), any())).thenReturn(response(4L, false));

        mvc.perform(put("/api/equipment/4").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void deleteReturnsEmptyNoContentResponse() throws Exception {
        mvc.perform(delete("/api/equipment/4"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(service).delete(4L);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"inventoryCode\":\" \",\"name\":\"Camera\",\"category\":\"Camera\"}",
            "{\"inventoryCode\":\"CAM-001\",\"name\":\" \",\"category\":\"Camera\"}",
            "{\"inventoryCode\":\"CAM-001\",\"name\":\"Camera\",\"category\":\" \"}",
            "{\"inventoryCode\":\"CAM-001\",\"name\":\"Camera\",\"category\":\"Camera\",\"unknown\":\"private\"}"
    })
    void invalidOrUnknownWritePropertiesReturnSafeValidationError(String body) throws Exception {
        mvc.perform(post("/api/equipment").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("private"))));
    }

    @Test
    void rawOverlengthFieldsAreRejectedBeforeService() throws Exception {
        String body = "{\"inventoryCode\":\"" + "x".repeat(51)
                + "\",\"name\":\"Camera\",\"category\":\"Camera\"}";

        mvc.perform(post("/api/equipment").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.inventoryCode").value("Invalid value"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "not-a-number"})
    void malformedAndNonpositivePathIdsAreRejected(String id) throws Exception {
        mvc.perform(delete("/api/equipment/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void serviceNotFoundAndConflictErrorsRetainStableCodes() throws Exception {
        when(service.update(eq(99L), any())).thenThrow(
                new ResourceNotFoundException("EQUIPMENT_NOT_FOUND", "Equipment not found"));
        doThrow(new BusinessConflictException(
                "EQUIPMENT_HAS_HISTORY", "Equipment has loan history"))
                .when(service).delete(4L);

        mvc.perform(put("/api/equipment/99").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_NOT_FOUND"));
        mvc.perform(delete("/api/equipment/4"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EQUIPMENT_HAS_HISTORY"));
    }

    private static EquipmentResponse response(long id, boolean available) {
        return new EquipmentResponse(
                id, "CAM-001", "Demo Camera", "Camera", null, available);
    }
}
