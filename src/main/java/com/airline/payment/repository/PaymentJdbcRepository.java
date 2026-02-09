package com.airline.payment.repository;

import com.airline.payment.domain.Payment;
import com.airline.payment.domain.PaymentMethod;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentJdbcRepository implements PaymentRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PaymentJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(Payment p) {
        String sql = """
        INSERT INTO payments (
            id, booking_id, amount, currency, payment_method, payment_status,
            transaction_id, redirect_url, return_url
        )
        VALUES (
            :id, :bookingId, :amount, :currency, :paymentMethod, :paymentStatus,
            :transactionId, :redirectUrl, :returnUrl
        )
    """;

        var params = new MapSqlParameterSource()
                .addValue("id", p.getId())
                .addValue("bookingId", p.getBookingId())
                .addValue("amount", p.getAmount())
                .addValue("currency", p.getCurrency())
                .addValue("paymentMethod", p.getPaymentMethod() == null ? null : p.getPaymentMethod().name())
                .addValue("paymentStatus", p.getStatus())
                .addValue("transactionId", p.getTransactionId())
                .addValue("redirectUrl", p.getRedirectUrl())
                .addValue("returnUrl", p.getReturnUrl());

        jdbcTemplate.update(sql, params);
    }


    @Override
    public Optional<Payment> findById(UUID paymentId) {
        String sql = """
            SELECT id, booking_id, amount, currency, payment_method, payment_status,
                   transaction_id, redirect_url, return_url, created_at, updated_at
            FROM payments
            WHERE id = :paymentId
        """;

        var params = new MapSqlParameterSource()
                .addValue("paymentId", paymentId);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> map(rs))
                .stream()
                .findFirst();
    }

    @Override
    public int updateStatus(UUID paymentId, String from, String to, String transactionId) {
        String sql = """
            UPDATE payments
            SET payment_status = :toStatus,
                transaction_id = :transactionId,
                updated_at = NOW()
            WHERE id = :paymentId
              AND payment_status = :fromStatus
        """;

        var params = new MapSqlParameterSource()
                .addValue("paymentId", paymentId)
                .addValue("fromStatus", from)
                .addValue("toStatus", to)
                .addValue("transactionId", transactionId);

        return jdbcTemplate.update(sql, params);
    }

    private Payment map(ResultSet rs) throws java.sql.SQLException {
        return Payment.builder()
                .id(rs.getObject("id", UUID.class))
                .bookingId(rs.getObject("booking_id", UUID.class))
                .amount(rs.getBigDecimal("amount"))
                .currency(rs.getString("currency"))
                .paymentMethod(rs.getString("payment_method") == null
                        ? null
                        : PaymentMethod.valueOf(rs.getString("payment_method")))
                .status(rs.getString("payment_status"))
                .transactionId(rs.getString("transaction_id"))
                .redirectUrl(rs.getString("redirect_url"))
                .returnUrl(rs.getString("return_url"))
                .build();
    }
}
