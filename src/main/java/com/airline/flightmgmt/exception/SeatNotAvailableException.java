package com.airline.flightmgmt.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.List;

public class SeatNotAvailableException extends BusinessException {

    public SeatNotAvailableException(String message) {
        super("SEAT_NOT_AVAILABLE",message, HttpStatus.CONFLICT);
    }
}
