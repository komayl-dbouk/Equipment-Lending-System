package edu.university.equipmentlending.shared.error;

import org.springframework.http.HttpStatus;

public final class BusinessConflictException extends ApiException {
    public BusinessConflictException(String code, String publicMessage) {
        super(HttpStatus.CONFLICT, code, publicMessage);
    }
}
