package com.airline.flightmgmt.api.dto;

import com.airline.flightmgmt.domain.SeatStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class SeatAssignmentsResponse {
    private UUID seatTemplateId;
    private SeatStatus status;
}
