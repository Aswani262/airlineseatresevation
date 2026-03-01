package com.airline.flightmgmt.application.query;

import com.airline.flightmgmt.api.dto.FareClassResponse;
import com.airline.flightmgmt.api.dto.FlightSearchResponse;
import com.airline.flightmgmt.api.dto.SeatAssignmentsResponse;
import com.airline.flightmgmt.api.dto.SeatAvalibityResponse;
import com.airline.flightmgmt.domain.*;
import com.airline.flightmgmt.repository.*;
import com.airline.shared.annotation.ApplicationService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationService
@RequiredArgsConstructor
public class FlightQueryHandler implements
        SearchFlightsUseCase,
        GetSeatAvailabilityUseCase
         {

    private final FlightQueryRepository flightQueryRepository;
    private final SeatAssigmentQueryRepository seatInventoryQueryRepository;
    private final ISeatTemplateCacheRepository seatTemplateCacheRepository;
    private final IFlightCacheRepository flightCacheRepository;
    private static Map<FareClass, BigDecimal> priceByFareClass = new HashMap<>();
             static {
                 priceByFareClass.put(FareClass.ECONOMY,new BigDecimal(5000.00));
                 priceByFareClass.put(FareClass.BUSINESS,new BigDecimal(7000.00));
             }



    @Override
    public java.util.List<FlightSearchResponse> search(String origin, String destination, LocalDate date) {
        return flightQueryRepository.search(
                origin.trim().toUpperCase(Locale.ROOT),
                destination.trim().toUpperCase(Locale.ROOT),
                date
        );
    }

    @Override
    public List<SeatAvalibityResponse> getSeatsAvailability(UUID flightId, String fareClass, String status) {

        //Cached Data
        FlightCache flight = flightCacheRepository.getFlight(flightId);

        LocalDate flightDate = flight.getFlightDate();

        //Cached Data
        AircraftSeatTemplateCache aircraftSeatTemplateCache = seatTemplateCacheRepository.getSeatTemplate(flight.getAircraftId());

        List<SeatTemplateCache> seatTemplates = aircraftSeatTemplateCache.getSeatsCache();

        List<SeatAssignmentsResponse> assignedSeats = seatInventoryQueryRepository.getAssignedSeats(flightId, flightDate);

        // Map of seatTemplateId to status name for quick lookup only HOLD/BOOKED
        Map<UUID, String> assignedStatuses = assignedSeats.stream()
                .collect(Collectors.toMap(SeatAssignmentsResponse::getSeatTemplateId, sa -> sa.getStatus().name()));

        List<SeatAvalibityResponse> responses = new ArrayList<>();

        for (SeatTemplateCache template : seatTemplates) {

            String seatFareClass = template.getFareClass().name();
            // Filter by fareClass if provided
            if (fareClass != null && !seatFareClass.equalsIgnoreCase(fareClass)) {
                continue;
            }

            String seatStatus;

            if (template.isBlocked()) {
                seatStatus = "BLOCKED";
            } else {
                seatStatus = assignedStatuses.getOrDefault(template.getSeatTemplateId(), "AVAILABLE");
            }

            // Filter by status if provided
            if (status != null && !seatStatus.equalsIgnoreCase(status)) {
                continue;
            }
           BigDecimal price =  priceByFareClass.get(FareClass.valueOf(seatFareClass));

            SeatAvalibityResponse response = SeatAvalibityResponse.builder()
                    .seatTemplateId(template.getSeatTemplateId())
                    .seatNumber(template.getSeatNumber())
                    .fareClass(seatFareClass)
                    .status(seatStatus)
                    .price(price)
                    .seatType(template.getSeatType())
                    .build();

            responses.add(response);
        }

        return responses;
    }

}
