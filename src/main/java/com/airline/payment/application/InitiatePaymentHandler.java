package com.airline.payment.application;

import com.airline.payment.domain.Payment;
import com.airline.payment.service.DemoGatewaySimulator;
import com.airline.payment.repository.PaymentRepository;
import com.airline.payment.service.PaymentCoreService;
import com.airline.shared.annoation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@ApplicationService
public class InitiatePaymentHandler implements InitiatePaymentUseCase {

    private final PaymentCoreService paymentCoreService;
    private final PaymentRepository paymentRepository;
    private final DemoGatewaySimulator demoGatewaySimulator;

    public InitiatePaymentHandler(PaymentCoreService paymentCoreService,
                                 PaymentRepository paymentRepository,
                                 DemoGatewaySimulator demoGatewaySimulator) {
        this.paymentCoreService = paymentCoreService;
        this.paymentRepository = paymentRepository;
        this.demoGatewaySimulator = demoGatewaySimulator;
    }

    @Override
    @Transactional
    public InitiatePaymentResult initiate(InitiatePaymentCommand command) {

        Payment payment = paymentCoreService.createPendingPayment(command);
        paymentRepository.insert(payment);

        // DEMO ONLY: simulate gateway callback async (success/fail)
        demoGatewaySimulator.simulateGatewayCallback(payment.getId(), payment.getBookingId());

        return new InitiatePaymentResult(
                payment.getId(),
                payment.getBookingId(),
                payment.getStatus(),
                "DEMO_GATEWAY",
                payment.getRedirectUrl(),
                Map.of(
                        "paymentId", payment.getId().toString(),
                        "amount", payment.getAmount().toPlainString(),
                        "currency", payment.getCurrency(),
                        "returnUrl", payment.getReturnUrl()
                ),
                payment.getCreatedAt()
        );
    }
}
