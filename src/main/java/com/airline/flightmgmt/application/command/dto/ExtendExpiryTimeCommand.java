package com.airline.flightmgmt.application.command.dto;

import java.util.List;
import java.util.UUID;

public record ExtendExpiryTimeCommand (UUID flightId, List<UUID> seatTemplateIds,UUID customerId, UUID bookingId){
}
