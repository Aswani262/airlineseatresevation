package com.airline.flightmgmt.domain;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("seat_inventory")
public class SeatInventory extends BaseEntity {

    @Id
    private UUID id;

    private UUID flightId;
    private String seatNumber;

    private FareClass fareClass;
    private SeatStatus status;

    private UUID lockedByBookingId;
    private OffsetDateTime lockExpiresAt;

    private BigDecimal price;

    @Version
    private Integer version;

}
