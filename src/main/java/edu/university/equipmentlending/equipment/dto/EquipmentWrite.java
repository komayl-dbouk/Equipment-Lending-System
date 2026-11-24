package edu.university.equipmentlending.equipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipmentWrite(
        @NotBlank @Size(max = 50) String inventoryCode,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 60) String category,
        @Size(max = 1000) String description) {
}
