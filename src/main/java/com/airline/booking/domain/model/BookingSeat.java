package com.airline.booking.domain.model;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("bookings_seats")
public class BookingSeat extends BaseEntity {

    private UUID bookingId;
    private UUID passengerId;

    private String seatNumber;

    private FareClass fareClass;
    private BigDecimal price;

}
