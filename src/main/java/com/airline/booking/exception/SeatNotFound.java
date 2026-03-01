package com.airline.booking.exception;

import com.airline.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.Objects;
import java.util.UUID;

public class SeatNotFound extends BusinessException {

    private static final String CODE = "SEAT_NOT_FOUND";
    private static final String MESSAGE = "The seat was not found Seat Template Id : %s";

    public SeatNotFound(UUID seatTemplateId) {
        super(CODE, String.format(MESSAGE, Objects.requireNonNull(seatTemplateId, "seatTemplateId cannot be null")), HttpStatus.BAD_REQUEST);
    }
}
