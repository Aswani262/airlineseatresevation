package com.airline.flightmgmt.api.dto;

import com.airline.flightmgmt.domain.SeatType;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatAvalibityResponse {
    private UUID seatTemplateId;
    private String seatNumber;     // e.g. 12A
    private String fareClass;      // ECONOMY
    private String status;         // AVAILABLE
    private BigDecimal price;
    private SeatType seatType;
}
