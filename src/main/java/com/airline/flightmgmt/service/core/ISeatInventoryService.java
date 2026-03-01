package com.airline.flightmgmt.service.core;

import com.airline.flightmgmt.application.command.dto.ExtendExpiryTimeCommand;
import com.airline.flightmgmt.application.command.dto.HoldSeatCommand;
import com.airline.flightmgmt.domain.HoldStage;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.shared.exception.ErrorNotification;
import com.airline.shared.model.SeatLockResult;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface ISeatInventoryService {
    SeatLockResult holdSeats(List<SeatAssignments> seats, Duration ttl);
    SeatLockResult extendSeatExpiryForPayment(List<SeatAssignments> seats, Duration paymentWindow,UUID customerId,UUID bookingId);
    void bookedHoldSeatsOrThrow(List<SeatAssignments> seats,UUID bookingId);
    void validateAndPrepareSeatsForHolding(List<SeatAssignments> seatsToLock, HoldStage holdStage,UUID customerId);
    void validateForPaymentExtension(List<SeatAssignments> seats,UUID customerId);

    //Move this validation to validator class
    ErrorNotification validate(HoldSeatCommand command);
    ErrorNotification validate(ExtendExpiryTimeCommand command);
}
