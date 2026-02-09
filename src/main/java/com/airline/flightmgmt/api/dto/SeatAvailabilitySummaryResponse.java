package com.airline.flightmgmt.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeatAvailabilitySummaryResponse {
    private UUID flightId;
    private List<FareClassAvailability> availability;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FareClassAvailability {
        private String fareClass;
        private long available;
        private long locked;
        private long booked;
        private BigDecimal minPrice; // cheapest AVAILABLE seat
    }
}
