package com.airline.flightmgmt.application.command.dto;

import com.airline.flightmgmt.domain.HoldStage;

import java.util.List;
import java.util.UUID;

public record ExtendExpiryTimeCommand (UUID flightId, List<UUID> seatTemplateIds, HoldStage holdStage){
}
