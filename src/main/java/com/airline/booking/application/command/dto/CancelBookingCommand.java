package com.airline.booking.application.command.dto;

import java.util.UUID;


public record CancelBookingCommand(
     UUID bookingId,
     String reason
    ){
}
