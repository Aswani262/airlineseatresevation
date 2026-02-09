package com.airline.payment.service;

import com.airline.payment.domain.PaymentStatus;
import com.airline.payment.repository.PaymentRepository;
import com.airline.shared.events.PaymentStatusEvent;
import com.airline.shared.service.EventPublisher;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;

@Component
public class DemoGatewaySimulator {

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;
    private final Random random = new Random();

    public DemoGatewaySimulator(PaymentRepository paymentRepository,
                                EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    public void simulateGatewayCallback(UUID paymentId, UUID bookingId) {

        // DEMO: randomly succeed (80%) or fail (20%)
        boolean ok = random.nextInt(10) < 8;

        String txnId = ok ? ("TXN-DEMO-" + UUID.randomUUID()) : null;
        String status = ok ? "SUCCESS" : "FAILED";

        // update DB with CAS (PENDING -> SUCCESS/FAILED)
        int updated = paymentRepository.updateStatus(paymentId, "PENDING", status, txnId);

        // If already processed, do nothing (idempotent)
        if (updated != 1) return;

        PaymentStatusEvent event = ok
                ? new PaymentStatusEvent(bookingId,  PaymentStatusEvent.Status.SUCCESS, null)
                : new PaymentStatusEvent(bookingId, PaymentStatusEvent.Status.FAILED,  "Gateway failure");

        eventPublisher.publish(event);
    }
}
