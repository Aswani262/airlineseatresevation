package com.airline.booking.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class SeatNotFound extends BusinessException {

    private static final String CODE = "SEAT_NOT_FOUND";
    private static final String MESSAGE = "The seat was not found.";

    public SeatNotFound() {
        super(CODE, MESSAGE , HttpStatus.BAD_REQUEST);
    }
}
