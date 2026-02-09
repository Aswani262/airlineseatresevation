package com.airline.payment.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiatePaymentCommand {

    @NotNull
    private UUID bookingId;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal amount;

    @NotBlank
    private String currency; // INR/USD

    @NotBlank
    private String returnUrl; // where user comes back after pay

    @NotBlank
    private String webhookUrl; // where gateway will call (demo only)

    private String paymentMethod; // CARD/UPI/NETBANKING (optional)
}
