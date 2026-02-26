package com.airline.flightmgmt.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class SeatHoldingExpiredException extends BusinessException {
    public SeatHoldingExpiredException(String details) {
        super("SEAT_HOLDING_EXPIRED", details , HttpStatus.BAD_REQUEST);
    }
}
