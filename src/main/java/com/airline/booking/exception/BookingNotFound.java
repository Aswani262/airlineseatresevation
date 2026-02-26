package com.airline.booking.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class BookingNotFound extends BusinessException {

    private static final String CODE = "BOOKING_NOT_FOUND";
    private static final String MESSAGE = "The booking was not found.";

    public BookingNotFound(UUID bookingId) {
        super(CODE, MESSAGE + " Booking ID: " + bookingId.toString(), HttpStatus.BAD_REQUEST);
    }
}
