package com.airline.payment.application;

import com.airline.payment.application.dto.InitiatePaymentCommand;
import com.airline.payment.domain.Payment;
import com.airline.payment.repository.IPaymentCommandRepository;
import com.airline.payment.service.PaymentCoreService;
import com.airline.shared.annotation.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
@RequiredArgsConstructor
public class InitiatePaymentHandler implements InitiatePaymentUseCase {

    private final PaymentCoreService paymentCoreService;
    private final IPaymentCommandRepository paymentRepository;

    @Override
    @Transactional
    public InitiatePaymentResult initiate(InitiatePaymentCommand command) {

        Payment payment = paymentCoreService.createPendingPayment(command);
        paymentRepository.save(payment);

        return new InitiatePaymentResult(
                payment.getId(),
                payment.getBookingId()
        );
    }
}
