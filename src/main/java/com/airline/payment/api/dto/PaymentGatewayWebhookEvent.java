package com.airline.payment.api.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentGatewayWebhookEvent {
    private String eventType;
    private UUID paymentId;
    private UUID bookingId;
    private String status;
    private String transactionId;
    private String reason;
    private String gatewayRawPayload;
}
