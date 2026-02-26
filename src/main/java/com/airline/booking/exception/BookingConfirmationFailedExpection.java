package com.airline.booking.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class BookingConfirmationFailedExpection extends BusinessException {

    private static final String CODE = "BOOKING_CONFIRMATION_FAILED";
    private static final String MESSAGE = "The booking process failed due to an unexpected error.";

    public BookingConfirmationFailedExpection(String details) {
        super(CODE, MESSAGE + " Details: " + details, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
