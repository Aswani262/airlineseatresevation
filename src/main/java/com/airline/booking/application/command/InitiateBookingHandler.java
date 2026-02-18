package com.airline.booking.application.command;

import com.airline.booking.api.dto.BookSeatResult;
import com.airline.booking.application.command.dto.InitiateBookingSeatCommand;
import com.airline.booking.domain.model.Booking;
import com.airline.booking.domain.model.BookingSeat;
import com.airline.booking.integration.SeatInventoryIntegrationService;
import com.airline.booking.repository.IBookingCommandRepository;
import com.airline.booking.service.core.IBookingService;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.model.SeatLockResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

//Application service orchestrating the booking process, coordinating between domain services and repositories.
// It handles the main flow of booking a seat, including creating a draft, locking seats, and persisting the booking.
@ApplicationService
@RequiredArgsConstructor
public class InitiateBookingHandler implements BookSeatUseCase {

    //Put in properties file or DB in real app, but hardcoded here for simplicity
    private static final int HOLD_MINUTES = 10;


    private final IBookingService bookingCoreService;
    private final IBookingCommandRepository bookingRepository;
    private final SeatInventoryIntegrationService seatInventoryService;

    @Override
    @Transactional
    public BookSeatResult initiateBooking(InitiateBookingSeatCommand command) {

        // 1. Draft booking data (validation, totals, seat normalization done here)
        // This creates a booking aggregate . It will be in DRAFT status.
        Booking booking = bookingCoreService.createDraft(command);

        // We can replace this with an event (in case of event driven arch )and
        // have a separate process that listens to booking created events and locks seats asynchronously

        // (doing it asynchronously would require handling the case where seat
        // locking fails after booking is created, and we would need to update
        // the booking status and notify the user accordingly)- as its over engineering for this

        // For Business logic which requires strong consistency between booking and seat inventory,
        // doing it synchronously in the same transaction is simpler and ensures data integrity.

        // 2. Lock seats in inventory - this will set the hold expiration time on the seat inventory records
        SeatLockResult lockResult =  seatInventoryService.lockSeats(
                    command.getFlightId(),
                    booking.getSeats().stream().map(BookingSeat::getSeatNumber).toList(),
                    booking.getId(),
                    HOLD_MINUTES
        );


        // Set the hold expiration from the lock result
        booking.setHoldExpiresAt(lockResult.expiresAt());

        try {
            // 3. Persist booking aggregate - this will save the booking in DRAFT status with the hold expiration time
            // Booking aggregate will hold passenger info, seat details, pricing, and the hold expiration time.
            bookingRepository.save(booking);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Booking creation failed due to concurrent modification; please retry", e);
        }

        //Publish BookingInitiatedEvent here if you want to have an event driven architecture and handle seat locking asynchronously
        //Use BookingInitiatedEvent to register the expiration time in a scheduler
        // or to trigger a process that will check for expired holds and release seats accordingly

        return new BookSeatResult(
                booking.getId(),
                booking.getBookingReference(),
                booking.getStatus().toString(),
                lockResult.expiresAt()
        );
    }
}