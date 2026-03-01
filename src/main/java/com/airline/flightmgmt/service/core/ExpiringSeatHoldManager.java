package com.airline.flightmgmt.service.core;

import com.airline.flightmgmt.domain.*;
import com.airline.flightmgmt.repository.IFlightCacheRepository;
import com.airline.flightmgmt.repository.ISeatInventoryCommandRepository;
import com.airline.shared.events.PaymentTimeExpired;
import com.airline.shared.service.EventPublisher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpiringMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
//Its in-memory expiring timer , no need to use any other distributed service like redis or event bus
//This will instantly remove at time of expiry
//If service or any one instance is crashed , that will we removed by schedular
//With this schedular frequancy is optimized
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiringSeatHoldManager {

    private final ISeatInventoryCommandRepository seatRepository;
    private final IFlightCacheRepository flightCacheRepository;
    private final EventPublisher eventPublisher;

    private ExpiringSeatHoldManager self;


    private ExpiringMap<SeatKey, SeatHoldInfo> seatHolds;

    @PostConstruct
    public void init() {
        seatHolds = ExpiringMap.builder()
                .variableExpiration()
                .asyncExpirationListener(new SeatExpiryListener())
                .build();
    }


    public void holdSeat(UUID flightId, UUID customerId,
                         HoldStage stage, OffsetDateTime ttl) {

        SeatKey key = new SeatKey(flightId, customerId);
        SeatHoldInfo info = new SeatHoldInfo(customerId, null, stage,
                ttl);

        seatHolds.put(key, info, ttl.getSecond(), TimeUnit.SECONDS);

        log.info("Seat held in ExpiringMap: {} for {} min (stage={})", key, ttl.getMinute(), stage);
    }

    public void removeHold(UUID flightId, UUID customerId) {
        seatHolds.remove(new SeatKey(flightId, customerId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSeatExpiry(SeatKey key, SeatHoldInfo holdInfo) {
        try {
            log.info("Releasing seat: {}", key);

            //Use flight date in query for partition purning
            LocalDate flightDate = flightCacheRepository.getFlight(key.flightId()).getFlightDate();

            //Get all the seats which is hold by this customer and in hold status
            List<SeatAssignments> seats = seatRepository
                    .findByFlightIdAndCustomerIdAndStatusAndFlightDate(key.flightId(), key.customerId(),SeatStatus.HOLD,flightDate);

            if (seats.isEmpty()) {
                log.debug("Seat already released or not in HOLD to ignoring");
                return;
            }

            seatRepository.deleteAll(seats);

            //Payment time out expired
            if(holdInfo.getHoldStage() == HoldStage.PAYMENT){
                eventPublisher.publish(new PaymentTimeExpired(holdInfo.getBookingId()));
            }

            log.info("Seat successfully released via ExpiringMap listener: {}", key);

        } catch (Exception e) {
            log.error("Failed to release seat {} on expiry", key, e);
        }
    }

    private class SeatExpiryListener implements ExpirationListener<SeatKey, SeatHoldInfo> {
        @Override
        public void expired(SeatKey key, SeatHoldInfo info) {
            self.handleSeatExpiry(key, info);
        }
    }
}