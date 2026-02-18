package com.airline.flightmgmt.domain;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("flights")
public class Flight extends BaseEntity {

    @Id
    private UUID id;
    private String flightNumber;
    private UUID aircraftId;
    private UUID routeId;
    private OffsetDateTime departureTime;
    private OffsetDateTime arrivalTime;
    private FlightStatus status;

    // Base price for each fare class, used for dynamic pricing and fare calculations
    private BigDecimal basePrice;

    private int totalSeats;
    private int availableSeats;

    //Copy from Aircraft configuration while create a flight, because
    // we need to keep track of available seats by fare class for booking purposes
    private Map<FareClass, Integer>  seatConfiguration;

    @Version
    private int version;
}
