package com.airline.payment.api;

import com.airline.payment.api.dto.PaymentGatewayWebhook;
import com.airline.payment.application.PaymentStatusWebhookUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentStatusWebhookUseCase paymentStatusWebhookUseCase;


    @PostMapping("/webhook")
    public void webhook(PaymentGatewayWebhook webhookResponse) {

        paymentStatusWebhookUseCase.handlePaymentGatewayStatus(webhookResponse);

    }
}
