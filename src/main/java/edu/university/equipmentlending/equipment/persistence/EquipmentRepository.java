package edu.university.equipmentlending.equipment.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipmentRepository extends JpaRepository<EquipmentEntity, Long> {

    List<EquipmentEntity> findAllByOrderByIdAsc();

    boolean existsByInventoryCode(String inventoryCode);

    boolean existsByInventoryCodeAndIdNot(String inventoryCode, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select equipment from EquipmentEntity equipment where equipment.id = :id")
    Optional<EquipmentEntity> findByIdForUpdate(@Param("id") Long id);
}
