package edu.university.equipmentlending.student.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.university.equipmentlending.shared.config.StrictTextDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Raw limits run before the service normalizes and checks email syntax. */
public record StudentWrite(
        @JsonDeserialize(using = StrictTextDeserializer.class)
        @NotBlank @Size(max = 120) @Pattern(regexp = "[^\\x00]*") String name,
        @JsonDeserialize(using = StrictTextDeserializer.class)
        @NotBlank @Size(max = 254) @Pattern(regexp = "[^\\x00]*")
        @Email(groups = Normalized.class) String email) {
    public interface Normalized { }
}
