package com.airline.flightmgmt.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FlightSearchResponse {
    private UUID flightId;
    private String flightNumber;
    private OffsetDateTime departureTime;
    private OffsetDateTime arrivalTime;
    private String status;
    private BigDecimal basePrice;
}
