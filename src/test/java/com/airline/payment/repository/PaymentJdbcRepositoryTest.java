package com.airline.payment.repository;

import com.airline.payment.domain.Payment;
import com.airline.payment.domain.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import(PaymentJdbcRepository.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:airline;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class PaymentJdbcRepositoryTest {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PaymentJdbcRepository repo;

    @BeforeEach
    void setupSchema() {
        jdbcTemplate.execute("""
            DROP TABLE IF EXISTS payments;

            CREATE TABLE payments (
                id UUID PRIMARY KEY,
                booking_id UUID NOT NULL,
                amount DECIMAL(12,2) NOT NULL,
                currency VARCHAR(10) NOT NULL,
                payment_method VARCHAR(30) NULL,
                payment_status VARCHAR(30) NOT NULL,
                transaction_id VARCHAR(100) NULL,
                redirect_url VARCHAR(500) NULL,
                return_url VARCHAR(500) NULL,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
                updated_at TIMESTAMP WITH TIME ZONE NULL
            );
        """);
    }

    @Test
    void insert_shouldInsertRow_withPaymentMethod() {
        UUID paymentId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        Payment p = Payment.builder()
                .id(paymentId)
                .bookingId(bookingId)
                .amount(new BigDecimal("999.50"))
                .currency("INR")
                .paymentMethod(PaymentMethod.UPI)
                .status("INITIATED") // repo writes this into payment_status
                .transactionId(null)
                .redirectUrl("https://pay/redirect")
                .returnUrl("https://app/return")
                .build();

        repo.insert(p);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM payments WHERE id = ?",
                paymentId
        );

        assertThat(row.get("id")).isEqualTo(paymentId);
        assertThat(row.get("booking_id")).isEqualTo(bookingId);
        assertThat(((BigDecimal) row.get("amount"))).isEqualByComparingTo("999.50");
        assertThat(row.get("currency")).isEqualTo("INR");
        assertThat(row.get("payment_method")).isEqualTo("UPI");
        assertThat(row.get("payment_status")).isEqualTo("INITIATED");
        assertThat(row.get("transaction_id")).isNull();
        assertThat(row.get("redirect_url")).isEqualTo("https://pay/redirect");
        assertThat(row.get("return_url")).isEqualTo("https://app/return");

        // created_at should be present (NOW default)
        assertThat(row.get("created_at")).isNotNull();
    }

    @Test
    void insert_shouldInsertRow_withNullPaymentMethod() {
        UUID paymentId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        Payment p = Payment.builder()
                .id(paymentId)
                .bookingId(bookingId)
                .amount(new BigDecimal("10.00"))
                .currency("INR")
                .paymentMethod(null)
                .status("INITIATED")
                .transactionId("txn-1")
                .redirectUrl(null)
                .returnUrl(null)
                .build();

        repo.insert(p);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM payments WHERE id = ?",
                paymentId
        );

        assertThat(row.get("payment_method")).isNull();
        assertThat(row.get("transaction_id")).isEqualTo("txn-1");
    }

    @Test
    void findById_shouldReturnPayment_whenExists_andMapEnum() {
        UUID paymentId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        // Insert directly (bypass repo insert if you want full control)
        jdbcTemplate.update("""
            INSERT INTO payments (
                id, booking_id, amount, currency, payment_method, payment_status,
                transaction_id, redirect_url, return_url, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
        """, paymentId, bookingId, new BigDecimal("500.00"), "USD",
                "CARD", "SUCCESS", "txn-999",
                "https://redirect", "https://return");

        Optional<Payment> out = repo.findById(paymentId);

        assertThat(out).isPresent();
        Payment p = out.get();

        assertThat(p.getId()).isEqualTo(paymentId);
        assertThat(p.getBookingId()).isEqualTo(bookingId);
        assertThat(p.getAmount()).isEqualByComparingTo("500.00");
        assertThat(p.getCurrency()).isEqualTo("USD");
        assertThat(p.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);

        // repo maps payment_status -> Payment.status (string)
        assertThat(p.getStatus()).isEqualTo("SUCCESS");

        assertThat(p.getTransactionId()).isEqualTo("txn-999");
        assertThat(p.getRedirectUrl()).isEqualTo("https://redirect");
        assertThat(p.getReturnUrl()).isEqualTo("https://return");
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        Optional<Payment> out = repo.findById(UUID.randomUUID());
        assertThat(out).isEmpty();
    }

    @Test
    void updateStatus_shouldUpdateWhenFromStatusMatches_andSetUpdatedAtAndTransactionId() {
        UUID paymentId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO payments (
                id, booking_id, amount, currency, payment_method, payment_status,
                transaction_id, redirect_url, return_url, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NULL)
        """, paymentId, bookingId, new BigDecimal("123.00"), "INR",
                "UPI", "INITIATED", null, null, null);

        int updated = repo.updateStatus(paymentId, "INITIATED", "SUCCESS", "txn-abc");
        assertThat(updated).isEqualTo(1);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM payments WHERE id = ?", paymentId);
        assertThat(row.get("payment_status")).isEqualTo("SUCCESS");
        assertThat(row.get("transaction_id")).isEqualTo("txn-abc");

        Object updatedAt = row.get("updated_at");
        assertThat(updatedAt).isNotNull();

        // H2 can return OffsetDateTime or Timestamp depending on driver; accept either
        assertThat(updatedAt).isInstanceOfAny(OffsetDateTime.class, Timestamp.class);
    }

    @Test
    void updateStatus_shouldReturn0_whenFromStatusDoesNotMatch() {
        UUID paymentId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO payments (
                id, booking_id, amount, currency, payment_method, payment_status,
                transaction_id, redirect_url, return_url, created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NULL)
        """, paymentId, bookingId, new BigDecimal("123.00"), "INR",
                "UPI", "SUCCESS", "txn-old", null, null);

        int updated = repo.updateStatus(paymentId, "INITIATED", "FAILED", "txn-new");
        assertThat(updated).isEqualTo(0);

        // unchanged
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM payments WHERE id = ?", paymentId);
        assertThat(row.get("payment_status")).isEqualTo("SUCCESS");
        assertThat(row.get("transaction_id")).isEqualTo("txn-old");
    }
}
