package com.airline.booking.domain.model;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("tickets")
public class Ticket extends BaseEntity {

    private UUID bookingId;
    private String ticketNumber;
    private UUID passengerId;
    private TicketStatus status;
    private OffsetDateTime issuedAt;
    private LocalDate bookingDate;
}
