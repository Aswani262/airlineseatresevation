package com.airline.flightmgmt.api.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAircraftResponse {
    private UUID id;
}
