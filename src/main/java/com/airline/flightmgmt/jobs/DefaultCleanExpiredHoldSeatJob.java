package com.airline.flightmgmt.jobs;

import com.airline.flightmgmt.domain.HoldStage;
import com.airline.flightmgmt.domain.SeatAssignments;
import com.airline.flightmgmt.domain.SeatStatus;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.shared.annotation.UtilityService;
import com.airline.shared.events.PaymentTimeExpired;
import com.airline.shared.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@UtilityService
@Component
@RequiredArgsConstructor
public class DefaultCleanExpiredHoldSeatJob implements CleanExpiredHoldSeatJob {

    private final ISeatInventoryCommandRepository seatRepository;
    private final EventPublisher eventPublisher;

    @Scheduled(cron = "0 */5 * * * ?")  // Run every 5 minutes
    @Transactional
    public void cleanExpiredHolds() {
        log.info("Starting cleanup of expired HOLD seat assignments");

        OffsetDateTime now = OffsetDateTime.now();
        List<SeatAssignments> expiredSeats = seatRepository.findByStatusAndLockExpiresAtBefore(SeatStatus.HOLD, now);

        if (expiredSeats.isEmpty()) {
            log.debug("No expired HOLD seats found");
            return;
        }

        for (SeatAssignments seat : expiredSeats) {
            try {
                // If in PAYMENT stage, publish timeout event
                if (seat.getHoldStage() == HoldStage.PAYMENT && seat.getBookingId() != null) {
                    eventPublisher.publish(new PaymentTimeExpired(seat.getBookingId()));
                }
            } catch (Exception e) {
                log.error("Failed to publish PaymentTimeExpired for bookingId: {}", seat.getBookingId(), e);
            }
        }

        // Bulk delete all expired seats
        seatRepository.deleteAll(expiredSeats);

        log.info("Cleaned up {} expired HOLD seat assignments", expiredSeats.size());
    }
}