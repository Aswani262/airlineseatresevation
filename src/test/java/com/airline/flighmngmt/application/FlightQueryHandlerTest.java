package com.airline.flighmngmt.application;

import com.airline.flightmgmt.api.dto.FareClassResponse;
import com.airline.flightmgmt.api.dto.FlightSearchResponse;
import com.airline.flightmgmt.api.dto.SeatAvailabilitySummaryResponse;
import com.airline.flightmgmt.api.dto.SeatResponse;
import com.airline.flightmgmt.application.query.FlightQueryHandler;
import com.airline.flightmgmt.repository.FareClassRepository;
import com.airline.flightmgmt.repository.FlightQueryRepository;
import com.airline.flightmgmt.repository.SeatInventoryQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlightQueryHandlerTest {

    private FlightQueryRepository flightQueryRepository;
    private SeatInventoryQueryRepository seatInventoryQueryRepository;
    private FareClassRepository fareClassRepository;

    private FlightQueryHandler handler;

    @BeforeEach
    void setup() {
        flightQueryRepository = mock(FlightQueryRepository.class);
        seatInventoryQueryRepository = mock(SeatInventoryQueryRepository.class);
        fareClassRepository = mock(FareClassRepository.class);

        handler = new FlightQueryHandler(
                flightQueryRepository,
                seatInventoryQueryRepository,
                fareClassRepository
        );
    }

    @Test
    void search_shouldTrimAndUppercaseOriginDestination_andDelegate() {
        LocalDate date = LocalDate.of(2026, 2, 8);

        var r1 = FlightSearchResponse.builder().flightId(UUID.randomUUID()).flightNumber("AI-101").build();
        when(flightQueryRepository.search("BOM", "DEL", date)).thenReturn(List.of(r1));

        List<FlightSearchResponse> result = handler.search("  bom ", " del  ", date);

        assertThat(result).containsExactly(r1);
        verify(flightQueryRepository).search("BOM", "DEL", date);
        verifyNoMoreInteractions(flightQueryRepository);
        verifyNoInteractions(seatInventoryQueryRepository, fareClassRepository);
    }

    @Test
    void getSummary_shouldDelegateAsIs() {
        UUID flightId = UUID.randomUUID();

        var summary = SeatAvailabilitySummaryResponse.builder()
                .flightId(flightId)
                .availability(List.of())
                .build();

        when(seatInventoryQueryRepository.getSummary(flightId)).thenReturn(summary);

        SeatAvailabilitySummaryResponse result = handler.getSummary(flightId);

        assertThat(result).isSameAs(summary);
        verify(seatInventoryQueryRepository).getSummary(flightId);
        verifyNoMoreInteractions(seatInventoryQueryRepository);
        verifyNoInteractions(flightQueryRepository, fareClassRepository);
    }

    @Test
    void getSeats_shouldUppercaseAndTrimFilters_whenProvided_andDelegate() {
        UUID flightId = UUID.randomUUID();

        var s1 = SeatResponse.builder().seatId(UUID.randomUUID()).seatNumber("12A").fareClass("ECONOMY").status("AVAILABLE").build();
        when(seatInventoryQueryRepository.getSeats(flightId, "ECONOMY", "AVAILABLE"))
                .thenReturn(List.of(s1));

        List<SeatResponse> result = handler.getSeats(flightId, " economy  ", "  available ");

        assertThat(result).containsExactly(s1);
        verify(seatInventoryQueryRepository).getSeats(flightId, "ECONOMY", "AVAILABLE");
        verifyNoMoreInteractions(seatInventoryQueryRepository);
        verifyNoInteractions(flightQueryRepository, fareClassRepository);
    }

    @Test
    void getSeats_shouldPassNulls_whenFareClassAndStatusAreNull() {
        UUID flightId = UUID.randomUUID();

        when(seatInventoryQueryRepository.getSeats(flightId, null, null))
                .thenReturn(List.of());

        List<SeatResponse> result = handler.getSeats(flightId, null, null);

        assertThat(result).isEmpty();
        verify(seatInventoryQueryRepository).getSeats(flightId, null, null);
        verifyNoMoreInteractions(seatInventoryQueryRepository);
        verifyNoInteractions(flightQueryRepository, fareClassRepository);
    }

    @Test
    void getSeats_shouldNormalizeOnlyNonNullParam() {
        UUID flightId = UUID.randomUUID();

        when(seatInventoryQueryRepository.getSeats(flightId, "BUSINESS", null))
                .thenReturn(List.of());

        handler.getSeats(flightId, " business ", null);

        verify(seatInventoryQueryRepository).getSeats(flightId, "BUSINESS", null);
        verifyNoMoreInteractions(seatInventoryQueryRepository);
        verifyNoInteractions(flightQueryRepository, fareClassRepository);
    }

    @Test
    void getByCode_shouldTrimAndUppercase_andDelegate() {
        var res = FareClassResponse.builder()
                .code("ECONOMY")
                .name("Economy")
                .build();

        when(fareClassRepository.getByCode("ECONOMY")).thenReturn(res);

        FareClassResponse out = handler.getByCode("  economy ");

        assertThat(out).isSameAs(res);
        verify(fareClassRepository).getByCode("ECONOMY");
        verifyNoMoreInteractions(fareClassRepository);
        verifyNoInteractions(flightQueryRepository, seatInventoryQueryRepository);
    }
}
