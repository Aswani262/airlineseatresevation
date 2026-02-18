package com.airline.booking.application.command.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiateBookingSeatCommand {

    private UUID flightId;
    private UUID customerId;

    private String currency; // INR/USD

    private List<Passenger> passengers;
    private List<SeatSelection> seatSelections;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Passenger {
        private String firstName;
        private String lastName;
        private String passengerType; // ADULT/CHILD/INFANT
        private String email;
        private String phone;
        private String passportNumber;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SeatSelection {
        private Integer passengerIndex; // index into passengers list
        private String seatNumber;      // E12
        private String fareClass;       // ECONOMY_SAVER
        private BigDecimal price;
    }
}
