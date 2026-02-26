package com.airline.booking.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class BookingHoldSeatNotAvailable extends BusinessException {

    private static final String CODE = "BOOKING_HOLD_SEAT_NOT_AVAILABLE";
    private static final String MESSAGE = "One of the seat which hold previously is expired or not available";

    public BookingHoldSeatNotAvailable() {
        super(CODE, MESSAGE, HttpStatus.BAD_REQUEST);
    }
}
