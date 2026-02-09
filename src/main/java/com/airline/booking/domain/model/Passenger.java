package com.airline.booking.domain.model;

import com.airline.shared.model.BaseEntity;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passenger extends BaseEntity {

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
