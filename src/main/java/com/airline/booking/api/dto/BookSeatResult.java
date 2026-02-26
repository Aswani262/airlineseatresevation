package com.airline.booking.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookSeatResult(
        UUID bookingId,
        String bookingReference,
        String status
) {}
