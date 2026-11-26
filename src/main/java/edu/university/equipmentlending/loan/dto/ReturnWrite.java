package edu.university.equipmentlending.loan.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ReturnWrite(
        @JsonDeserialize(using = StrictLoanDateDeserializer.class)
        @NotNull LocalDate returnDate) {
}
