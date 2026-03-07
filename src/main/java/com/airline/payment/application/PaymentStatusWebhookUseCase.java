package com.airline.payment.application;

import com.airline.payment.api.dto.PaymentGatewayWebhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface PaymentStatusWebhookUseCase  {


    void handlePaymentGatewayStatus(PaymentGatewayWebhook webhook);
}