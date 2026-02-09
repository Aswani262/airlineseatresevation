package com.airline.flightmgmt.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatResponse {
    private UUID seatId;
    private String seatNumber;     // e.g. 12A
    private String fareClass;      // ECONOMY
    private String status;         // AVAILABLE
    private BigDecimal price;
}
