package com.airline.payment.application;


import com.airline.payment.application.dto.InitiatePaymentCommand;

public interface InitiatePaymentUseCase {
    InitiatePaymentResult initiate(InitiatePaymentCommand command);
}
