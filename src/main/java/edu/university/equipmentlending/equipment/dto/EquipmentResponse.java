package edu.university.equipmentlending.equipment.dto;

public record EquipmentResponse(
        Long id,
        String inventoryCode,
        String name,
        String category,
        String description,
        boolean available) {
}
