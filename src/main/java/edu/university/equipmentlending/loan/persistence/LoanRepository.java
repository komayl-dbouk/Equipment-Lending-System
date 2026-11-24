package edu.university.equipmentlending.loan.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Shared read foundation for future equipment and loan history queries. */
public interface LoanRepository extends JpaRepository<LoanEntity, Long> {

    List<LoanEntity> findAllByEquipmentIdOrderByIdAsc(Long equipmentId);

    boolean existsByEquipmentId(Long equipmentId);

    boolean existsByEquipmentIdAndReturnDateIsNull(Long equipmentId);
}
