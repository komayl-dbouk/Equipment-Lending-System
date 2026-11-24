package edu.university.equipmentlending.shared.error;

import org.springframework.http.HttpStatus;

/** An intentional client error with a stable code and a safe public message. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String publicMessage) {
        super(publicMessage);
        if (!status.is4xxClientError()) {
            throw new IllegalArgumentException("ApiException requires a client-error status");
        }
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
}
