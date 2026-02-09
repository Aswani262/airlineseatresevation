package com.airline.shared.service;

import com.airline.shared.events.PaymentStatusEvent;

public interface EventPublisher {
    void publish(PaymentStatusEvent event);
}
