package com.airline.booking.integration;

import java.util.List;
import java.util.UUID;

public interface SeatInventoryIntegrationService {

    void extendSeatExpiryTimeForPayment(UUID flightId, List<UUID> seatTemplateIds,UUID customerId,UUID bookingId);

    void bookHoldSeat(UUID flightId, List<UUID> seatTemplateIds, UUID customerId, UUID bookingId);

}
