package com.airline.payment.eventhandler;


import com.airline.payment.application.HandleSeatTimeoutUseCase;
import com.airline.payment.application.dto.ExpirePaymentCommand;
import com.airline.shared.annotation.EventService;
import com.airline.shared.events.SeatHoldingTimeExpired;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@EventService
public class BookingSeatTimeoutEventHandler {


    private final HandleSeatTimeoutUseCase handleSeatTimeoutUseCase;

    public void on(SeatHoldingTimeExpired event){

        handleSeatTimeoutUseCase.expirePayment(new ExpirePaymentCommand(event.getBookingId()));

    }
}
