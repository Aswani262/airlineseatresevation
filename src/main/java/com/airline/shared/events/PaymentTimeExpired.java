package com.airline.shared.events;

import lombok.Getter;

import java.util.UUID;

@Getter
public class PaymentTimeExpired extends IntegrationEvent {
    private UUID bookingId;


    public PaymentTimeExpired(UUID bookingId){
        super();
        this.bookingId = bookingId;
    }

    @Override
    public String getEventType() {
        return "payment.time.expired.v1";
    }
}
