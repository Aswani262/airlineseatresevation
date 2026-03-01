-- Seed Data for Aircraft
INSERT INTO aircrafts (id, registration_number, model, manufacturer, total_seats, configuration, created_at, updated_at)
VALUES 
    ('19449d6a-b5b1-46a5-88ca-7f3ea6dcd909', 'N123AB', 'Boeing 737-800', 'Boeing', 189, '{"ECONOMY": 165, "BUSINESS": 24}'::jsonb, NOW(), NOW()),
    ('587b4dac-0421-4ada-bfdf-1feebbb6b1b4', 'N456CD', 'Airbus A320', 'Airbus', 180, '{"ECONOMY": 150, "BUSINESS": 30}'::jsonb, NOW(), NOW());

-- Seed Data for Route
INSERT INTO routes (id, origin_airport, destination_airport, distance_km, estimated_duration_minutes, is_international, created_at, updated_at)
VALUES 
    ('277eda7c-481e-4c7d-877f-d2d94164d9ec', 'DEL', 'BOM', 1136, 120, FALSE, NOW(), NOW()),
    ('26b4115d-f9f6-40e7-b6ed-a1e73e924238', 'JFK', 'LHR', 5542, 420, TRUE, NOW(), NOW()),
    ('500d3ebe-495c-4fd0-bad0-ce4d1c36d61b', 'LAX', 'SFO', 543, 90, FALSE, NOW(), NOW());

-- Seed Data for Flight
-- Note: Assuming flights use the first aircraft and various routes. Adjust IDs as needed.
INSERT INTO flights (id, flight_number, aircraft_id, route_id, departure_time, arrival_time, status, flight_date, total_seats, available_seats, version, created_at, updated_at)
VALUES 
    ('00e218f0-6336-4e4a-8bce-21fe28abb8e6', 'AI101', '19449d6a-b5b1-46a5-88ca-7f3ea6dcd909', '277eda7c-481e-4c7d-877f-d2d94164d9ec', '2026-03-15T10:00:00Z', '2026-03-15T12:00:00Z', 'SCHEDULED', '2026-03-15',   189, 189, 0, NOW(), NOW()),
    ('725105d0-8669-4ddb-923d-e1e305dc0fa1', 'AI102', '19449d6a-b5b1-46a5-88ca-7f3ea6dcd909', '277eda7c-481e-4c7d-877f-d2d94164d9ec', '2026-03-16T14:00:00Z', '2026-03-16T16:00:00Z', 'SCHEDULED', '2026-03-16',   189, 189, 0, NOW(), NOW()),
    ('57c3b9c8-aabc-4799-9d56-c4eb1eedbe81', 'UA201', '587b4dac-0421-4ada-bfdf-1feebbb6b1b4', '26b4115d-f9f6-40e7-b6ed-a1e73e924238', '2026-03-20T08:00:00Z', '2026-03-20T15:00:00Z', 'SCHEDULED', '2026-03-20',   180, 180, 0, NOW(), NOW());

-- Seed Data for SeatTemplate
-- Note: Assuming these are for the first aircraft (Boeing 737-800). Generate sample seats across fare classes.
INSERT INTO seat_templates (seat_template_id, aircraft_id, seat_number, seat_type, fare_class, row_number, is_blocked, version, created_at, updated_at)
VALUES 
    ('5bbafd4d-c5f2-4138-b6da-07e0f510c59c', '19449d6a-b5b1-46a5-88ca-7f3ea6dcd909', '1A', 'WINDOW', 'BUSINESS', 1, FALSE, 0, NOW(), NOW()),
    ('c9d06913-1ea8-4ded-a72e-f6f21ff801b6', '19449d6a-b5b1-46a5-88ca-7f3ea6dcd909', '1B', 'AISLE', 'BUSINESS', 1, FALSE, 0, NOW(), NOW()),
    ('c3f8e6f4-009f-4a60-b661-71d167fe87b6', '19449d6a-b5b1-46a5-88ca-7f3ea6dcd909', '2A', 'WINDOW', 'BUSINESS', 2, FALSE, 0, NOW(), NOW()),
    ('fb73730a-5cff-44d5-997c-18fe1cfc3ec6', '19449d6a-b5b1-46a5-88ca-7f3ea6dcd909', '2B', 'AISLE', 'BUSINESS', 2, FALSE, 0, NOW(), NOW()),
    ('7097cfa7-d156-4d0d-91d1-adab543aa22d', '19449d6a-b5b1-46a5-88ca-7f3ea6dcd909', '10A', 'WINDOW', 'ECONOMY', 10, FALSE, 0, NOW(), NOW()),
    ('f6a77087-8291-40a3-b066-3c8200ba5b92', '19449d6a-b5b1-46a5-88ca-7f3ea6dcd909', '10B', 'MIDDLE', 'ECONOMY', 10, FALSE, 0, NOW(), NOW()),
    ('1c370cc1-cff8-447b-b20c-6659f6cd5333', '19449d6a-b5b1-46a5-88ca-7f3ea6dcd909', '10C', 'AISLE', 'ECONOMY', 10, FALSE, 0, NOW(), NOW()),
    ('498f4e68-11d8-42f2-aab6-71ce1bb105ff', '19449d6a-b5b1-46a5-88ca-7f3ea6dcd909', '15A', 'WINDOW', 'ECONOMY', 15, TRUE, 0, NOW(), NOW()),  -- Blocked seat example
    ('acebb0ab-4444-4538-9877-c93b23880a9c', '19449d6a-b5b1-46a5-88ca-7f3ea6dcd909', '15B', 'MIDDLE', 'ECONOMY', 15, FALSE, 0, NOW(), NOW()),
    ('d75bf3e4-d8a5-4015-818f-85f731a53033', '19449d6a-b5b1-46a5-88ca-7f3ea6dcd909', '15C', 'AISLE', 'ECONOMY', 15, FALSE, 0, NOW(), NOW());