package edu.university.equipmentlending.loan;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.university.equipmentlending.loan.dto.Loan;
import edu.university.equipmentlending.shared.error.ApiExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class LoanQueryControllerTest {
    @Mock private LoanQueryService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new LoanQueryController(service))
                .setControllerAdvice(new ApiExceptionHandler(
                        Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)))
                .build();
    }

    @Test
    void listAcceptsOmittedAndCombinedFilters() throws Exception {
        Loan loan = new Loan(7L, 1L, 2L,
                LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 2), null);
        when(service.list(null, null)).thenReturn(List.of());
        when(service.list(1L, 2L)).thenReturn(List.of(loan));

        mvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/loans?studentId=1&equipmentId=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].studentId").value(1))
                .andExpect(jsonPath("$[0].equipmentId").value(2));
        verify(service).list(null, null);
        verify(service).list(1L, 2L);
    }

    @Test
    void overdueDelegatesToQueryService() throws Exception {
        when(service.overdue()).thenReturn(List.of());

        mvc.perform(get("/api/loans/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        verify(service).overdue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "-1", "+1", "word", "9223372036854775808"})
    void invalidQueryIdsReturnValidationErrorWithoutServiceCall(String value) throws Exception {
        mvc.perform(get("/api/loans?studentId=" + value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service);
    }
}
