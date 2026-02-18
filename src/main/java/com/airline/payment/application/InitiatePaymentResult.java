package com.airline.payment.application;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record InitiatePaymentResult(
        UUID paymentId,
        UUID bookingId
) {}
