package com.airline.payment.application;

import java.util.UUID;

public record InitiatePaymentResult(
        UUID paymentId,
        UUID bookingId
) {}
