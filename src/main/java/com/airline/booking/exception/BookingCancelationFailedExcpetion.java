package com.airline.booking.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class BookingCancelationFailedExcpetion extends BusinessException {
    private static final String CODE = "BOOKING_CANCELLED_FAILED";
    private static final String MESSAGE = "The booking cancelled process failed due to an unexpected error.";

    public BookingCancelationFailedExcpetion(Throwable cause){
        super(CODE, MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR,cause);
    }
    public BookingCancelationFailedExcpetion(String message){
        super(CODE, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
