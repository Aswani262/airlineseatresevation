package com.airline.searchengine.repository;

import com.airline.searchengine.domain.FlightSearchDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.UUID;

public interface FlightSearchRepository extends ElasticsearchRepository<FlightSearchDoc, UUID> {

    List<FlightSearchDoc> findByOriginAirportAndDestinationAirport(String origin, String destination);

    List<FlightSearchDoc> findByRouteId(UUID routeId);

    List<FlightSearchDoc> findByAircraftId(UUID aircraftId);
}
