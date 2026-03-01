package com.airline.booking.application.command.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelConfirmedBookingResult {

    private UUID bookingId;
    private String bookingReference;
    private String status;   // e.g. CANCELLED, FAILED
}
