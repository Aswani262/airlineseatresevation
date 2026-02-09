package com.airline.flightmgmt.domain;

import com.airline.shared.model.BaseEntity;
import lombok.*;



import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flight extends BaseEntity {

    private UUID id;
    private String flightNumber;
    private UUID aircraftId;
    private UUID routeId;
    private OffsetDateTime departureTime;
    private OffsetDateTime arrivalTime;
    private FlightStatus status;
    private BigDecimal basePrice;

}
