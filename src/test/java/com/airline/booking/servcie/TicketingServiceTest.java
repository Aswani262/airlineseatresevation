package com.airline.booking.servcie;

import com.airline.booking.service.TicketingService;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class TicketingServiceTest {

    private final TicketingService service = new TicketingService();

    @Test
    void generateTicketNumber_shouldFollowExpectedFormat() {
        String ticket = service.generateTicketNumber();

        assertThat(ticket).isNotNull();
        assertThat(ticket).hasSize(13);                // 3 (prefix) + 10 digits
        assertThat(ticket).startsWith("176");

        // Only digits after prefix
        assertThat(ticket.substring(3)).matches("\\d{10}");

        // Full regex check
        assertThat(ticket).matches("176\\d{10}");
    }

    @Test
    void generateTicketNumber_shouldZeroPadTo10Digits() {
        // Call multiple times to increase chance of small numbers
        for (int i = 0; i < 20; i++) {
            String ticket = service.generateTicketNumber();
            String numericPart = ticket.substring(3);

            assertThat(numericPart).hasSize(10);
            assertThat(numericPart).matches("\\d{10}");
        }
    }

    @Test
    void generateTicketNumber_shouldGenerateDifferentValuesAcrossCalls() {
        Set<String> tickets = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            tickets.add(service.generateTicketNumber());
        }

        // Not a strict randomness test, just a sanity check
        assertThat(tickets.size())
                .isGreaterThan(1);  // extremely unlikely to all be same
    }

    @Test
    void generateTicketNumber_shouldContainOnlyDigitsAfterPrefix() {
        String ticket = service.generateTicketNumber();

        String numericPart = ticket.substring(3);
        assertThat(numericPart.chars().allMatch(Character::isDigit)).isTrue();
    }
}
