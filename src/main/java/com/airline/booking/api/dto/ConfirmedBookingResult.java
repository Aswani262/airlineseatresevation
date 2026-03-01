package com.airline.booking.api.dto;

import java.util.UUID;

public record ConfirmedBookingResult(
        UUID bookingId,
        String bookingReference,
        String status
) {}
