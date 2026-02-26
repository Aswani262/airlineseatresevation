package com.airline.flightmgmt.exception;

import com.airline.shared.exception.StructuralException;

public class SeatCannotBlankException extends StructuralException {
    public SeatCannotBlankException() {
        super("SEAT_CANNOT_BLANK", "seats", "Seat cannot be blank");
    }
}
