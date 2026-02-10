package com.airline.flightmgmt.domain;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("aircrafts")
public class Aircraft extends BaseEntity {


    private UUID id;

    private String registrationNumber;
    private String model;
    private String manufacturer;
    private Integer totalSeats;

    // Stored as JSONB in Postgres
    //Its a map of fare class code to number of seats in that class
    private Map<String, Integer> configuration;

}
