package com.airline.booking.domain.model;

import com.airline.flightmgmt.domain.FareClass;
import com.airline.shared.model.BaseEntity;
import lombok.*;


import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatInventory extends BaseEntity {

    private UUID id;

    private UUID flightId;
    private String seatNumber;

    private FareClass fareClass;
    private SeatStatus status;

    private BigDecimal price;

}
