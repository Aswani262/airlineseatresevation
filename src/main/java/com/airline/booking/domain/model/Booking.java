package com.airline.booking.domain.model;

import com.airline.shared.model.BaseEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Booking extends BaseEntity {

    private UUID id;
    private String bookingReference;

    private UUID flightId;
    private UUID customerId;

    private BigDecimal totalAmount;
    private BookingStatus status;

    private OffsetDateTime bookingDate;

    @Builder.Default
    private List<Passenger> passengers = new ArrayList<>();

    @Builder.Default
    private List<BookingSeat> seats = new ArrayList<>();

    @Builder.Default
    private List<Ticket> tickets = new ArrayList<>();
}
