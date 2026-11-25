package edu.university.equipmentlending.equipment.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.university.equipmentlending.shared.config.StrictTextDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EquipmentWrite(
        @JsonDeserialize(using = StrictTextDeserializer.class)
        @NotBlank @Size(max = 50) @Pattern(regexp = "[^\\x00]*") String inventoryCode,
        @JsonDeserialize(using = StrictTextDeserializer.class)
        @NotBlank @Size(max = 120) @Pattern(regexp = "[^\\x00]*") String name,
        @JsonDeserialize(using = StrictTextDeserializer.class)
        @NotBlank @Size(max = 60) @Pattern(regexp = "[^\\x00]*") String category,
        @JsonDeserialize(using = StrictTextDeserializer.class)
        @Size(max = 1000) @Pattern(regexp = "[^\\x00]*") String description) {
}
