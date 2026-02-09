package com.airline.payment.repository;

import com.airline.payment.domain.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    void insert(Payment payment);
    Optional<Payment> findById(UUID paymentId);
    int updateStatus(UUID paymentId, String from, String to, String transactionId);
}
