package com.airline.booking.domain.model;

import com.airline.shared.model.BaseEntity;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket extends BaseEntity {

    private UUID id;
    private String ticketNumber;

    private UUID bookingId;
    private UUID passengerId;

    private TicketStatus status;
    private OffsetDateTime issuedAt;
}
