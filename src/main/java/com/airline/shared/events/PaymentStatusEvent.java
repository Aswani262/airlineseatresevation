package com.airline.shared.events;



import lombok.Getter;

import java.util.UUID;

@Getter
public final class PaymentStatusEvent extends IntegrationEvent {

    public enum Status { SUCCESS, FAILED, EXPIRED }

    private final UUID bookingId;
    private final Status status;
    private final String reason;

    public PaymentStatusEvent(UUID bookingId, Status status, String reason) {
        super();
        this.bookingId = bookingId;
        this.status = status;
        this.reason = reason;
    }

    @Override
    public String getEventType() { return "PaymentStatus"; }

}
