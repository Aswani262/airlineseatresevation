package com.airline.booking.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class BookingHoldExpiredException extends BusinessException {

    private static final String CODE = "BOOKING_HOLD_EXPIRED";
    private static final String MESSAGE = "The booking hold has expired.";

    public BookingHoldExpiredException() {
        super(CODE, MESSAGE, HttpStatus.CONFLICT);
    }
}
