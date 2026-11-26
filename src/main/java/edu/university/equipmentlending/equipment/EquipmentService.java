package edu.university.equipmentlending.equipment;

import edu.university.equipmentlending.equipment.dto.EquipmentResponse;
import edu.university.equipmentlending.equipment.dto.EquipmentWrite;
import edu.university.equipmentlending.equipment.persistence.EquipmentEntity;
import edu.university.equipmentlending.equipment.persistence.EquipmentRepository;
import edu.university.equipmentlending.loan.persistence.LoanRepository;
import edu.university.equipmentlending.shared.error.ApiException;
import edu.university.equipmentlending.shared.error.BusinessConflictException;
import edu.university.equipmentlending.shared.error.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentService {
    private static final String DUPLICATE_CONSTRAINT = "equipment_inventory_code_key";
    private static final String HISTORY_CONSTRAINT = "loan_equipment_id_fkey";

    private final EquipmentRepository equipmentRepository;
    private final LoanRepository loanRepository;

    public EquipmentService(
            EquipmentRepository equipmentRepository, LoanRepository loanRepository) {
        this.equipmentRepository = equipmentRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional
    public EquipmentResponse create(EquipmentWrite request) {
        NormalizedEquipment normalized = normalize(request);
        if (equipmentRepository.existsByInventoryCode(normalized.inventoryCode())) {
            throw duplicateInventoryCode();
        }

        EquipmentEntity equipment = new EquipmentEntity(
                normalized.inventoryCode(), normalized.name(), normalized.category(),
                normalized.description());
        try {
            equipmentRepository.saveAndFlush(equipment);
        } catch (DataIntegrityViolationException exception) {
            if (causedByConstraint(exception, "23505", DUPLICATE_CONSTRAINT)) {
                throw duplicateInventoryCode();
            }
            throw exception;
        }
        return response(equipment);
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponse> list() {
        return list(null);
    }

    @Transactional(readOnly = true)
    public List<EquipmentResponse> list(Boolean available) {
        return equipmentRepository.findAllResponses(available);
    }

    @Transactional
    public EquipmentResponse update(long id, EquipmentWrite request) {
        NormalizedEquipment normalized = normalize(request);
        EquipmentEntity equipment = equipmentRepository.findById(id)
                .orElseThrow(EquipmentService::equipmentNotFound);
        if (equipmentRepository.existsByInventoryCodeAndIdNot(normalized.inventoryCode(), id)) {
            throw duplicateInventoryCode();
        }

        equipment.replaceEditableFields(
                normalized.inventoryCode(), normalized.name(), normalized.category(),
                normalized.description());
        try {
            equipmentRepository.saveAndFlush(equipment);
        } catch (DataIntegrityViolationException exception) {
            if (causedByConstraint(exception, "23505", DUPLICATE_CONSTRAINT)) {
                throw duplicateInventoryCode();
            }
            throw exception;
        }
        return response(equipment);
    }

    @Transactional
    public void delete(long id) {
        EquipmentEntity equipment = equipmentRepository.findByIdForUpdate(id)
                .orElseThrow(EquipmentService::equipmentNotFound);
        if (loanRepository.existsByEquipmentId(id)) {
            throw equipmentHasHistory();
        }

        try {
            equipmentRepository.delete(equipment);
            equipmentRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (causedByConstraint(exception, "23503", HISTORY_CONSTRAINT)) {
                throw equipmentHasHistory();
            }
            throw exception;
        }
    }

    private EquipmentResponse response(EquipmentEntity equipment) {
        boolean available = !loanRepository.existsByEquipmentIdAndReturnDateIsNull(
                equipment.getId());
        return new EquipmentResponse(
                equipment.getId(), equipment.getInventoryCode(), equipment.getName(),
                equipment.getCategory(), equipment.getDescription(), available);
    }

    private static NormalizedEquipment normalize(EquipmentWrite request) {
        String inventoryCode = request.inventoryCode().strip().toUpperCase(Locale.ROOT);
        String name = request.name().strip();
        String category = request.category().strip();
        if (inventoryCode.isBlank() || name.isBlank() || category.isBlank()
                || inventoryCode.length() > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Invalid request");
        }
        return new NormalizedEquipment(
                inventoryCode,
                name,
                category,
                request.description() == null ? null : request.description().strip());
    }

    private static boolean causedByConstraint(
            DataIntegrityViolationException exception, String sqlState, String constraintName) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && sqlState.equals(violation.getSQLState())
                    && constraintName.equals(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static ResourceNotFoundException equipmentNotFound() {
        return new ResourceNotFoundException("EQUIPMENT_NOT_FOUND", "Equipment not found");
    }

    private static BusinessConflictException duplicateInventoryCode() {
        return new BusinessConflictException(
                "DUPLICATE_INVENTORY_CODE", "Inventory code already registered");
    }

    private static BusinessConflictException equipmentHasHistory() {
        return new BusinessConflictException(
                "EQUIPMENT_HAS_HISTORY", "Equipment has loan history");
    }

    private record NormalizedEquipment(
            String inventoryCode, String name, String category, String description) {
    }
}
