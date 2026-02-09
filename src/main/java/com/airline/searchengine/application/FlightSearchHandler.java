package com.airline.searchengine.application;

import com.airline.searchengine.domain.FlightSearchDoc;
import com.airline.searchengine.integration.FlightServiceIntegrationServcie;
import com.airline.searchengine.repository.FlightSearchRepository;
import com.airline.shared.annoation.ApplicationService;
import lombok.RequiredArgsConstructor;

@ApplicationService
@RequiredArgsConstructor
public class FlightSearchHandler implements UpdateFlightSearchDoc {

    private final FlightSearchRepository flightSearchRepository;

    private final FlightServiceIntegrationServcie flightServiceIntegrationServcie;


    @Override
    public void handle(UpdateFlightSearchDocCommand command) {
        var flight = flightServiceIntegrationServcie.getFlightById(command.flightId());
        var route = flightServiceIntegrationServcie.getRouteById(flight.getRouteId());
        var aircraft = flightServiceIntegrationServcie.getAircraftById(flight.getAircraftId());

        FlightSearchDoc doc = FlightSearchDoc.builder()
                .id(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .aircraftId(flight.getAircraftId())
                .routeId(flight.getRouteId())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .status(flight.getStatus().name())
                .basePrice(flight.getBasePrice())

                .originAirport(route.getOriginAirport())
                .destinationAirport(route.getDestinationAirport())
                .distanceKm(route.getDistanceKm())
                .estimatedDurationMinutes(route.getEstimatedDurationMinutes())
                .isInternational(route.getIsInternational())

                .registrationNumber(aircraft.getRegistrationNumber())
                .model(aircraft.getModel())
                .manufacturer(aircraft.getManufacturer())
                .totalSeats(aircraft.getTotalSeats())
                .configuration(aircraft.getConfiguration())
                .build();

        flightSearchRepository.save(doc);
    }
}
