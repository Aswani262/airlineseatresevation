package com.airline.flightmgmt.api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FareClassResponse {
    private String code;
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
