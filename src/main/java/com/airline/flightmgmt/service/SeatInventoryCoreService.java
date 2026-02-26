package com.airline.flightmgmt.service;

import com.airline.flightmgmt.domain.HoldStage;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.domain.SeatStatus;
import com.airline.flightmgmt.exception.SeatAlreadyHeldException;
import com.airline.flightmgmt.exception.SeatCannotBlankException;
import com.airline.flightmgmt.exception.SeatHoldingExpiredException;
import com.airline.flightmgmt.exception.SeatNotAvailableException;
import com.airline.shared.annotation.CoreService;
import com.airline.shared.model.SeatLockResult;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@CoreService
@RequiredArgsConstructor
public class SeatInventoryCoreService implements ISeatInventoryService {

    @Override
    public SeatLockResult holdSeats(List<SeatAssignments> seats, Duration ttl) {
        if (seats.isEmpty()) {
            throw new SeatCannotBlankException();
        }

        OffsetDateTime expiresAt = OffsetDateTime.now(Clock.systemUTC()).plusSeconds(ttl.getSeconds());

        for (SeatAssignments seat : seats) {
            seat.setStatus(SeatStatus.HOLD);
            seat.setLockExpiresAt(expiresAt);
        }

        return new SeatLockResult(true, expiresAt);
    }

    public void validateAndPrepareSeatsForHolding(List<SeatAssignments> seats, HoldStage stage) {
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());

        for (SeatAssignments seat : seats) {

            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw new SeatNotAvailableException("Seat is already booked");
            }

            if (seat.getStatus() == SeatStatus.HOLD) {

                boolean isExpired =  now.isAfter(seat.getLockExpiresAt());

                if (isExpired) {
                    throw new SeatHoldingExpiredException("One or more seat is holding is expired");
                } else {
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setLockExpiresAt(null);
                    seat.setHoldStage(stage);
                    seat.setBookingId(null);
                }
            }
        }
    }

    @Override
    public SeatLockResult extendSeatExpiryForPayment(List<SeatAssignments> seats, Duration paymentWindow) {
        if (seats.isEmpty()) {
            throw new SeatCannotBlankException();
        }
        validateForPaymentExtension(seats);

        OffsetDateTime expiresAt = OffsetDateTime.now(Clock.systemUTC()).plus(paymentWindow);

        for (SeatAssignments seat : seats) {
            seat.setLockExpiresAt(expiresAt);
            seat.setHoldStage(HoldStage.PAYMENT);
        }
        return new SeatLockResult(true, expiresAt);
    }


    // Your existing methods (slightly cleaned for consistency)
    @Override
    public void confirmLockedSeatsOrThrow(List<SeatAssignments> seats) {
        if (seats.isEmpty()) {
            throw new SeatCannotBlankException();
        }

        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());

        for (SeatAssignments seat : seats) {
            if (seat.getStatus() == SeatStatus.BOOKED) {
                continue;
            }
            if (seat.getStatus() != SeatStatus.HOLD ||
                    (seat.getLockExpiresAt() != null && now.isAfter(seat.getLockExpiresAt()))) {
                throw new SeatNotAvailableException(
                        "Seat " + seat.getSeatTemplateId() + " is not locked or lock has expired");
            }
        }

        for (SeatAssignments seat : seats) {
            if (seat.getStatus() == SeatStatus.HOLD) {
                seat.setStatus(SeatStatus.BOOKED);
                seat.setLockExpiresAt(null);
            }
        }
    }

    @Override
    public void releaseBookedSeats(List<SeatAssignments> seats) {
        for (SeatAssignments seat : seats) {
            if (seat.getStatus() == SeatStatus.BOOKED) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setBookingId(null);
                seat.setLockExpiresAt(null);
                seat.setHoldStage(null);
            }
        }
    }

    @Override
    public void validateForPaymentExtension(List<SeatAssignments> seats) {
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());

        for (SeatAssignments seat : seats) {
            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw new SeatNotAvailableException("Seat is already booked");
            }

            if (seat.getStatus() != SeatStatus.HOLD) {
                throw new SeatNotAvailableException("Seat is not held (cannot extend)");
            }

            if (now.isAfter(seat.getLockExpiresAt())) {
                throw new SeatHoldingExpiredException("Seat holding time expired. Please select seat again.");
            }

        }
    }

    @Override
    public void releaseLockedSeats(List<SeatAssignments> seats) {
        if (seats.isEmpty()) {
            return;
        }

        for (SeatAssignments seat : seats) {
            if (seat.getStatus() == SeatStatus.HOLD) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setBookingId(null);
                seat.setLockExpiresAt(null);
                seat.setHoldStage(null);
            }
        }
    }
}