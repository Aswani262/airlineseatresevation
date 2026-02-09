package com.airline.shared.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidationError {
    private final String code;
    private final String field;
    private final String message;
}
