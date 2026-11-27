package edu.university.equipmentlending.loan;

import edu.university.equipmentlending.loan.dto.Loan;
import edu.university.equipmentlending.loan.persistence.LoanEntity;

/** Maps a loan entity to the feature's exact public response shape. */
final class LoanMapper {
    private LoanMapper() {
    }

    static Loan toDto(LoanEntity loan) {
        return new Loan(loan.getId(), loan.getStudent().getId(), loan.getEquipment().getId(),
                loan.getBorrowingDate(), loan.getDueDate(), loan.getReturnDate());
    }
}
