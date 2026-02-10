package com.airline.payment.application.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiatePaymentCommand {

    private UUID bookingId;

    private BigDecimal amount;

    private String currency; // INR/USD

    private String returnUrl; // where user comes back after pay

    private String webhookUrl; // where gateway will call (demo only)

    private String paymentMethod; // CARD/UPI/NETBANKING (optional)
}
