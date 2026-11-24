package edu.university.equipmentlending.shared.error;

import java.time.Instant;
import java.util.Map;

/** Public error contract; never include rejected values or exception details. */
public record ApiError(
        Instant timestamp, int status, String code, String message,
        String path, Map<String, String> fieldErrors) {
    public ApiError {
        fieldErrors = Map.copyOf(fieldErrors);
    }
}
