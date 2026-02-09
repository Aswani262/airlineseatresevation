package com.airline.booking.application.command.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelBookingCommand {
    private UUID bookingId;
    private String reason; // optional
}
