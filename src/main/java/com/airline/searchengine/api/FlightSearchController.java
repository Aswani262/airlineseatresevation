package com.airline.searchengine.api;

import com.airline.searchengine.domain.FlightSearchDoc;
import com.airline.searchengine.service.FlightSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search-engine/search-flight")
public class FlightSearchController {

    private final FlightSearchService service;

    @GetMapping
    public Page<FlightSearchDoc> search(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime departFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime departTo,

            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean international,

            // free text query
            @RequestParam(required = false) String q,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.search(origin, destination, departFrom, departTo, status, international, q, page, size);
    }
}
