package com.airline.booking.integration;

import com.airline.flightmgmt.application.command.LockSeatCommand;
import com.airline.flightmgmt.application.command.LockSeatUseCase;
import com.airline.flightmgmt.exception.SeatLockingFailedException;
import com.airline.shared.annotation.IntegrationService;
import com.airline.shared.model.SeatLockResult;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
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
     private final LockSeatUseCase lockSeatUseCase;



        // Implement methods to call the inventory service using HTTP client (e.g., RestTemplate, WebClient)
//Implement Circuit Breaker pattern for resilience and fallback handling in case of inventory service failure

//    public boolean lockSeats(String flightId, int numberOfSeats, String bookingId, long holdTtlMinutes) {
//        // Implement HTTP call to lock seats in the inventory service
//        // Handle response and return success/failure
//        return true; // Placeholder
//    }


    //Same can be implemented with webclient or rest client
    // But for integration purpose , consider using Apace camel which provides a higher level abstraction for integration
    // and supports various protocols and
    // patterns out of the box,
    // including HTTP, retries, circuit breakers, etc.
    @Override
    @Retryable(retryFor = {SeatLockingFailedException.class, TimeoutException.class}, maxAttempts = 2, backoff = @Backoff(delay = 1000)
    )
    public SeatLockResult lockSeats(UUID flightId, List<String> seats, UUID bookingId, int holdMinutes) {
         LockSeatCommand command = new LockSeatCommand(flightId, seats, bookingId, holdMinutes);
         return lockSeatUseCase.lockSeat(command);
    }
}
