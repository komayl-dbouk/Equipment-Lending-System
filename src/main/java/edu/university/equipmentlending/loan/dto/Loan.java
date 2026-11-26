package edu.university.equipmentlending.loan.dto;

import java.time.LocalDate;

public record Loan(Long id, Long studentId, Long equipmentId,
        LocalDate borrowingDate, LocalDate dueDate, LocalDate returnDate) {
}
