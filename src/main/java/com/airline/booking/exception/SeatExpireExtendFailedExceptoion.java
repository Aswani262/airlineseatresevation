package com.airline.booking.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class SeatExpireExtendFailedExceptoion extends BusinessException {

    private static final String CODE = "SEAT_EXPIRED_EXTENDING_FAILED";
    private static final String MESSAGE = "Error while extending hold expire time of seat for payment failed due to an unexpected error.";

    public SeatExpireExtendFailedExceptoion( Throwable cause) {
        super(CODE, MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
