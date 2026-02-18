package com.airline.shared.service;

import com.airline.shared.annotation.EventService;
import com.airline.shared.events.PaymentStatusEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@EventService
@RequiredArgsConstructor
public class DefaultEventPublisher implements EventPublisher{

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(PaymentStatusEvent event) {
            // In a real implementation, this would publish to a message broker (e.g., Kafka, RabbitMQ)
            // For this demo we'll just print the event to the console
          applicationEventPublisher.publishEvent(event);
    }
}
