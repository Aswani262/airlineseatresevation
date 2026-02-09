package com.airline.booking.domain.model;

import com.airline.shared.model.BaseEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeat extends BaseEntity {

    private UUID id;
    private UUID bookingId;
    private UUID passengerId;

    private String seatNumber;

    private FareClassCode fareClass;
    private BigDecimal price;

}
