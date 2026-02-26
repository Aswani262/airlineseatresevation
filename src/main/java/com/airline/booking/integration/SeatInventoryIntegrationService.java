package com.airline.booking.integration;

import com.airline.flightmgmt.api.dto.SeatBookedResult;
import com.airline.flightmgmt.api.dto.SeatReleaseResult;
import com.airline.shared.model.SeatLockResult;

import java.util.List;
import java.util.UUID;

public interface SeatInventoryIntegrationService {

    void extendSeatExpiryTimeForPayment(UUID flightId, List<UUID> seatTemplateIds);

    void confirmSeat(UUID flightId, List<UUID> seatTemplateIds);


}
