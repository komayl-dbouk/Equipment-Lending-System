package edu.university.equipmentlending.loan.persistence;

import edu.university.equipmentlending.loan.dto.Loan;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Read-only DTO projections used by loan-history and overdue queries. */
public interface LoanQueryRepository extends Repository<LoanEntity, Long> {

    @Query("""
            select new edu.university.equipmentlending.loan.dto.Loan(
                loan.id, loan.student.id, loan.equipment.id,
                loan.borrowingDate, loan.dueDate, loan.returnDate)
            from LoanEntity loan
            where (:studentId is null or loan.student.id = :studentId)
              and (:equipmentId is null or loan.equipment.id = :equipmentId)
            order by loan.id
            """)
    List<Loan> findHistory(@Param("studentId") Long studentId,
            @Param("equipmentId") Long equipmentId);

    @Query("""
            select new edu.university.equipmentlending.loan.dto.Loan(
                loan.id, loan.student.id, loan.equipment.id,
                loan.borrowingDate, loan.dueDate, loan.returnDate)
            from LoanEntity loan
            where loan.returnDate is null and loan.dueDate < :today
            order by loan.id
            """)
    List<Loan> findOverdue(@Param("today") LocalDate today);
}
