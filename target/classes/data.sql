-- Seed data for routes
INSERT INTO routes (id, origin_airport, destination_airport, distance_km, estimated_duration_minutes, is_international, created_at, updated_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'DEL', 'BOM', 1140, 120, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222222', 'BOM', 'SIN', 3910, 300, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333333', 'JFK', 'LHR', 5540, 420, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Seed data for aircrafts
-- Assuming configuration as JSONB with seats per class
INSERT INTO aircrafts (id, registration_number, model, manufacturer, total_seats, configuration, created_at, updated_at)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'VT-ABC', 'A320', 'Airbus', 180, '{"ECONOMY": 150, "BUSINESS": 30, "FIRST": 0}'::JSONB, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'VT-DEF', 'B737', 'Boeing', 189, '{"ECONOMY": 189, "BUSINESS": 0, "FIRST": 0}'::JSONB, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'VT-GHI', 'A350', 'Airbus', 300, '{"ECONOMY": 200, "BUSINESS": 80, "FIRST": 20}'::JSONB, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Seed data for fare_classes
INSERT INTO fare_classes (id, code, name, description, baggage_allowance_kg, carry_on_allowed, refundable, changeable, change_fee_percentage, priority_boarding, meal_service, seat_selection_free, created_at, updated_at, version)
VALUES
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'ECONOMY', 'Economy', 'Standard economy class', 20, TRUE, FALSE, TRUE, 10.00, FALSE, FALSE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'BUSINESS', 'Business', 'Business class with extra amenities', 30, TRUE, TRUE, TRUE, 5.00, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'FIRST', 'First', 'First class luxury', 40, TRUE, TRUE, TRUE, 0.00, TRUE, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1);

-- Seed data for flights
INSERT INTO flights (id, flight_number, aircraft_id, route_id, departure_time, arrival_time, status, base_price, created_at, updated_at)
VALUES
    ('11111111-aaaa-aaaa-aaaa-111111111111', 'AI101', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', '2026-03-01 10:00:00+00', '2026-03-01 12:00:00+00', 'SCHEDULED', 5000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22222222-bbbb-bbbb-bbbb-222222222222', 'SQ202', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222', '2026-03-02 14:00:00+00', '2026-03-02 19:00:00+00', 'SCHEDULED', 15000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('33333333-cccc-cccc-cccc-333333333333', 'BA303', 'cccccccc-cccc-cccc-cccc-cccccccccccc', '33333333-3333-3333-3333-333333333333', '2026-03-03 08:00:00+00', '2026-03-03 15:00:00+00', 'SCHEDULED', 30000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Seed data for seat_inventory
-- For each flight, add some seats.
INSERT INTO seat_inventory (id, flight_id, seat_number, fare_class, status, price, created_at, updated_at, version)
VALUES
    -- Economy seats for flight1
    ('50000000-0001-0001-0001-000000000001', '11111111-aaaa-aaaa-aaaa-111111111111', '1A', 'ECONOMY', 'AVAILABLE', 5000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    ('50000000-0001-0001-0001-000000000002', '11111111-aaaa-aaaa-aaaa-111111111111', '1B', 'ECONOMY', 'AVAILABLE', 5000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    ('50000000-0001-0001-0001-000000000003', '11111111-aaaa-aaaa-aaaa-111111111111', '2A', 'ECONOMY', 'AVAILABLE', 5000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    -- Business seats for flight1
    ('50000000-0001-0001-0001-000000000004', '11111111-aaaa-aaaa-aaaa-111111111111', '10A', 'BUSINESS', 'AVAILABLE', 10000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    ('50000000-0001-0001-0001-000000000005', '11111111-aaaa-aaaa-aaaa-111111111111', '10B', 'BUSINESS', 'AVAILABLE', 10000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),

    -- For flight2 (B737 all economy)
    ('50000000-0002-0002-0002-000000000001', '22222222-bbbb-bbbb-bbbb-222222222222', '1A', 'ECONOMY', 'AVAILABLE', 15000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    ('50000000-0002-0002-0002-000000000002', '22222222-bbbb-bbbb-bbbb-222222222222', '1B', 'ECONOMY', 'AVAILABLE', 15000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),

    -- For flight3 (A350 with first)
    ('50000000-0003-0003-0003-000000000001', '33333333-cccc-cccc-cccc-333333333333', '1A', 'FIRST', 'AVAILABLE', 50000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    ('50000000-0003-0003-0003-000000000002', '33333333-cccc-cccc-cccc-333333333333', '20A', 'BUSINESS', 'AVAILABLE', 40000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    ('50000000-0003-0003-0003-000000000003', '33333333-cccc-cccc-cccc-333333333333', '30A', 'ECONOMY', 'AVAILABLE', 30000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1);