package com.airline.flightmgmt.service;

import com.airline.flightmgmt.domain.HoldStage;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.shared.model.SeatLockResult;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface ISeatInventoryService {
    SeatLockResult holdSeats(List<SeatAssignments> seats, Duration ttl);

    SeatLockResult extendSeatExpiryForPayment(List<SeatAssignments> seats, Duration paymentWindow);

    void confirmLockedSeatsOrThrow(List<SeatAssignments> seats);

    void releaseBookedSeats(List<SeatAssignments> seats);

    void validateAndPrepareSeatsForHolding(List<SeatAssignments> seatsToLock, HoldStage holdStage);

    void validateForPaymentExtension(List<SeatAssignments> seats);

    void releaseLockedSeats(List<SeatAssignments> seats);
}
