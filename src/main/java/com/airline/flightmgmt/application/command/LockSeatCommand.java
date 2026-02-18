package com.airline.flightmgmt.application.command;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;


public record LockSeatCommand (
     UUID flightId,
     List<String> seatNumber,
     UUID bookingId,
     int holdMinutes
     ) {
}
