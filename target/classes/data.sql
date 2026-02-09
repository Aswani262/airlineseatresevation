-- =========================
-- ROUTES
-- =========================
SET TIME ZONE 'UTC';

INSERT INTO routes (id, origin_airport, destination_airport, distance_km, estimated_duration_minutes, is_international)
VALUES
('11111111-1111-1111-1111-111111111111', 'DEL', 'BLR', 1740, 150, false),
('22222222-2222-2222-2222-222222222222', 'DEL', 'DXB', 2190, 210, true);

-- =========================
-- AIRCRAFT
-- =========================
INSERT INTO aircraft (id, registration_number, model, manufacturer, total_seats, configuration)
VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'VT-IND', 'A320', 'Airbus', 180,
 '{"ECONOMY": 140, "BUSINESS": 20, "PREMIUM": 20}'),

('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'VT-JET', 'B737', 'Boeing', 160,
 '{"ECONOMY": 120, "BUSINESS": 20, "PREMIUM": 20}');

-- =========================
-- FARE CLASSES
-- =========================
INSERT INTO fare_classes (
    id, code, name, description,
    baggage_allowance_kg, carry_on_allowed,
    refundable, changeable, change_fee_percentage,
    priority_boarding, meal_service, seat_selection_free
) VALUES
('f1111111-1111-1111-1111-111111111111', 'ECONOMY', 'Economy', 'Standard economy fare',
 15, true, false, true, 30.00, false, true, false),

('f2222222-2222-2222-2222-222222222222', 'BUSINESS', 'Business', 'Business class fare',
 30, true, true, true, 5.00, true, true, true),

('f3333333-3333-3333-3333-333333333333', 'PREMIUM', 'Premium Economy', 'Premium economy with extra legroom',
 25, true, true, true, 10.00, true, true, true);

-- =========================
-- FLIGHTS
-- =========================
INSERT INTO flights (
    id, flight_number, aircraft_id, route_id,
    departure_time, arrival_time, status, base_price
)
VALUES
('faaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'AI-202',
 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
 '11111111-1111-1111-1111-111111111111',
 NOW() + INTERVAL '1 day',
 NOW() + INTERVAL '1 day 2 hours',
 'SCHEDULED',
 4500.00),

('fbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'AI-909',
 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
 '22222222-2222-2222-2222-222222222222',
 NOW() + INTERVAL '2 days',
 NOW() + INTERVAL '2 days 3 hours',
 'SCHEDULED',
 12000.00);

-- =========================
-- SEAT INVENTORY (VALID UUIDs)
-- =========================
INSERT INTO seat_inventory (id, flight_id, seat_number, fare_class, status, price)
VALUES
-- AI-202
('a1111111-1111-1111-1111-111111111111', 'faaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '12A', 'ECONOMY', 'AVAILABLE', 4500.00),
('a2222222-2222-2222-2222-222222222222', 'faaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '12B', 'ECONOMY', 'AVAILABLE', 4500.00),
('a3333333-3333-3333-3333-333333333333', 'faaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '2A',  'BUSINESS', 'AVAILABLE', 9000.00),
('a4444444-4444-4444-4444-444444444444', 'faaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '3A',  'PREMIUM',  'AVAILABLE', 7000.00),

-- AI-909
('b5555555-5555-5555-5555-555555555555', 'fbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '14A', 'ECONOMY', 'AVAILABLE', 12000.00),
('b6666666-6666-6666-6666-666666666666', 'fbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '14B', 'ECONOMY', 'AVAILABLE', 12000.00),
('b7777777-7777-7777-7777-777777777777', 'fbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '2A',  'BUSINESS', 'AVAILABLE', 22000.00),
('b8888888-8888-8888-8888-888888888888', 'fbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '3A',  'PREMIUM',  'AVAILABLE', 18000.00);
