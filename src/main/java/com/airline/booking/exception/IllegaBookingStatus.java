package com.airline.booking.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class IllegaBookingStatus extends BusinessException {

    private static final String CODE = "ILLEGAL_BOOKING_STATUS";
    //private static final String MESSAGE = "The booking status is illegal for this operation.";

    public IllegaBookingStatus(String message) {
         super(CODE, message, HttpStatus.CONFLICT);
    }
}
