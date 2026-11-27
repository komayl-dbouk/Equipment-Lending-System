package edu.university.equipmentlending.shared.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Required private credential for the single staff delivery boundary. */
@Validated
@ConfigurationProperties("app.security.staff")
public record StaffAccessProperties(
        @NotBlank String username,
        @NotBlank String password) {
}
