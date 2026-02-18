INSERT INTO routes (id, origin_airport, destination_airport, distance_km, estimated_duration_minutes, is_international, created_at, updated_at)
VALUES
('11111111-1111-1111-1111-111111111111', 'DEL', 'BLR', 1740, 150, false, NOW(), NOW()),
('22222222-2222-2222-2222-222222222222', 'DEL', 'BOM', 1148, 130, false, NOW(), NOW()),
('33333333-3333-3333-3333-333333333333', 'DEL', 'DXB', 2190, 210, true, NOW(), NOW());


INSERT INTO aircrafts (id, registration_number, model, manufacturer, total_seats, seat_configuration, created_at, updated_at)
VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'VT-IND', 'A320', 'Airbus', 180,
 '{"ECONOMY": 140, "BUSINESS": 20, "PREMIUM": 20}', NOW(), NOW()),

('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'VT-JET', 'B737', 'Boeing', 160,
 '{"ECONOMY": 120, "BUSINESS": 20, "PREMIUM": 20}', NOW(), NOW());
INSERT INTO fare_classes (
    id, code, name, description, baggage_allowance_kg, carry_on_allowed,
    refundable, changeable, change_fee_percentage,
    priority_boarding, meal_service, created_at, updated_at, version
)
VALUES
('f1111111-1111-1111-1111-111111111111', 'ECONOMY', 'Economy Saver',
 'Basic economy fare with limited benefits',
 15, true, false, false, 0.00, false, false, NOW(), NOW(), 0),

('f2222222-2222-2222-2222-222222222222', 'PREMIUM', 'Premium Economy',
 'Extra legroom with meals included',
 25, true, true, true, 10.00, true, true, NOW(), NOW(), 0),

('f3333333-3333-3333-3333-333333333333', 'BUSINESS', 'Business Class',
 'Luxury seating, lounge access, refundable',
 35, true, true, true, 0.00, true, true, NOW(), NOW(), 0);


INSERT INTO flights (
    id, flight_number, aircraft_id, route_id,
    departure_time, arrival_time, status,
    total_seats, available_seats, seat_configuration, base_price,
    created_at, updated_at
)
VALUES
('dddddddd-dddd-dddd-dddd-dddddddddddd', 'AI-203',
 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
 '11111111-1111-1111-1111-111111111111',
 NOW() + INTERVAL '1 day',
 NOW() + INTERVAL '1 day 2 hours 30 minutes',
 'SCHEDULED',
 180, 180,
 '{"ECONOMY": 140, "BUSINESS": 20, "PREMIUM": 20}',
 5500.00,
 NOW(), NOW()),

('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'EK-511',
 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
 '33333333-3333-3333-3333-333333333333',
 NOW() + INTERVAL '2 days',
 NOW() + INTERVAL '2 days 3 hours 30 minutes',
 'SCHEDULED',
 160, 160,
 '{"ECONOMY": 120, "BUSINESS": 20, "PREMIUM": 20}',
 12500.00,
 NOW(), NOW());


INSERT INTO seat_inventory (
    id, flight_id, seat_number, fare_class, status,
    locked_by_booking_id, lock_expires_at, price,
    created_at, updated_at, version
)
VALUES
('51111111-1111-1111-1111-111111111111', 'dddddddd-dddd-dddd-dddd-dddddddddddd', '12A', 'ECONOMY',  'AVAILABLE', NULL, NULL, 5500.00, NOW(), NOW(), 0),
('52222222-2222-2222-2222-222222222222', 'dddddddd-dddd-dddd-dddd-dddddddddddd', '12B', 'ECONOMY',  'AVAILABLE', NULL, NULL, 5500.00, NOW(), NOW(), 0),
('53333333-3333-3333-3333-333333333333', 'dddddddd-dddd-dddd-dddd-dddddddddddd', '5A',  'BUSINESS', 'AVAILABLE', NULL, NULL, 15000.00, NOW(), NOW(), 0);
