package com.airline.payment.api.dto;

import com.airline.payment.application.dto.PaymentGatewayStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentGatewayWebhook {
    private String eventType;
    private UUID paymentId;
    private UUID bookingId;
    private PaymentGatewayStatus status;
    private String transactionId;
    private String reason;
    private String gatewayRawPayload;
}
