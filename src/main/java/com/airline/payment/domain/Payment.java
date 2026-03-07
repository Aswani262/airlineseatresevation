package com.airline.payment.domain;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("payments")
public class Payment extends BaseEntity {

    @Id
    private UUID id;

    private UUID bookingId;

    private BigDecimal amount;
    private String currency;

    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private TranascationFor tranascationFor;
    private Reason reason;

    private String transactionId;

    // JSONB response from payment gateway (Razorpay, Stripe, etc.)
    private Map<String, Object> gatewayResponse;

    private String redirectUrl;
    private String returnUrl;

    @Version
    private int version;

}
