package com.airline.searchengine.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "flight_search")
public class FlightSearchDoc {

    @Id
    private UUID id;

    // Flight fields
    @Field(type = FieldType.Keyword)
    private String flightNumber;

    @Field(type = FieldType.Keyword)
    private UUID aircraftId;

    @Field(type = FieldType.Keyword)
    private UUID routeId;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime departureTime;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private OffsetDateTime arrivalTime;

    @Field(type = FieldType.Keyword)
    private String status; // SCHEDULED/DELAYED...

    @Field(type = FieldType.Double)
    private BigDecimal basePrice;

    // Route (denormalized)
    @Field(type = FieldType.Keyword)
    private String originAirport;

    @Field(type = FieldType.Keyword)
    private String destinationAirport;

    @Field(type = FieldType.Integer)
    private Integer distanceKm;

    @Field(type = FieldType.Integer)
    private Integer estimatedDurationMinutes;

    @Field(type = FieldType.Boolean)
    private Boolean isInternational;

    // Aircraft (denormalized)
    @Field(type = FieldType.Keyword)
    private String registrationNumber;

    @Field(type = FieldType.Text) // for partial search: "Airbus", "Boeing"
    private String model;

    @Field(type = FieldType.Text)
    private String manufacturer;

    @Field(type = FieldType.Integer)
    private Integer totalSeats;

    // fareClass -> seats
    // stored as JSON-like object in ES
    @Field(type = FieldType.Object)
    private Map<String, Integer> configuration;
}
