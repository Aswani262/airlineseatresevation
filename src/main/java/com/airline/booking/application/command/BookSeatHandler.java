package com.airline.booking.application.command;

import com.airline.booking.api.dto.BookSeatResult;
import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.service.SeatInventoryService;
import com.airline.booking.repository.BookingRepository;
import com.airline.booking.service.BookingCoreService;
import com.airline.shared.annoation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
//Application service orchestrating the booking process, coordinating between domain services and repositories.
// It handles the main flow of booking a seat, including creating a draft, locking seats, and persisting the booking.
@ApplicationService
@RequiredArgsConstructor
public class BookSeatHandler implements BookSeatUseCase {

    //Put in properties file or DB in real app, but hardcoded here for simplicity
    private static final int HOLD_MINUTES = 10;
    private static final Duration HOLD_TTL = Duration.ofMinutes(HOLD_MINUTES);

    private final BookingCoreService bookingCoreService;
    private final SeatInventoryService seatInventoryService;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public BookSeatResult book(BookSeatCommand command) {

        // 1) Draft booking data (validation, totals, seat normalization done here or in seat service)
        var draft = bookingCoreService.createDraft(command, HOLD_MINUTES);

        // 2) Lock seats (core/domain service)
        var lockResult = seatInventoryService.lockSeats(
                command.getFlightId(),
                draft.bookingId(),
                draft.seatNumbers(),
                HOLD_TTL
        );
        seatInventoryService.ensureLockedOrThrow(lockResult);

        //TODO: Rather inserting booking using parameter method, pass domain entity and let repository map save it or map with persistence model
        //TODO: change name from insert booking to save booking,
        // as it can be insert or update based on the existence of bookingId in DB.
        // This will also help in confirm booking use case where we need to update the status from DRAFT to CONFIRMED
        //TODO: Need to change everywhere in code base to use saveBooking instead of insertBooking for better clarity and consistency.

        // 3) Persist booking aggregate
        bookingRepository.insertBooking(
                draft.bookingId(),
                draft.bookingReference(),
                command.getFlightId(),
                command.getCustomerId(),
                draft.totalAmount(),
                command.getCurrency().trim().toUpperCase(),
                draft.status(),
                lockResult.expiresAt() // use lock expiry as hold expiry
        );

        //TODO:We should put the passenger and seat in booking domain model and let the repository handle the persistence of the whole aggregate instead of having separate calls for passengers and seats.
        var passengerIds = bookingRepository.insertPassengers(draft.bookingId(), command.getPassengers());

        bookingRepository.insertBookingSeats(draft.bookingId(), command.getSeatSelections(), passengerIds);

        return new BookSeatResult(
                draft.bookingId(),
                draft.bookingReference(),
                draft.status(),
                lockResult.expiresAt()
        );
    }
}
