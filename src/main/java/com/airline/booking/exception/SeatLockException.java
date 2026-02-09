package com.airline.booking.exception;

import java.util.List;

public class SeatLockException extends RuntimeException {
    
    private final List<String> lockedSeats;
    
    public SeatLockException(String message, List<String> lockedSeats) {
        super(message);
        this.lockedSeats = lockedSeats;
    }
    
    public List<String> getLockedSeats() {
        return lockedSeats;
    }
}
