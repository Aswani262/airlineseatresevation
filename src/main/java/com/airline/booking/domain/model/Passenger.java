package com.airline.booking.domain.model;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("passengers")
public class Passenger extends BaseEntity {
    @Id
    private UUID id;
    private UUID bookingId;

    private String firstName;
    private String lastName;

    private String email;
    private String phone;

    private String passportNumber;
    private LocalDate dateOfBirth;

    private PassengerType passengerType;

}
