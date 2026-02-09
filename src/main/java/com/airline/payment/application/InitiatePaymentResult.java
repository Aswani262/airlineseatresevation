package com.airline.payment.application;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record InitiatePaymentResult(
        UUID paymentId,
        UUID bookingId,
        String status,                 // PENDING
        String gateway,                // DEMO_GATEWAY
        String redirectUrl,            // user redirect here
        Map<String, String> gatewayParams,
        OffsetDateTime createdAt
) {}
