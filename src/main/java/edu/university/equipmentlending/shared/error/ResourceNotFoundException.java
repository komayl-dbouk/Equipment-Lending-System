package edu.university.equipmentlending.shared.error;

import org.springframework.http.HttpStatus;

public final class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String code, String publicMessage) {
        super(HttpStatus.NOT_FOUND, code, publicMessage);
    }
}
