package edu.university.equipmentlending.equipment.persistence;

import edu.university.equipmentlending.equipment.dto.EquipmentResponse;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipmentRepository extends JpaRepository<EquipmentEntity, Long> {

    @Query("""
            select new edu.university.equipmentlending.equipment.dto.EquipmentResponse(
                item.id, item.inventoryCode, item.name, item.category, item.description,
                case when activeLoan.id is null then true else false end)
            from EquipmentEntity item
            left join LoanEntity activeLoan on activeLoan.equipment = item and activeLoan.returnDate is null
            where :available is null
               or (:available = true and activeLoan.id is null)
               or (:available = false and activeLoan.id is not null)
            order by item.id
            """)
    List<EquipmentResponse> findAllResponses(@Param("available") Boolean available);

    boolean existsByInventoryCode(String inventoryCode);

    boolean existsByInventoryCodeAndIdNot(String inventoryCode, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select equipment from EquipmentEntity equipment where equipment.id = :id")
    Optional<EquipmentEntity> findByIdForUpdate(@Param("id") Long id);
}
