package com.airline.shared.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(StructuralValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleStructural(StructuralValidationException ex) {
        return ResponseEntity.status(ex.getHttpStatus()).body(
                ApiErrorResponse.of(
                        ex.getCode(),
                        ex.getMessage(),
                        ex.getNotification().view()
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> illegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiErrorResponse.of(
                        null,
                        ex.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> illegalStateException(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiErrorResponse.of(
                        null,
                        ex.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessValidationException ex) {
        return ResponseEntity.status(ex.getHttpStatus()).body(
                ApiErrorResponse.of(
                        ex.getCode(),
                        ex.getMessage(),
                        null
                )
        );
    }

    // Optional fallback:
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.internalServerError().body(
                ApiErrorResponse.of(
                        "internal_server_error",
                        "Something went wrong",
                        null
                )
        );
    }
}
