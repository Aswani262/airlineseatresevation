package com.airline.shared.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(StructuralException.class)
    public ResponseEntity<ApiErrorResponse> handleStructural(StructuralException ex) {
        return ResponseEntity.status(ex.getHttpStatus()).body(
                ApiErrorResponse.of(
                        ex.getCode(),
                        ex.getMessage(),
                        ex.getNotification().view()
                )
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getHttpStatus()).body(
                ApiErrorResponse.of(
                        ex.getCode(),
                        ex.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiErrorResponse.of(
                        null,
                        ex.getMessage(),
                        null
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiErrorResponse.of(
                        null,
                        ex.getMessage(),
                        null
                )
        );
    }

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
