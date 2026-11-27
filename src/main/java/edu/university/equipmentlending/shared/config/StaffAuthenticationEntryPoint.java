package edu.university.equipmentlending.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.university.equipmentlending.shared.error.ApiError;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Emits the public API error shape without exposing authentication failure details. */
@Component
public final class StaffAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final String CHALLENGE = "Basic realm=\"equipment-lending\"";
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public StaffAuthenticationEntryPoint(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, CHALLENGE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiError(
                Instant.now(clock), HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED",
                "Staff credentials required", request.getRequestURI(), Map.of()));
    }
}
