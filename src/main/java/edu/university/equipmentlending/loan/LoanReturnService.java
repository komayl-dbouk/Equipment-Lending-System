package edu.university.equipmentlending.loan;

import edu.university.equipmentlending.equipment.persistence.EquipmentRepository;
import edu.university.equipmentlending.loan.dto.Loan;
import edu.university.equipmentlending.loan.dto.ReturnWrite;
import edu.university.equipmentlending.loan.persistence.LoanEntity;
import edu.university.equipmentlending.loan.persistence.LoanRepository;
import edu.university.equipmentlending.shared.error.ApiException;
import edu.university.equipmentlending.shared.error.BusinessConflictException;
import edu.university.equipmentlending.shared.error.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LoanReturnService {
    private final EquipmentRepository equipment;
    private final LoanRepository loans;
    private final TransactionTemplate writes;

    public LoanReturnService(EquipmentRepository equipment, LoanRepository loans,
            PlatformTransactionManager transactionManager) {
        this.equipment = equipment;
        this.loans = loans;
        writes = new TransactionTemplate(transactionManager);
        writes.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        writes.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public Loan recordReturn(long id, ReturnWrite request) {
        // execute returns only after commit, including deferred database constraints.
        return writes.execute(status -> returnUnderLock(id, request));
    }

    private Loan returnUnderLock(long id, ReturnWrite request) {
        Long equipmentId = loans.findEquipmentReferenceById(id).orElseThrow(LoanReturnService::missingLoan);
        // Match borrowing/deletion order. Do not load the loan into the persistence context yet.
        equipment.findByIdForUpdate(equipmentId).orElseThrow(LoanReturnService::missingLoan);
        LoanEntity loan = loans.findByIdForUpdate(id).orElseThrow(LoanReturnService::missingLoan);

        if (request.returnDate().isBefore(loan.getBorrowingDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RETURN_DATE",
                    "Return date must be on or after borrowing date");
        }
        if (loan.getReturnDate() != null) {
            throw new BusinessConflictException("LOAN_ALREADY_RETURNED", "Loan has already been returned");
        }

        loan.recordReturn(request.returnDate());
        loans.flush();
        return new Loan(loan.getId(), loan.getStudent().getId(), loan.getEquipment().getId(),
                loan.getBorrowingDate(), loan.getDueDate(), loan.getReturnDate());
    }

    private static ResourceNotFoundException missingLoan() {
        return new ResourceNotFoundException("LOAN_NOT_FOUND", "Loan not found");
    }
}
