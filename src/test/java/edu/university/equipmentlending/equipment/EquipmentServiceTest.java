package edu.university.equipmentlending.equipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.university.equipmentlending.equipment.dto.EquipmentResponse;
import edu.university.equipmentlending.equipment.dto.EquipmentWrite;
import edu.university.equipmentlending.equipment.persistence.EquipmentEntity;
import edu.university.equipmentlending.equipment.persistence.EquipmentRepository;
import edu.university.equipmentlending.loan.persistence.LoanRepository;
import edu.university.equipmentlending.shared.error.ApiException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {
    @Mock
    private EquipmentRepository equipmentRepository;
    @Mock
    private LoanRepository loanRepository;

    private EquipmentService service;

    @BeforeEach
    void setUp() {
        service = new EquipmentService(equipmentRepository, loanRepository);
    }

    @Test
    void createNormalizesFieldsAndDerivesAvailability() {
        when(equipmentRepository.existsByInventoryCode("CAM-001")).thenReturn(false);
        when(loanRepository.existsByEquipmentIdAndReturnDateIsNull(any())).thenReturn(false);

        EquipmentResponse response = service.create(new EquipmentWrite(
                "  cam-001  ", "  Demo Camera  ", "  Camera  ", "  Lab use  "));

        ArgumentCaptor<EquipmentEntity> saved = ArgumentCaptor.forClass(EquipmentEntity.class);
        verify(equipmentRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getInventoryCode()).isEqualTo("CAM-001");
        assertThat(saved.getValue().getName()).isEqualTo("Demo Camera");
        assertThat(saved.getValue().getCategory()).isEqualTo("Camera");
        assertThat(saved.getValue().getDescription()).isEqualTo("Lab use");
        assertThat(response.available()).isTrue();
    }

    @Test
    void duplicateCreateUsesStableConflictAndDoesNotWrite() {
        when(equipmentRepository.existsByInventoryCode("CAM-001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(write("cam-001")))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(409);
                    assertThat(exception.getCode()).isEqualTo("DUPLICATE_INVENTORY_CODE");
                });
        verify(equipmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void confirmedDatabaseUniquenessRaceUsesDuplicateConflict() {
        when(equipmentRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "write failed", new SQLException(
                        "duplicate key violates constraint equipment_inventory_code_key", "23505")));

        assertThatThrownBy(() -> service.create(write("cam-001")))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("DUPLICATE_INVENTORY_CODE"));
    }

    @Test
    void unrelatedPersistenceFailureIsNotRecastAsConflict() {
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
                "write failed", new SQLException("other constraint", "23514"));
        when(equipmentRepository.saveAndFlush(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.create(write("cam-001"))).isSameAs(failure);
    }

    @Test
    void listPreservesRepositoryIdOrderAndDerivesEachAvailability() {
        EquipmentEntity first = entity(1L, "CAM-001");
        EquipmentEntity second = entity(2L, "LAP-001");
        when(equipmentRepository.findAllByOrderByIdAsc()).thenReturn(List.of(first, second));
        when(loanRepository.existsByEquipmentIdAndReturnDateIsNull(1L)).thenReturn(true);
        when(loanRepository.existsByEquipmentIdAndReturnDateIsNull(2L)).thenReturn(false);

        List<EquipmentResponse> responses = service.list();

        assertThat(responses).extracting(EquipmentResponse::id).containsExactly(1L, 2L);
        assertThat(responses).extracting(EquipmentResponse::available)
                .containsExactly(false, true);
    }

    @Test
    void updateMayRetainOwnCodeAndReplacementClearsOmittedDescription() {
        EquipmentEntity existing = entity(7L, "CAM-001");
        when(equipmentRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(equipmentRepository.existsByInventoryCodeAndIdNot("CAM-001", 7L)).thenReturn(false);

        EquipmentResponse response = service.update(7L, new EquipmentWrite(
                " cam-001 ", " Replacement Name ", " Camera ", null));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.inventoryCode()).isEqualTo("CAM-001");
        assertThat(response.name()).isEqualTo("Replacement Name");
        assertThat(response.description()).isNull();
        verify(equipmentRepository).saveAndFlush(existing);
    }

    @Test
    void updateRejectsAnotherItemsNormalizedCodeWithoutMutation() {
        EquipmentEntity existing = entity(7L, "CAM-001");
        when(equipmentRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(equipmentRepository.existsByInventoryCodeAndIdNot("LAP-001", 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(7L, write(" lap-001 ")))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("DUPLICATE_INVENTORY_CODE"));
        assertThat(existing.getInventoryCode()).isEqualTo("CAM-001");
        verify(equipmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void missingUpdateUsesDocumentedNotFoundError() {
        when(equipmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, write("CAM-001")))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(404);
                    assertThat(exception.getCode()).isEqualTo("EQUIPMENT_NOT_FOUND");
                });
    }

    @Test
    void deleteLocksAndRemovesUnreferencedEquipment() {
        EquipmentEntity existing = entity(3L, "CAM-001");
        when(equipmentRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(existing));

        service.delete(3L);

        verify(loanRepository).existsByEquipmentId(3L);
        verify(equipmentRepository).delete(existing);
        verify(equipmentRepository).flush();
    }

    @Test
    void deleteRejectsAnyHistoryAndPreservesEquipment() {
        EquipmentEntity existing = entity(3L, "CAM-001");
        when(equipmentRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(existing));
        when(loanRepository.existsByEquipmentId(3L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("EQUIPMENT_HAS_HISTORY"));
        verify(equipmentRepository, never()).delete(any());
    }

    @Test
    void missingDeleteUsesDocumentedNotFoundError() {
        when(equipmentRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo("EQUIPMENT_NOT_FOUND"));
    }

    @Test
    void rejectsInventoryCodeThatExceedsLimitAfterUppercaseExpansion() {
        String rawCode = "a".repeat(49) + "ß";

        assertThatThrownBy(() -> service.create(write(rawCode)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(400);
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_FAILED");
                });
        verify(equipmentRepository, never()).saveAndFlush(any());
    }

    private static EquipmentWrite write(String code) {
        return new EquipmentWrite(code, "Demo Camera", "Camera", "Description");
    }

    private static EquipmentEntity entity(long id, String code) {
        EquipmentEntity entity = new EquipmentEntity(code, "Demo Camera", "Camera", "Description");
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
