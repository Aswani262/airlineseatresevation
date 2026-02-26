package com.airline.flightmgmt.application.command.dto;

import java.util.List;
import java.util.UUID;

public record ReleaseBookedSeatCommand(UUID flightId, List<UUID> seatTemplateIds) {
}
