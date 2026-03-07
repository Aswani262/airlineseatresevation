package com.airline.payment.repository;

import com.airline.payment.domain.Payment;
import com.airline.payment.domain.PaymentStatus;
import com.airline.payment.domain.TranascationFor;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IPaymentCommandRepository extends CrudRepository<Payment, UUID> {
    Optional<Payment> findByBookingId(UUID paymentId);

    Payment findByBookingIdAndStatusAndTranascationFor(UUID uuid, PaymentStatus paymentStatus, TranascationFor tranascationFor);

    @Query("SELECT p FROM Payment p WHERE p.bookingId = :bookingId AND p.status IN :statuses")
    Optional<Payment> findActivePayment(@Param("bookingId") UUID bookingId,
                                        @Param("statuses") List<PaymentStatus> statuses);

    Optional<Payment> findByBookingIdAndStatus(UUID bookingId, PaymentStatus paymentStatus);
}
