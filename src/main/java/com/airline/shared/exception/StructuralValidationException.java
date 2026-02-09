package com.airline.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StructuralValidationException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;
    private final Notification notification;

    public StructuralValidationException(Notification notification) {
        super("Structural validation failed");
        this.code = "structural_validation_failed";
        this.httpStatus = HttpStatus.BAD_REQUEST;
        this.notification = notification;
    }
}
