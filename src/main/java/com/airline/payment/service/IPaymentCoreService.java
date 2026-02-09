package com.airline.payment.service;

import com.airline.payment.application.InitiatePaymentCommand;
import com.airline.payment.domain.Payment;

public interface IPaymentCoreService {

     Payment createPendingPayment(InitiatePaymentCommand cmd);
}
