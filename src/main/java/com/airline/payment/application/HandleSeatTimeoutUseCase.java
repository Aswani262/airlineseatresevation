package com.airline.payment.application;

import com.airline.payment.application.dto.ExpirePaymentCommand;

public interface HandleSeatTimeoutUseCase {
    void expirePayment(ExpirePaymentCommand expirePaymentCommand);
}
