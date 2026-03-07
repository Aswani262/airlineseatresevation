package com.airline.shared.events;

import com.airline.flightmgmt.domain.HoldStage;
import lombok.Getter;

import java.util.UUID;

@Getter
public class SeatHoldingTimeExpired extends IntegrationEvent{

   private UUID bookingId;
   private HoldStage holdStage;

   public SeatHoldingTimeExpired(UUID bookingId,HoldStage holdStage){
       super();
       this.bookingId = bookingId;
       this.holdStage = holdStage;
   }

    @Override
    public String getEventType() {
        return "seat.holding.time.expired.v1";
    }
}
