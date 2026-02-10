package com.airline.booking.domain.model;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

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
@Table ("bookings")
public class Booking extends BaseEntity {

    @Id
    private UUID id;

    private String bookingReference;

    private UUID flightId;
    private UUID customerId;

    private BigDecimal totalAmount;
    private String currency;
    private BookingStatus status;
    private OffsetDateTime holdExpiresAt;

    private OffsetDateTime bookingDate;

    //This will become the aggregate when we are sharing the passenger with different booking entities, but for simplicity we will keep it here for now.
    @Builder.Default
    @MappedCollection(idColumn = "booking_id",keyColumn = "passenger_order")
    private List<Passenger> passengers = new ArrayList<>();

    @Builder.Default
    @MappedCollection(idColumn = "booking_id",keyColumn = "seat_order")
    private List<BookingSeat> seats = new ArrayList<>();

    @Builder.Default
    @MappedCollection(idColumn = "booking_id",keyColumn = "ticket_order")
    private List<Ticket> tickets = new ArrayList<>();

    @Version
    private Long version;
}