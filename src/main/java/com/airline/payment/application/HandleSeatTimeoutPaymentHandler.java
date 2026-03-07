package com.airline.payment.application;

import com.airline.payment.application.dto.ExpirePaymentCommand;
import com.airline.payment.domain.Payment;
import com.airline.payment.domain.PaymentStatus;
import com.airline.payment.domain.Reason;
import com.airline.payment.domain.TranascationFor;
import com.airline.payment.repository.IPaymentCommandRepository;
import com.airline.shared.annotation.ApplicationService;
import com.airline.shared.events.PaymentStatusEvent;
import com.airline.shared.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationService
@RequiredArgsConstructor
public class HandleSeatTimeoutPaymentHandler implements HandleSeatTimeoutUseCase {

    private final IPaymentCommandRepository paymentRepository;
    private final EventPublisher eventPublisher;

    @Override
    public void expirePayment(ExpirePaymentCommand command) {

        // Find the pending payment for this booking (only BOOKING type)
        Payment pendingPayment = paymentRepository.findByBookingIdAndStatusAndTranascationFor(
                command.bookingId(),
                PaymentStatus.PENDING,
                TranascationFor.BOOKING
        );

        if (pendingPayment == null) {
            log.debug("No pending payment found for bookingId: {} already processed or not created",
                    command.bookingId());
            return;
        }

        log.info("Seat hold expired. Marking pending payment as FAILED due to timeout. PaymentId: {}, BookingId: {}", 
                pendingPayment.getId(), command.bookingId());

        // Mark payment as failed with proper reason
        pendingPayment.setStatus(PaymentStatus.FAILED);
        pendingPayment.setReason(Reason.PAYMENT_TIME_OUT);

        // Save the updated payment
        paymentRepository.save(pendingPayment);

        log.info("Payment marked as FAILED due to timeout and event published for bookingId: {}", 
                command.bookingId());
    }
}