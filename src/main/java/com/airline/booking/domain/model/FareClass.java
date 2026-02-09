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
public class FareClass extends BaseEntity {

    private UUID id;

    private String code;                // e.g. ECONOMY, BUSINESS, FIRST
    private String name;
    private String description;

    private Integer baggageAllowanceKg;
    private Boolean carryOnAllowed;

    private Boolean refundable;
    private Boolean changeable;
    private BigDecimal changeFeePercentage;

    private Boolean priorityBoarding;
    private Boolean mealService;
    private Boolean seatSelectionFree;

}
