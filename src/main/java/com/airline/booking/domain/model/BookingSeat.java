package com.airline.booking.domain.model;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private UUID seatTemplateId;
    private BigDecimal price;
    private LocalDate bookingDate;
}
