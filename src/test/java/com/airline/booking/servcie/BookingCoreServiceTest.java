package com.airline.booking.servcie;

import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.service.BookingCoreService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class BookingCoreServiceTest {

    private final BookingCoreService service = new BookingCoreService();

    @Test
    void createDraft_shouldCreateDraft_withNormalizedSeatsAndTotalAmount() {
        UUID flightId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        BookSeatCommand cmd = validCommand(flightId, customerId,
                List.of(
                        seatSel(0, " 12a ", "economy", new BigDecimal("100.00")),
                        seatSel(1, "14B", "economy", new BigDecimal("250.50"))
                ),
                List.of(
                        passenger("John", "Doe", "ADULT"),
                        passenger("Jane", "Roe", "CHILD")
                ),
                "INR"
        );

        OffsetDateTime before = OffsetDateTime.now(java.time.Clock.systemUTC());

        var draft = service.createDraft(cmd, 10);

        OffsetDateTime after = OffsetDateTime.now(java.time.Clock.systemUTC());

        assertThat(draft.bookingId()).isNotNull();
        assertThat(draft.status()).isEqualTo("DRAFT");

        // booking reference should be 8 chars and only from ALPHANUM set
        assertThat(draft.bookingReference()).hasSize(8);
        assertThat(draft.bookingReference()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}");

        assertThat(draft.seatNumbers()).containsExactly("12A", "14B");

        assertThat(draft.totalAmount()).isEqualByComparingTo(new BigDecimal("350.50"));

        // holdExpiresAt should be about now + 10 minutes
        OffsetDateTime expectedMin = before.plusMinutes(10).minusSeconds(2);
        OffsetDateTime expectedMax = after.plusMinutes(10).plusSeconds(2);

        assertThat(draft.holdExpiresAt()).isAfterOrEqualTo(expectedMin);
        assertThat(draft.holdExpiresAt()).isBeforeOrEqualTo(expectedMax);
    }

    @Test
    void createDraft_shouldRejectNullCommand() {
        assertThatThrownBy(() -> service.createDraft(null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("command is required");
    }

    @Test
    void createDraft_shouldRejectMissingFlightId() {
        BookSeatCommand cmd = validCommand(null, UUID.randomUUID(),
                List.of(seatSel(0, "12A", "ECONOMY", BigDecimal.ONE)),
                List.of(passenger("A", "B", "ADULT")),
                "INR"
        );

        assertThatThrownBy(() -> service.createDraft(cmd, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("flightId is required");
    }

    @Test
    void createDraft_shouldRejectMissingCustomerId() {
        BookSeatCommand cmd = validCommand(UUID.randomUUID(), null,
                List.of(seatSel(0, "12A", "ECONOMY", BigDecimal.ONE)),
                List.of(passenger("A", "B", "ADULT")),
                "INR"
        );

        assertThatThrownBy(() -> service.createDraft(cmd, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customerId is required");
    }

    @Test
    void createDraft_shouldRejectBlankCurrency() {
        BookSeatCommand cmd = validCommand(UUID.randomUUID(), UUID.randomUUID(),
                List.of(seatSel(0, "12A", "ECONOMY", BigDecimal.ONE)),
                List.of(passenger("A", "B", "ADULT")),
                "   "
        );

        assertThatThrownBy(() -> service.createDraft(cmd, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency is required");
    }

    @Test
    void createDraft_shouldRejectEmptyPassengers() {
        BookSeatCommand cmd = validCommand(UUID.randomUUID(), UUID.randomUUID(),
                List.of(seatSel(0, "12A", "ECONOMY", BigDecimal.ONE)),
                List.of(),
                "INR"
        );

        assertThatThrownBy(() -> service.createDraft(cmd, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("passengers is required");
    }

    @Test
    void createDraft_shouldRejectEmptySeatSelections() {
        BookSeatCommand cmd = validCommand(UUID.randomUUID(), UUID.randomUUID(),
                List.of(),
                List.of(passenger("A", "B", "ADULT")),
                "INR"
        );

        assertThatThrownBy(() -> service.createDraft(cmd, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seatSelections is required");
    }

    @Test
    void createDraft_shouldRejectPassengerIndexOutOfRange_negative() {
        BookSeatCommand cmd = validCommand(UUID.randomUUID(), UUID.randomUUID(),
                List.of(seatSel(-1, "12A", "ECONOMY", BigDecimal.ONE)),
                List.of(passenger("A", "B", "ADULT")),
                "INR"
        );

        assertThatThrownBy(() -> service.createDraft(cmd, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid passengerIndex");
    }

    @Test
    void createDraft_shouldRejectPassengerIndexOutOfRange_tooLarge() {
        BookSeatCommand cmd = validCommand(UUID.randomUUID(), UUID.randomUUID(),
                List.of(seatSel(1, "12A", "ECONOMY", BigDecimal.ONE)), // only 1 passenger => index 1 invalid
                List.of(passenger("A", "B", "ADULT")),
                "INR"
        );

        assertThatThrownBy(() -> service.createDraft(cmd, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid passengerIndex");
    }

    @Test
    void createDraft_shouldRejectBlankSeatNumber() {
        BookSeatCommand cmd = validCommand(UUID.randomUUID(), UUID.randomUUID(),
                List.of(seatSel(0, "   ", "ECONOMY", BigDecimal.ONE)),
                List.of(passenger("A", "B", "ADULT")),
                "INR"
        );

        assertThatThrownBy(() -> service.createDraft(cmd, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seatNumber is required");
    }

    @Test
    void createDraft_shouldRejectNegativeSeatPrice() {
        BookSeatCommand cmd = validCommand(UUID.randomUUID(), UUID.randomUUID(),
                List.of(seatSel(0, "12A", "ECONOMY", new BigDecimal("-1.00"))),
                List.of(passenger("A", "B", "ADULT")),
                "INR"
        );

        assertThatThrownBy(() -> service.createDraft(cmd, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid seat price");
    }

    @Test
    void createDraft_shouldRejectDuplicateSeats_afterNormalization() {
        BookSeatCommand cmd = validCommand(UUID.randomUUID(), UUID.randomUUID(),
                List.of(
                        seatSel(0, "12a", "ECONOMY", BigDecimal.ONE),
                        seatSel(0, " 12A ", "ECONOMY", BigDecimal.ONE)
                ),
                List.of(passenger("A", "B", "ADULT")),
                "INR"
        );

        assertThatThrownBy(() -> service.createDraft(cmd, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate seat in request: 12A");
    }

    // -----------------------
    // Helpers (adjust if your DTO differs)
    // -----------------------

    private static BookSeatCommand validCommand(
            UUID flightId,
            UUID customerId,
            List<BookSeatCommand.SeatSelection> selections,
            List<BookSeatCommand.Passenger> passengers,
            String currency
    ) {
        BookSeatCommand cmd = new BookSeatCommand();
        cmd.setFlightId(flightId);
        cmd.setCustomerId(customerId);
        cmd.setCurrency(currency);
        cmd.setSeatSelections(new ArrayList<>(selections));
        cmd.setPassengers(new ArrayList<>(passengers));
        return cmd;
    }

    private static BookSeatCommand.SeatSelection seatSel(Integer passengerIndex, String seatNumber, String fareClass, BigDecimal price) {
        BookSeatCommand.SeatSelection s = new BookSeatCommand.SeatSelection();
        s.setPassengerIndex(passengerIndex);
        s.setSeatNumber(seatNumber);
        s.setFareClass(fareClass);
        s.setPrice(price);
        return s;
    }

    private static BookSeatCommand.Passenger passenger(String first, String last, String type) {
        BookSeatCommand.Passenger p = new BookSeatCommand.Passenger();
        p.setFirstName(first);
        p.setLastName(last);
        p.setPassengerType(type);
        return p;
    }
}
