package com.airline.booking.service.core;

import com.airline.shared.annotation.CoreService;

import java.security.SecureRandom;

@CoreService
public class TicketingCoreService implements ITicketingService {

    private static final SecureRandom RND = new SecureRandom();
    // Example format: 3-digit airline + 10-digit number (e-ticket style)
    // You can make it more realistic later.
    @Override
    public String generateTicketNumber() {
        long n = Math.abs(RND.nextLong()) % 1_000_000_0000L; // 10 digits
        return "176" + String.format("%010d", n);
    }
}
