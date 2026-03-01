package com.airline.booking.repository;

import com.airline.booking.domain.model.Booking;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IBookingCommandRepository extends CrudRepository<Booking, UUID> {

}