package edu.university.equipmentlending.equipment.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "equipment")
public class EquipmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_code", nullable = false, unique = true, length = 50)
    private String inventoryCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(length = 1000)
    private String description;
}
