package com.airline.searchengine.service;

import co.elastic.clients.json.JsonData;
import com.airline.searchengine.domain.FlightSearchDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class FlightSearchService {

    private final ElasticsearchOperations operations;

    public Page<FlightSearchDoc> search(
            String origin,
            String destination,
            OffsetDateTime departFrom,
            OffsetDateTime departTo,
            String status,
            Boolean international,
            String q,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Query query = NativeQuery.builder()
                .withQuery(qb -> qb.bool(b -> {
                    if (origin != null && !origin.isBlank()) {
                        b.filter(f -> f.term(t -> t.field("originAirport").value(origin)));
                    }
                    if (destination != null && !destination.isBlank()) {
                        b.filter(f -> f.term(t -> t.field("destinationAirport").value(destination)));
                    }
                    if (status != null && !status.isBlank()) {
                        b.filter(f -> f.term(t -> t.field("status").value(status)));
                    }
                    if (international != null) {
                        b.filter(f -> f.term(t -> t.field("isInternational").value(international)));
                    }
                    if (departFrom != null || departTo != null) {
                        b.filter(f -> f.range(r -> {
                            r.field("departureTime");

                            if (departFrom != null) {
                                r.gte(JsonData.of(departFrom));
                            }

                            if (departTo != null) {
                                r.lte(JsonData.of(departTo));
                            }

                            return r;
                        }));
                    }

                    // free text query across useful fields
                    if (q != null && !q.isBlank()) {
                        b.must(m -> m.multiMatch(mm -> mm
                                .query(q)
                                .fields("flightNumber", "model", "manufacturer", "registrationNumber")
                        ));
                    }

                    return b;
                }))
                .withPageable(pageable)
                .build();

        SearchHits<FlightSearchDoc> hits = operations.search(query, FlightSearchDoc.class);
        return SearchHitSupport.searchPageFor(hits, pageable)
                .map(SearchHit::getContent);
    }
}
