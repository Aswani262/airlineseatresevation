package com.airline.flightmgmt.application.query;

import com.airline.flightmgmt.api.dto.FareClassResponse;
import com.airline.flightmgmt.api.dto.FlightSearchResponse;
import com.airline.flightmgmt.api.dto.SeatAvailabilitySummaryResponse;
import com.airline.flightmgmt.api.dto.SeatResponse;
import com.airline.flightmgmt.repository.FareClassRepository;
import com.airline.flightmgmt.repository.FlightQueryRepository;
import com.airline.flightmgmt.repository.SeatInventoryQueryRepository;
import com.airline.shared.annoation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@ApplicationService
@RequiredArgsConstructor
public class FlightQueryHandler implements
        SearchFlightsUseCase,
        GetSeatAvailabilityUseCase,
        GetFareClassUseCase {

    private final FlightQueryRepository flightQueryRepository;
    private final SeatInventoryQueryRepository seatInventoryQueryRepository;
    private final FareClassRepository fareClassRepository;


    @Override
    public java.util.List<FlightSearchResponse> search(String origin, String destination, LocalDate date) {
        return flightQueryRepository.search(
                origin.trim().toUpperCase(Locale.ROOT),
                destination.trim().toUpperCase(Locale.ROOT),
                date
        );
    }

    @Override
    public SeatAvailabilitySummaryResponse getSummary(UUID flightId) {
        return seatInventoryQueryRepository.getSummary(flightId);
    }

    @Override
    public List<SeatResponse> getSeats(UUID flightId, String fareClass, String status) {
        return seatInventoryQueryRepository.getSeats(flightId,
                fareClass == null ? null : fareClass.trim().toUpperCase(Locale.ROOT),
                status == null ? null : status.trim().toUpperCase(Locale.ROOT)
        );
    }

    @Override
    public FareClassResponse getByCode(String code) {
        return fareClassRepository.getByCode(code.trim().toUpperCase(Locale.ROOT));
    }
}
