package com.airline.payment.api;

import com.airline.payment.application.dto.InitiatePaymentCommand;
import com.airline.payment.application.InitiatePaymentResult;
import com.airline.payment.application.InitiatePaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final InitiatePaymentUseCase initiatePaymentUseCase;

    public PaymentController(InitiatePaymentUseCase initiatePaymentUseCase) {
        this.initiatePaymentUseCase = initiatePaymentUseCase;
    }

    @PostMapping("/initiate")
    @ResponseStatus(HttpStatus.CREATED)
    public InitiatePaymentResult initiate(@Valid @RequestBody InitiatePaymentCommand command) {
        return initiatePaymentUseCase.initiate(command);
    }
}
