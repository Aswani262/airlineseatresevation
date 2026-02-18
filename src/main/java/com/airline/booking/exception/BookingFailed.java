package com.airline.booking.exception;

import com.airline.shared.exception.BusinessException;
import com.airline.shared.exception.Transient;
import org.springframework.http.HttpStatus;

public class BookingFailed  extends BusinessException implements Transient {

    private static final String CODE = "BOOKING_FAILED";
    private static final String MESSAGE = "The booking process failed due to an unexpected error.";

    public BookingFailed(String details) {
        super(CODE, MESSAGE + " Details: " + details, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
