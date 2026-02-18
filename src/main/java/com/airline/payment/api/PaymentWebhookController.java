package com.airline.payment.api;

import com.airline.payment.api.dto.PaymentGatewayWebhookEvent;
import com.airline.payment.service.DemoGatewaySimulator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final DemoGatewaySimulator simulator;

    @PostMapping("/webhook")
    public void webhook(PaymentGatewayWebhookEvent event) {
        simulator.simulateGatewayCallback(event.getPaymentId(), event.getBookingId());
    }
}
