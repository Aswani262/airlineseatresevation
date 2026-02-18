package com.airline.booking.servcie;

import com.airline.booking.service.core.TicketingCoreService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TicketingCoreServiceTest {

    private final TicketingCoreService ticketingCoreService = new TicketingCoreService();

    @Test
    void generateTicketNumber_returnsCorrectFormat() {
        // Act
        String ticketNumber = ticketingCoreService.generateTicketNumber();

        // Assert
        assertNotNull(ticketNumber);
        assertEquals(13, ticketNumber.length());
        assertTrue(ticketNumber.startsWith("176"));
        
        String digits = ticketNumber.substring(3);
        assertEquals(10, digits.length());
        assertTrue(digits.matches("\\d{10}"), "Digits part should be 10 numeric digits: " + digits);
    }

    @Test
    void generateTicketNumber_generatesDifferentNumbers() {
        // Act
        String ticket1 = ticketingCoreService.generateTicketNumber();
        String ticket2 = ticketingCoreService.generateTicketNumber();

        // Assert
        assertNotEquals(ticket1, ticket2, "Generated tickets should be different");
    }
}