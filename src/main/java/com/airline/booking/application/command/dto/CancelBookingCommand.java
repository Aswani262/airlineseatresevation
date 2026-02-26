package com.airline.booking.application.command.dto;

import lombok.*;

import java.util.UUID;


public record CancelBookingCommand(
     UUID bookingId,
     String reason,
     UUID customerId){
}
