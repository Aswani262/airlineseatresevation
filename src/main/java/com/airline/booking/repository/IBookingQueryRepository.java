package com.airline.booking.repository;

import com.airline.booking.domain.model.Booking;
import org.springframework.data.repository.Repository;

import java.util.UUID;

public interface IBookingQueryRepository extends Repository<Booking, UUID> {
}
