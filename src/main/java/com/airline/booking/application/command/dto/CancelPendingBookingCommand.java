package com.airline.booking.application.command.dto;

import java.util.UUID;

public record CancelPendingBookingCommand (UUID bookingId, String reason){
}
