package com.airline.payment.service;

import com.airline.payment.domain.Payment;
import com.airline.payment.domain.PaymentStatus;
import com.airline.payment.repository.IPaymentCommandRepository;
import com.airline.shared.events.PaymentStatusEvent;
import com.airline.shared.service.EventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;

@Component
public class DemoGatewaySimulator {

    private final IPaymentCommandRepository
            paymentRepository;
    private final EventPublisher eventPublisher;
    private final Random random = new Random();

    public DemoGatewaySimulator(IPaymentCommandRepository paymentRepository,
                                EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void simulateGatewayCallback(UUID paymentId, UUID bookingId) {
        // DEMO: randomly succeed (80%) or fail (20%)
        boolean ok = random.nextInt(10) < 8;

        String txnId = ok ? ("TXN-DEMO-" + UUID.randomUUID()) : null;

        //Payment gateway send success or failed in any case , in case of timeout
        //if payment gateway send any event , then still that will failed with reason timeout
        String newStatus = ok ? "SUCCESS" : "FAILED";
        String failureReason = ok ? null : "Gateway failure";

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        // Idempotency: If already in a final state (SUCCESS or FAILED), skip update and event publishing
        if ("SUCCESS".equals(payment.getStatus().name()) || "FAILED".equals(payment.getStatus().name())) {

            return;
        }

        // Update payment details
        payment.setStatus(PaymentStatus.valueOf(newStatus));
        if (ok) {
            payment.setTransactionId(txnId);
        }

        try {
            paymentRepository.save(payment);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Concurrent modification detected on payment " + paymentId + "; callback may need retry", e);
        }

        // Publish event only if update succeeded
        PaymentStatusEvent event = ok
                ? new PaymentStatusEvent(bookingId, PaymentStatusEvent.Status.SUCCESS, null)
                : new PaymentStatusEvent(bookingId, PaymentStatusEvent.Status.FAILED, failureReason);

        eventPublisher.publish(event);
    }
}
