package com.airline.booking.application.command.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmBookingCommand {
    private UUID bookingId;
    private UUID paymentId;
    private String transactionId;
}
