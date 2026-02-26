package com.airline.flightmgmt.application.command.dto;

import java.util.List;
import java.util.UUID;

public record CheckSeatHoldStatusCommand(UUID fightId, List<UUID> seatTemaplateIds, UUID customerId) {
}
