package com.airline.booking.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class BookingNotFound extends BusinessException {

    private static final String CODE = "BOOKING_NOT_FOUND";
    private static final String MESSAGE = "The booking was not found.";

    public BookingNotFound(String bookingId) {
        super(CODE, MESSAGE + " Booking ID: " + bookingId, HttpStatus.BAD_REQUEST);
    }
}
