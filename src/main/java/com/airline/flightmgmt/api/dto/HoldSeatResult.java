package com.airline.flightmgmt.api.dto;

import lombok.RequiredArgsConstructor;


public record HoldSeatResult (
     boolean success,
     String message) {
}
