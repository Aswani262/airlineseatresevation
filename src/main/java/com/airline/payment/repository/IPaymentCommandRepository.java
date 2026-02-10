package com.airline.payment.repository;

import com.airline.payment.domain.Payment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IPaymentCommandRepository extends CrudRepository<Payment, UUID> {
}
