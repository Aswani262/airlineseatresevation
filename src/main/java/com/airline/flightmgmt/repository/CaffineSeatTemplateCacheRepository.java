package com.airline.flightmgmt.repository;

import com.airline.flightmgmt.domain.AircraftSeatTemplateCache;
import com.airline.flightmgmt.domain.SeatTemplate;
import com.airline.flightmgmt.domain.SeatTemplateCache;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class CaffineSeatTemplateCacheRepository implements ISeatTemplateCacheRepository{
    private final ISeatTemplateRepository seatTemplateRepository;

    @Cacheable(value = "seatTemplate", key = "#aircraftId")
    @Override
    public AircraftSeatTemplateCache getSeatTemplate(UUID aircraftId) {

        AircraftSeatTemplateCache aircraftSeatTemplateCache = new AircraftSeatTemplateCache();

        List<SeatTemplate> seatTemplates = seatTemplateRepository.findByAircraftId(aircraftId);

        List<SeatTemplateCache> seatTemplateCaches =  seatTemplates.stream().map(seatTemplate -> {SeatTemplateCache seatTemplateCache = new SeatTemplateCache();
            seatTemplateCache.setSeatNumber(seatTemplate.getSeatNumber());
            seatTemplateCache.setSeatType(seatTemplate.getSeatType());
            seatTemplateCache.setSeatTemplateId(seatTemplate.getSeatTemplateId());
            seatTemplateCache.setBlocked(seatTemplate.isBlocked());
            seatTemplateCache.setFareClass(seatTemplate.getFareClass());
            return seatTemplateCache;

        }).toList();
        aircraftSeatTemplateCache.setSeatsCache(seatTemplateCaches);
        return aircraftSeatTemplateCache;
    }
}
