package com.airline.flightmgmt.domain;

import com.airline.shared.model.BaseEntity;
import lombok.*;



import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route extends BaseEntity {

    private UUID id;

    private String originAirport;
    private String destinationAirport;
    private Integer distanceKm;
    private Integer estimatedDurationMinutes;
    private Boolean isInternational;

}
