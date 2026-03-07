package com.airline.payment.application.dto;

import java.util.UUID;

public record ExpirePaymentCommand (UUID bookingId){
}
