package com.airline.payment.application;


public interface InitiatePaymentUseCase {
    InitiatePaymentResult initiate(InitiatePaymentCommand command);
}
