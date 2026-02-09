package com.airline.shared.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ApiErrorResponse {
    private String code;
    private String message;
    private OffsetDateTime timestamp;
    private List<?> details;

    public static ApiErrorResponse of(String code, String message, List<?> details) {
        return new ApiErrorResponse(code, message, OffsetDateTime.now(), details);
    }
}
