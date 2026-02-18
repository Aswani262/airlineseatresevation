package com.airline.flighmngmt.api;

import com.airline.flightmgmt.api.FlightController;
import com.airline.flightmgmt.api.dto.FareClassResponse;
import com.airline.flightmgmt.api.dto.FlightSearchResponse;
import com.airline.flightmgmt.api.dto.SeatAvailabilitySummaryResponse;
import com.airline.flightmgmt.api.dto.SeatResponse;
import com.airline.flightmgmt.application.query.GetFareClassUseCase;
import com.airline.flightmgmt.application.query.GetSeatAvailabilityUseCase;
import com.airline.flightmgmt.application.query.SearchFlightsUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FlightControllerTest {

    @InjectMocks
    private FlightController flightController;

    @Mock
    private SearchFlightsUseCase searchFlightsUseCase;

    @Mock
    private GetSeatAvailabilityUseCase getSeatAvailabilityUseCase;

    @Mock
    private GetFareClassUseCase getFareClassUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(flightController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void search_validParams_returnsListOfFlights() throws Exception {
        // Arrange
        String origin = "NYC";
        String destination = "LAX";
        LocalDate date = LocalDate.of(2024, 1, 1);
        List<FlightSearchResponse> expected = List.of(
                FlightSearchResponse.builder()
                        .flightId(UUID.randomUUID())
                        .flightNumber("FL123")
                        .departureTime(OffsetDateTime.now())
                        .arrivalTime(OffsetDateTime.now().plusHours(5))
                        .status("SCHEDULED")
                        .basePrice(BigDecimal.valueOf(200))
                        .build()
        );

        when(searchFlightsUseCase.search(origin, destination, date)).thenReturn(expected);

        // Act & Assert
        mockMvc.perform(get("/api/v1/flights/search")
                        .param("origin", origin)
                        .param("destination", destination)
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(expected.size()))
                .andExpect(jsonPath("$[0].flightNumber").value(expected.get(0).getFlightNumber()));

        verify(searchFlightsUseCase).search(origin, destination, date);
    }

    @Test
    void seatSummary_validFlightId_returnsSummary() throws Exception {
        // Arrange
        UUID flightId = UUID.randomUUID();
        SeatAvailabilitySummaryResponse expected = SeatAvailabilitySummaryResponse.builder()
                .flightId(flightId)
                .availability(List.of(
                        SeatAvailabilitySummaryResponse.FareClassAvailability.builder()
                                .fareClass("ECONOMY")
                                .available(10)
                                .locked(2)
                                .booked(5)
                                .minPrice(BigDecimal.valueOf(100))
                                .build()
                ))
                .build();

        when(getSeatAvailabilityUseCase.getSummary(flightId)).thenReturn(expected);

        // Act & Assert
        mockMvc.perform(get("/api/v1/flights/{flightId}/seats/summary", flightId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flightId").value(flightId.toString()))
                .andExpect(jsonPath("$.availability[0].fareClass").value("ECONOMY"))
                .andExpect(jsonPath("$.availability[0].available").value(10));

        verify(getSeatAvailabilityUseCase).getSummary(flightId);
    }

    @Test
    void seats_validParams_returnsListOfSeats() throws Exception {
        // Arrange
        UUID flightId = UUID.randomUUID();
        String fareClass = "ECONOMY";
        String status = "AVAILABLE";
        List<SeatResponse> expected = List.of(
                SeatResponse.builder()
                        .seatId(UUID.randomUUID())
                        .seatNumber("A1")
                        .fareClass("ECONOMY")
                        .status("AVAILABLE")
                        .price(BigDecimal.valueOf(100))
                        .build()
        );

        when(getSeatAvailabilityUseCase.getSeats(flightId, fareClass, status)).thenReturn(expected);

        // Act & Assert
        mockMvc.perform(get("/api/v1/flights/{flightId}/seats", flightId)
                        .param("fareClass", fareClass)
                        .param("status", status))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(expected.size()))
                .andExpect(jsonPath("$[0].seatNumber").value("A1"))
                .andExpect(jsonPath("$[0].price").value(100));

        verify(getSeatAvailabilityUseCase).getSeats(flightId, fareClass, status);
    }

    @Test
    void seats_noParams_returnsListOfSeats() throws Exception {
        // Arrange
        UUID flightId = UUID.randomUUID();
        List<SeatResponse> expected = List.of();

        when(getSeatAvailabilityUseCase.getSeats(flightId, null, null)).thenReturn(expected);

        // Act & Assert
        mockMvc.perform(get("/api/v1/flights/{flightId}/seats", flightId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(0));

        verify(getSeatAvailabilityUseCase).getSeats(flightId, null, null);
    }

    @Test
    void getByCode_validCode_returnsFareClass() throws Exception {
        // Arrange
        String code = "ECONOMY";
        FareClassResponse expected = FareClassResponse.builder()
                .code("ECONOMY")
                .name("Economy")
                .description("Standard class")
                .baggageAllowanceKg(23)
                .carryOnAllowed(true)
                .refundable(false)
                .changeable(true)
                .changeFeePercentage(BigDecimal.valueOf(10))
                .priorityBoarding(false)
                .mealService(false)
                .seatSelectionFree(false)
                .build();

        when(getFareClassUseCase.getByCode(code)).thenReturn(expected);

        // Act & Assert
        mockMvc.perform(get("/api/v1/flights/fare-class/{code}", code))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("ECONOMY"))
                .andExpect(jsonPath("$.baggageAllowanceKg").value(23))
                .andExpect(jsonPath("$.refundable").value(false));

        verify(getFareClassUseCase).getByCode(code);
    }
}