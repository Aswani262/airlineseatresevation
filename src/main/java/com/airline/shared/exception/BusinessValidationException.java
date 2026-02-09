package com.airline.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessValidationException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public BusinessValidationException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
