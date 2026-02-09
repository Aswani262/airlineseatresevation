package com.airline.booking.repository;

import com.airline.booking.application.command.dto.BookSeatCommand;
import com.airline.booking.domain.model.Booking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository {


    void saveBooking(Booking booking);

    int updateStatus(UUID bookingId, String fromStatus, String toStatus, OffsetDateTime updatedAt);

}
