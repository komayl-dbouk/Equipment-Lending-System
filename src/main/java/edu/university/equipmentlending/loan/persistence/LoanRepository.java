package edu.university.equipmentlending.loan.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Shared read foundation for future equipment and loan history queries. */
public interface LoanRepository extends JpaRepository<LoanEntity, Long> {

    // Scalar lookup avoids caching an unlocked loan before waiting for equipment.
    @Query("select loan.equipment.id from LoanEntity loan where loan.id = :id")
    Optional<Long> findEquipmentReferenceById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select loan from LoanEntity loan where loan.id = :id")
    Optional<LoanEntity> findByIdForUpdate(@Param("id") Long id);

    List<LoanEntity> findAllByEquipmentIdOrderByIdAsc(Long equipmentId);

    boolean existsByEquipmentId(Long equipmentId);

    boolean existsByEquipmentIdAndReturnDateIsNull(Long equipmentId);
}
