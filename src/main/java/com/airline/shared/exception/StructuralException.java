package com.airline.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

//All structural validation errors
// (e.g., invalid input format, missing required fields) should be categorized under this exception.
// These errors indicate that the request cannot be processed due to issues with the structure of the input data,
// and they are not expected to be resolved by retrying the same request without modification.
@Getter
public class StructuralException extends RuntimeException implements NonTransient {

    private final String code;
    private final HttpStatus httpStatus;
    private final ErrorNotification notification;

    public StructuralException(ErrorNotification notification) {
        super("Structural validation failed");
        this.code = "structural_validation_failed";
        this.httpStatus = HttpStatus.BAD_REQUEST;
        this.notification = notification;
    }

    public StructuralException(String code,String field ,String message) {
        super("Structural validation failed");
        this.code = code;
        this.httpStatus = HttpStatus.BAD_REQUEST;
        ErrorNotification errorNotification = new ErrorNotification();
        errorNotification.add(code, field, message);
        this.notification = errorNotification;
    }
}
