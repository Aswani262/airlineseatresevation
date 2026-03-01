package com.airline.flightmgmt.service.core;

import com.airline.flightmgmt.application.command.dto.ExtendExpiryTimeCommand;
import com.airline.flightmgmt.application.command.dto.HoldSeatCommand;
import com.airline.flightmgmt.domain.HoldStage;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.domain.SeatStatus;
import com.airline.flightmgmt.exception.SeatAlreadyHeldException;
import com.airline.flightmgmt.exception.SeatCannotBlankException;
import com.airline.flightmgmt.exception.SeatHoldingExpiredException;
import com.airline.flightmgmt.exception.SeatNotAvailableException;
import com.airline.shared.annotation.CoreService;
import com.airline.shared.exception.ErrorNotification;
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

    @Override
    public void validateAndPrepareSeatsForHolding(List<SeatAssignments> seats,
                                                  HoldStage stage,UUID customerId) {
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());

        for (SeatAssignments seat : seats) {
            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw new SeatNotAvailableException("Seat is already booked");
            }

            if (seat.getStatus() == SeatStatus.HOLD) {
                //Not expired
                if (!now.isAfter(seat.getLockExpiresAt())) {
                    //If hold another user then throw exception , if not then do nothing that will still with current customer
                    if (!Objects.equals(seat.getCustomerId(), customerId)) {
                        throw new SeatAlreadyHeldException("Seat is held by another user");
                    }
                } else {
                    // Expired allow re-hold  customer
                    seat.setStatus(SeatStatus.AVAILABLE);
                    seat.setLockExpiresAt(null);
                    seat.setHoldStage(stage);
                    seat.setCustomerId(customerId);
                    seat.setBookingId(null);
                }
            }
        }
    }

    @Override
    public SeatLockResult extendSeatExpiryForPayment(List<SeatAssignments> seats, Duration paymentWindow,UUID customerId,UUID bookingId) {
        if (seats.isEmpty()) {
            throw new SeatCannotBlankException();
        }
        validateForPaymentExtension(seats,customerId);

        OffsetDateTime expiresAt = OffsetDateTime.now(Clock.systemUTC()).plus(paymentWindow);

        for (SeatAssignments seat : seats) {
            seat.setLockExpiresAt(expiresAt);
            seat.setHoldStage(HoldStage.PAYMENT);
            seat.setCustomerId(customerId);
            seat.setBookingId(bookingId);
        }
        return new SeatLockResult(true, expiresAt);
    }


    // Your existing methods (slightly cleaned for consistency)
    @Override
    public void bookedHoldSeatsOrThrow(List<SeatAssignments> seats,UUID bookingId) {
        if (seats.isEmpty()) {
            throw new SeatCannotBlankException();
        }

        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());

        for (SeatAssignments seat : seats) {

            //Idempotent
            if (seat.getStatus() == SeatStatus.BOOKED) {

                if(!seat.getBookingId().equals(bookingId)){
                   throw new SeatNotAvailableException(
                            "Seat " + seat.getSeatTemplateId() + " is booked by another user");
                }
                continue;
            }

            //Seat is locked by another user
            if(seat.getStatus() == SeatStatus.HOLD && !seat.getBookingId().equals(bookingId) &&
                    (seat.getLockExpiresAt() != null && !now.isAfter(seat.getLockExpiresAt()))){
                throw new SeatNotAvailableException(
                        "Seat " + seat.getSeatTemplateId() + " is  locked by another user");
            }


            //Seat lock is expired
            if ((seat.getStatus() != SeatStatus.HOLD && seat.getBookingId().equals(bookingId)) ||
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
    public void validateForPaymentExtension(List<SeatAssignments> seats,UUID customerId) {
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC());

        for (SeatAssignments seat : seats) {
            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw new SeatNotAvailableException("Seat is already booked");
            }

            if (seat.getStatus() != SeatStatus.HOLD) {
                throw new SeatNotAvailableException("Seat is not held cannot extend");
            }

            if (now.isAfter(seat.getLockExpiresAt())) {
                throw new SeatHoldingExpiredException("Seat holding time expired. Please select seat again.");
            }

            if (!seat.getCustomerId().equals(customerId)) {
                throw new SeatNotAvailableException("Seat is acquired by another customer, Please select another");
            }

        }
    }

    @Override
    public ErrorNotification validate(HoldSeatCommand cmd) {
        ErrorNotification notification = new ErrorNotification();

        if (cmd == null) {
            notification.add("command_required", "command", "command is required");
            return notification;
        }

        if (cmd.flightId() == null) {
            notification.add("flight_id_required", "flightId", "flightId is required");
        }

        if (cmd.customerId() == null) {
            notification.add("customer_id_required", "customerId", "customerId is required");
        }

        if (cmd.seatTemplateId() == null || cmd.seatTemplateId().isEmpty()) {
            notification.add("seat_template_id_required", "seatTemplateId", "seatTemplateId is required");
        }

      return notification;

    }

    @Override
    public ErrorNotification validate(ExtendExpiryTimeCommand cmd) {
        ErrorNotification notification = new ErrorNotification();

        if (cmd == null) {
            notification.add("command_required", "command", "command is required");
            return notification;
        }

        if (cmd.flightId() == null) {
            notification.add("flight_id_required", "flightId", "flightId is required");
        }

        if (cmd.customerId() == null) {
            notification.add("customer_id_required", "customerId", "customerId is required");
        }

        if (cmd.seatTemplateIds() == null || cmd.seatTemplateIds().isEmpty()) {
            notification.add("seat_template_id_required", "seatTemplateId", "seatTemplateId is required");
        }

        if (cmd.bookingId() == null ) {
            notification.add("booking_id_required", "bookingId", "bookingId is required");
        }

        return notification;

    }

}