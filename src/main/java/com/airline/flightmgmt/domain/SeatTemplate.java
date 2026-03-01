package com.airline.flightmgmt.domain;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("seat_templates")
public class SeatTemplate extends BaseEntity {

    @Id
    private UUID seatTemplateId;
    private UUID aircraftId;
    private String seatNumber;
    private SeatType seatType;
    private FareClass fareClass;
    private Integer rowNumber;
    private boolean isBlocked;//Blocked seats are not available for booking,
    // used for maintenance or for crew or anything

    @Version
    private int version;
}
