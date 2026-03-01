package com.airline.booking.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class BookHoldSeatFailedExceptoion  extends BusinessException {

    private static final String CODE = "BOOK_SEAT_FAILED";
    private static final String MESSAGE = "Error while booking the hold  seat due to an unexpected error.";

    public BookHoldSeatFailedExceptoion(Throwable cause) {
        super(CODE, MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
