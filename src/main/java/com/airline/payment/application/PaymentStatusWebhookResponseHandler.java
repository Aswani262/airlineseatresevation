package com.airline.payment.application;

import com.airline.payment.api.dto.PaymentGatewayWebhook;
import com.airline.payment.application.dto.InitiatePaymentCommand;
import com.airline.payment.domain.Payment;
import com.airline.payment.domain.PaymentStatus;
import com.airline.payment.domain.Reason;
import com.airline.payment.domain.TranascationFor;
import com.airline.payment.repository.IPaymentCommandRepository;
import com.airline.payment.service.PaymentCoreService;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.events.PaymentStatusEvent;
import com.airline.shared.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@RequiredArgsConstructor
@ApplicationService
@Slf4j
public class PaymentStatusWebhookResponseHandler implements PaymentStatusWebhookUseCase {

    private final IPaymentCommandRepository paymentRepository;
    private final PaymentCoreService paymentCoreService;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public void handlePaymentGatewayStatus(PaymentGatewayWebhook webhook) {

        Payment originalPayment = paymentRepository.findById(webhook.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + webhook.getPaymentId()));

        if (originalPayment.getStatus() != PaymentStatus.PENDING) {
            log.info("Ignoring duplicate/late webhook for already final payment: {}", webhook.getPaymentId());
            return;
        }

        PaymentStatus newStatus = mapGatewayStatus(webhook.getStatus());

        // LATE SUCCESS AFTER TIMEOUT  CREATE REFUND PAYMENT
        if (newStatus == PaymentStatus.SUCCESS &&
                originalPayment.getReason() == Reason.PAYMENT_TIME_OUT) {

            log.info("Late SUCCESS after timeout. Creating REFUND payment for booking: {}",
                    originalPayment.getBookingId());

            Payment refundPayment = paymentCoreService.createRefundPayment(
                    originalPayment,
                    webhook.getTransactionId(),
                    webhook.getGatewayRawPayload()
            );

            paymentRepository.save(refundPayment);

            return;
        }

        originalPayment.setStatus(newStatus);
        originalPayment.setTransactionId(webhook.getTransactionId());
        paymentRepository.save(originalPayment);

        eventPublisher.publish(new PaymentStatusEvent(
                originalPayment.getBookingId(),
                mapToEventStatus(newStatus),
                webhook.getReason()
        ));
    }

    private PaymentStatus mapGatewayStatus(com.airline.payment.application.dto.PaymentGatewayStatus status) {
        return switch (status) {
            case SUCCESS -> PaymentStatus.SUCCESS;
            case FAILED, EXPIRED -> PaymentStatus.FAILED;
        };
    }

    private PaymentStatusEvent.Status mapToEventStatus(PaymentStatus status) {
        return status == PaymentStatus.SUCCESS
                ? PaymentStatusEvent.Status.SUCCESS
                : PaymentStatusEvent.Status.FAILED;
    }


}
