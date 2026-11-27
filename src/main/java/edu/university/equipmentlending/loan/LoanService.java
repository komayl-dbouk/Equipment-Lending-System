package edu.university.equipmentlending.loan;

import edu.university.equipmentlending.equipment.persistence.EquipmentEntity;
import edu.university.equipmentlending.equipment.persistence.EquipmentRepository;
import edu.university.equipmentlending.loan.dto.Loan;
import edu.university.equipmentlending.loan.dto.LoanWrite;
import edu.university.equipmentlending.loan.persistence.LoanEntity;
import edu.university.equipmentlending.loan.persistence.LoanRepository;
import edu.university.equipmentlending.shared.error.ApiException;
import edu.university.equipmentlending.shared.error.BusinessConflictException;
import edu.university.equipmentlending.shared.error.ResourceNotFoundException;
import edu.university.equipmentlending.shared.persistence.DatabaseConstraintMatcher;
import edu.university.equipmentlending.student.persistence.StudentEntity;
import edu.university.equipmentlending.student.persistence.StudentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LoanService {
    private final StudentRepository students;
    private final EquipmentRepository equipment;
    private final LoanRepository loans;
    private final TransactionTemplate writes;

    public LoanService(StudentRepository students, EquipmentRepository equipment,
            LoanRepository loans, PlatformTransactionManager transactionManager) {
        this.students = students;
        this.equipment = equipment;
        this.loans = loans;
        this.writes = new TransactionTemplate(transactionManager);
        this.writes.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Loan borrow(LoanWrite request) {
        if (!request.dueDate().isAfter(request.borrowingDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LOAN_DATES",
                    "Due date must be after borrowing date");
        }

        try {
            // execute returns only after commit. A failed flush or commit rolls back first.
            return writes.execute(status -> createUnderLock(request));
        } catch (DataIntegrityViolationException exception) {
            if (DatabaseConstraintMatcher.matches(
                    exception, "23505", "ux_loan_active_equipment")) {
                throw new BusinessConflictException("EQUIPMENT_UNAVAILABLE",
                        "Equipment is already on loan");
            }
            throw exception;
        }
    }

    private Loan createUnderLock(LoanWrite request) {
        StudentEntity student = students.findById(request.studentId()).orElseThrow(() ->
                new ResourceNotFoundException("STUDENT_NOT_FOUND", "Student not found"));
        EquipmentEntity item = equipment.findByIdForUpdate(request.equipmentId()).orElseThrow(() ->
                new ResourceNotFoundException("EQUIPMENT_NOT_FOUND", "Equipment not found"));
        if (loans.existsByEquipmentIdAndReturnDateIsNull(item.getId())) {
            throw new BusinessConflictException("EQUIPMENT_UNAVAILABLE",
                    "Equipment is already on loan");
        }

        LoanEntity saved = loans.saveAndFlush(new LoanEntity(
                student, item, request.borrowingDate(), request.dueDate()));
        return LoanMapper.toDto(saved);
    }
}
