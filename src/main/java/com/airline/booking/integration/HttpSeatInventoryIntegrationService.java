package com.airline.booking.integration;

import com.airline.booking.exception.BookHoldSeatFailedExceptoion;
import com.airline.booking.exception.SeatExpireExtendFailedExceptoion;
import com.airline.flightmgmt.application.command.BookSeatUseCase;
import com.airline.flightmgmt.application.command.ExtendExpiryTimeUseCase;
import com.airline.flightmgmt.application.command.HoldSeatUseCase;
import com.airline.flightmgmt.application.command.ReleaseBookedSeatUseCase;
import com.airline.flightmgmt.application.command.dto.ConfirmSeatCommand;
import com.airline.flightmgmt.application.command.dto.ExtendExpiryTimeCommand;
import com.airline.shared.annotation.IntegrationService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
// This class is responsible for integrating with the seat inventory service via HTTP calls in case of microservices architecture.
// It implements the SeatInventoryIntegrationService interface and uses an HTTP client (e.g., RestTemplate, WebClient)
// to communicate with the inventory service.The lockSeats method will make an HTTP
// request to the inventory service to lock the specified seats for a given flight and booking ID,
// and return the result of the operation.

//In case of monolithic architecture(multi-modular),
// this class can directly call the LockSeatUseCase
// to perform the seat locking operation without making HTTP calls.

// Note: Do not call the LockSeatUseCase directly from the application service (BookSeatHandler)
// to avoid tight coupling between the booking and inventory modules.
// Or use seat inventory or seat inventory repository directly,
// as that would bypass the domain logic in the inventory module and lead to an anemic design.

//Its works as anti-corruption layer between booking and inventory modules,
// ensuring that the booking module remains decoupled from the internal implementation of the inventory module,
// whether it's accessed via HTTP or directly through application services in a monolithic setup.
@IntegrationService
@RequiredArgsConstructor
public class HttpSeatInventoryIntegrationService implements SeatInventoryIntegrationService {
    // private final String inventoryServiceUrl;
     private final HoldSeatUseCase lockSeatUseCase;
     private final ExtendExpiryTimeUseCase extendExpiryTimeUseCase;
     private final BookSeatUseCase confirmSeatUseCase;
     private final ReleaseBookedSeatUseCase releaseSeatUseCase;



        // Implement methods to call the inventory service using HTTP client (e.g., RestTemplate, WebClient)
//Implement Circuit Breaker pattern for resilience and fallback handling in case of inventory service failure

//    public boolean lockSeats(String flightId, int numberOfSeats, String bookingId, long holdTtlMinutes) {
//        // Implement HTTP call to lock seats in the inventory service
//        // Handle response and return success/failure
//        return true; // Placeholder
//    }

    @Override
    public void extendSeatExpiryTimeForPayment(UUID flightId, List<UUID> seatTemplateIds,UUID customerId , UUID bookingId) {
        extendExpiryTimeUseCase.extendExpiryTime(new ExtendExpiryTimeCommand(flightId,seatTemplateIds,customerId,bookingId));
    }


    @Override
    public void bookHoldSeat(UUID flightId, List<UUID> seatTemplateIds, UUID customerId, UUID bookingId) {
        confirmSeatUseCase.bookHoldSeat(new ConfirmSeatCommand(flightId,seatTemplateIds,customerId,bookingId));
    }
}
