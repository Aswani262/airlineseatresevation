package com.airline.flightmgmt.domain;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
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

    //Use for partition
    private LocalDate flightDate;

    private int totalSeats;
    private int availableSeats;

    @Version
    private int version;
}
