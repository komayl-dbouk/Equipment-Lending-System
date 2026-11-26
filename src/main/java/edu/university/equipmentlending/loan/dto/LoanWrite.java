package edu.university.equipmentlending.loan.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record LoanWrite(
        @JsonDeserialize(using = StrictLoanIdDeserializer.class)
        @NotNull @Positive Long studentId,
        @JsonDeserialize(using = StrictLoanIdDeserializer.class)
        @NotNull @Positive Long equipmentId,
        @JsonDeserialize(using = StrictLoanDateDeserializer.class)
        @NotNull LocalDate borrowingDate,
        @JsonDeserialize(using = StrictLoanDateDeserializer.class)
        @NotNull LocalDate dueDate) {
}
