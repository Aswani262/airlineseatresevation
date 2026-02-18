package com.airline.flightmgmt.exception;

import com.airline.shared.exception.BusinessException;
import com.airline.shared.exception.Transient;
import org.springframework.http.HttpStatus;
//Create Separate Exception for seat locking failure to distinguish it from seat availability issues.
// This can help in implementing retry logic for transient errors.
public class SeatLockingFailedException extends BusinessException implements Transient {

    public SeatLockingFailedException(String details) {
        super("SEAT_LOCKING_FAILED", details , HttpStatus.CONFLICT);
    }
}
