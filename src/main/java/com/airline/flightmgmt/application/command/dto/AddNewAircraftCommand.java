package com.airline.flightmgmt.application.command.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddNewAircraftCommand {

    @NotBlank
    @Size(max = 20)
    private String registrationNumber;

    @NotBlank
    @Size(max = 50)
    private String model;

    @NotBlank
    @Size(max = 50)
    private String manufacturer;

    @NotNull
    @Min(1)
    private Integer totalSeats;

    @NotNull
    @Size(min = 1)
    private Map<@NotBlank String, @NotNull @Min(0) Integer> configuration;
}
