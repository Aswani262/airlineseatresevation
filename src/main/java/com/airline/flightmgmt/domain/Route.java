package com.airline.flightmgmt.domain;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("routes")
// Root cannot be aggregate because it cannot be share between flights
public class Route extends BaseEntity {

    @Id
    private UUID id;

    private String originAirport;
    private String destinationAirport;
    private Integer distanceKm;
    private Integer estimatedDurationMinutes;
    private Boolean isInternational;

    @Version
    private int version;
}
