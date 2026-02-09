package com.airline.booking.servcie;

import com.airline.booking.repository.SeatInventoryRepository;
import com.airline.booking.service.SeatInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SeatInventoryServiceTest {

    private SeatInventoryRepository repo;
    private SeatInventoryService service;

    @BeforeEach
    void setup() {
        repo = mock(SeatInventoryRepository.class);
        service = new SeatInventoryService(repo);
    }

    @Test
    void lockSeats_shouldNormalizeSeats_callRepo_andReturnSuccessWhenAllLocked() {
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Duration ttl = Duration.ofMinutes(10);

        List<String> inputSeats = List.of(" 12a ", "14B");
        List<String> expectedNormalized = List.of("12A", "14B");

        when(repo.lockSeats(eq(flightId), eq(expectedNormalized), eq(bookingId), eq(ttl)))
                .thenReturn(2);

        OffsetDateTime before = OffsetDateTime.now(Clock.systemUTC());
        var result = service.lockSeats(flightId, bookingId, inputSeats, ttl);
        OffsetDateTime after = OffsetDateTime.now(Clock.systemUTC());

        assertThat(result.success()).isTrue();
        assertThat(result.seatNumbers()).containsExactly("12A", "14B");

        // expiresAt should be around now + ttl
        OffsetDateTime min = before.plusSeconds(ttl.getSeconds()).minusSeconds(2);
        OffsetDateTime max = after.plusSeconds(ttl.getSeconds()).plusSeconds(2);
        assertThat(result.expiresAt()).isAfterOrEqualTo(min);
        assertThat(result.expiresAt()).isBeforeOrEqualTo(max);

        verify(repo).lockSeats(flightId, expectedNormalized, bookingId, ttl);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void lockSeats_shouldReturnFailureWhenNotAllSeatsLocked() {
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Duration ttl = Duration.ofSeconds(30);

        // 2 seats requested, only 1 updated
        when(repo.lockSeats(eq(flightId), eq(List.of("12A", "14B")), eq(bookingId), eq(ttl)))
                .thenReturn(1);

        var result = service.lockSeats(flightId, bookingId, List.of("12A", "14B"), ttl);

        assertThat(result.success()).isFalse();
        assertThat(result.seatNumbers()).containsExactly("12A", "14B");
    }

    @Test
    void lockSeats_shouldThrowIllegalArgumentException_whenSeatNumbersNullOrEmpty() {
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        assertThatThrownBy(() -> service.lockSeats(flightId, bookingId, null, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seatNumbers is required");

        assertThatThrownBy(() -> service.lockSeats(flightId, bookingId, List.of(), Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seatNumbers is required");

        verifyNoInteractions(repo);
    }

    @Test
    void ensureLockedOrThrow_shouldThrowWhenNotSuccess() {
        var result = new SeatInventoryService.SeatLockResult(
                false,
                List.of("12A"),
                OffsetDateTime.now(Clock.systemUTC()).plusMinutes(1)
        );

        assertThatThrownBy(() -> service.ensureLockedOrThrow(result))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void ensureLockedOrThrow_shouldNotThrowWhenSuccess() {
        var result = new SeatInventoryService.SeatLockResult(
                true,
                List.of("12A"),
                OffsetDateTime.now(Clock.systemUTC()).plusMinutes(1)
        );

        assertThatCode(() -> service.ensureLockedOrThrow(result))
                .doesNotThrowAnyException();
    }

    @Test
    void confirmLockedSeatsOrThrow_shouldNormalizeAndCallRepo_andNotThrowWhenAllConfirmed() {
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(repo.confirmSeats(eq(flightId), eq(List.of("12A", "14B")), eq(bookingId)))
                .thenReturn(2);

        assertThatCode(() -> service.confirmLockedSeatsOrThrow(
                flightId, bookingId, List.of(" 12a ", "14B")
        )).doesNotThrowAnyException();

        verify(repo).confirmSeats(flightId, List.of("12A", "14B"), bookingId);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void confirmLockedSeatsOrThrow_shouldThrowWhenNotAllConfirmed() {
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(repo.confirmSeats(eq(flightId), eq(List.of("12A", "14B")), eq(bookingId)))
                .thenReturn(1);

        assertThatThrownBy(() -> service.confirmLockedSeatsOrThrow(
                flightId, bookingId, List.of("12A", "14B")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lock expired");

        verify(repo).confirmSeats(flightId, List.of("12A", "14B"), bookingId);
    }

    @Test
    void releaseLockedSeatsOrThrow_shouldNormalizeAndCallRepo_andNotThrowWhenAllReleased() {
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(repo.releaseLockedSeats(eq(flightId), eq(List.of("12A", "14B")), eq(bookingId)))
                .thenReturn(2);

        assertThatCode(() -> service.releaseLockedSeatsOrThrow(
                flightId, bookingId, List.of(" 12a ", "14b ")
        )).doesNotThrowAnyException();

        verify(repo).releaseLockedSeats(flightId, List.of("12A", "14B"), bookingId);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void releaseLockedSeatsOrThrow_shouldThrowWhenNotAllReleased() {
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(repo.releaseLockedSeats(eq(flightId), eq(List.of("12A", "14B")), eq(bookingId)))
                .thenReturn(1);

        assertThatThrownBy(() -> service.releaseLockedSeatsOrThrow(
                flightId, bookingId, List.of("12A", "14B")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not locked by this booking");

        verify(repo).releaseLockedSeats(flightId, List.of("12A", "14B"), bookingId);
    }

    @Test
    void releaseBookedSeats_shouldNormalizeAndCallRepo_andNotThrow() {
        UUID flightId = UUID.randomUUID();

        when(repo.releaseBookedSeats(eq(flightId), eq(List.of("12A", "14B"))))
                .thenReturn(1); // can be any number; method is idempotent

        assertThatCode(() -> service.releaseBookedSeats(flightId, List.of(" 12a ", "14b ")))
                .doesNotThrowAnyException();

        verify(repo).releaseBookedSeats(flightId, List.of("12A", "14B"));
        verifyNoMoreInteractions(repo);
    }

    @Test
    void confirmLockedSeatsOrThrow_shouldPassNormalizedList_evenIfWeDontHardcodeIt() {
        // This test shows how to assert normalization using ArgumentCaptor
        UUID flightId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(repo.confirmSeats(eq(flightId), anyList(), eq(bookingId)))
                .thenReturn(1);

        ArgumentCaptor<List<String>> seatsCaptor = ArgumentCaptor.forClass(List.class);

        assertThatThrownBy(() -> service.confirmLockedSeatsOrThrow(
                flightId, bookingId, List.of(" 12a ", " 14b ")
        )).isInstanceOf(IllegalStateException.class);

        verify(repo).confirmSeats(eq(flightId), seatsCaptor.capture(), eq(bookingId));
        assertThat(seatsCaptor.getValue()).containsExactly("12A", "14B");
    }
}
