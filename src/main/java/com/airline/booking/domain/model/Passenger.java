package com.airline.booking.domain.model;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;
// Passenger entity representing a passenger associated with a booking
// This entity is should be treated as aggregate root because
// it is  associated with the different Booking aggregate and  shared across other aggregates.
// and have its own lifecycle and identity. It is also used in the Booking aggregate as a part of the booking process,
// but it can exist independently and be referenced by other aggregates such as Ticket or SeatInventory.

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
    private LocalDate dateOfBirth;
    private PassengerType passengerType;
    private LocalDate bookingDate;
}
