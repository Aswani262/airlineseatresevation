package com.airline.flightmgmt.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class SeatAlreadyHeldException extends BusinessException {

    public SeatAlreadyHeldException(String message) {
        super("SEAT_ALREADY_HELD", message, HttpStatus.BAD_REQUEST);
    }
}
