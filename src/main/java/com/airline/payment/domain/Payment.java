package com.airline.payment.domain;

import com.airline.shared.model.BaseEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    private UUID id;

    private UUID bookingId;

    private BigDecimal amount;
    private String currency;          // ISO-4217 (USD, INR, EUR)

    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    private String transactionId;// gateway txn id

    // JSONB response from payment gateway (Razorpay, Stripe, etc.)
    private Map<String, Object> gatewayResponse;

    private String redirectUrl;
    private String returnUrl;
    private String status;

}
