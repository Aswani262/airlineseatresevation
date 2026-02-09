package com.airline.flightmgmt.api;

import com.airline.flightmgmt.api.dto.FareClassResponse;
import com.airline.flightmgmt.api.dto.FlightSearchResponse;
import com.airline.flightmgmt.api.dto.SeatAvailabilitySummaryResponse;
import com.airline.flightmgmt.api.dto.SeatResponse;
import com.airline.flightmgmt.application.query.GetFareClassUseCase;
import com.airline.flightmgmt.application.query.GetSeatAvailabilityUseCase;
import com.airline.flightmgmt.application.query.SearchFlightsUseCase;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/flights")
public class FlightQueryController {

    private final SearchFlightsUseCase searchFlightsUseCase;
    private final GetSeatAvailabilityUseCase getSeatAvailabilityUseCase;
    private final GetFareClassUseCase getFareClassUseCase;

    public FlightQueryController(SearchFlightsUseCase searchFlightsUseCase,
                                 GetSeatAvailabilityUseCase getSeatAvailabilityUseCase, GetFareClassUseCase getFareClassUseCase) {
        this.searchFlightsUseCase = searchFlightsUseCase;
        this.getSeatAvailabilityUseCase = getSeatAvailabilityUseCase;
        this.getFareClassUseCase = getFareClassUseCase;
    }

    @GetMapping("/search")
    public List<FlightSearchResponse> search(
            @RequestParam @NotBlank String origin,
            @RequestParam @NotBlank String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return searchFlightsUseCase.search(origin, destination, date);
    }

    //This endpoint is out of scope for the current implementation, but we can use it in the future to show seat availability summary in the flight search result
    @GetMapping("/{flightId}/seats/summary")
    public SeatAvailabilitySummaryResponse seatSummary(@PathVariable UUID flightId) {
        return getSeatAvailabilityUseCase.getSummary(flightId);
    }

    @GetMapping("/{flightId}/seats")
    public List<SeatResponse> seats(
            @PathVariable UUID flightId,
            @RequestParam(required = false) String fareClass,   // ECONOMY/BUSINESS
            @RequestParam(required = false) String status       // AVAILABLE/LOCKED/BOOKED
    ) {
        return getSeatAvailabilityUseCase.getSeats(flightId, fareClass, status);
    }

    @GetMapping("/fare-class/{code}")
    public FareClassResponse getByCode(@PathVariable String code) {
        return getFareClassUseCase.getByCode(code);
    }
}
