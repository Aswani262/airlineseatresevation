package com.airline.flightmgmt.domain;

import com.airline.shared.model.BaseEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
//We can have a separate table for seat assignments to keep track of which seats are assigned to which bookings, and their status (e.g., reserved, occupied, available). This allows us to manage seat availability and assignments efficiently,
// especially when dealing with changes or cancellations.
//We can use booking table also to keep track of seat assignments, but having a separate seat assignment table allows for more flexibility and better performance when querying seat availability and managing assignments, especially in scenarios where multiple bookings may involve the same flight and seat template. It also helps to decouple the booking logic from the seat management logic
// , making the system more modular and easier to maintain.
//Rather than calling booking service to get the booking details every
// time we need to check seat availability or assign a seat,
// we can directly query the seat assignment table,
// which can be optimized for these specific queries.
// This reduces the load on the booking service and
// improves overall system performance.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("seat_assignments")
public class SeatAssignments extends BaseEntity {

    @Id
    private UUID id;
    private UUID flightId;
    private UUID seatTemplateId;
    private SeatStatus status;
    private UUID bookingId;
    private UUID customerId;

    //At the time of seat selection by the passenger , we are holding the seat for a certain period of time
    // then we extend the expire time when adding the passenger details
    // again we extend the expire time when added the meal option
    // again we extend the expire time when enter to payment page and complete the payment

    //Why we are doing this , because that impact the inventory if user select the seat and then abandon the booking process,
    // that seat will be held for a long time and not available for other passengers to select,
    // which can lead to lost sales and customer dissatisfaction.
    // By extending the expire time at each step of the booking process,
    // we can ensure that the seat is held for a reasonable amount of time while the passenger completes their booking,
    // but also released in a timely manner if they abandon the process.

    // (this will be last time we extend the expire time, after payment complete we will confirm the seat assignment and set the status to occupied)
    // If payment is not completed within the hold period, the seat will be released and made available for other passengers to select.
    // if payment gateway provide the expired event or callback, we can also listen to that event and release the seat immediately instead of waiting for the expire time.
    // We have to also run some schedule to periodically check for expired seat holds and release them, to ensure that seats are not held indefinitely due to system failures or missed callbacks.
    private OffsetDateTime lockExpiresAt;

    private HoldStage holdStage;

    //Only to use for partitioning and querying, not for any business logic
    //This is best suited for partitioning and query
    //because we are already using flight date for flight table for partitioning and these
    // two tables are closely related and often queried together, it makes sense to use the same partitioning key for both tables. This allows for efficient joins and queries that involve both tables,
    // as they will be located in the same partition based on the flight date.
    private LocalDate flightDate;

    @Version
    private Integer version;

}
