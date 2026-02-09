-- =========================
-- ROUTES
-- =========================
CREATE TABLE routes (
    id UUID PRIMARY KEY,

    origin_airport CHAR(3) NOT NULL,
    destination_airport CHAR(3) NOT NULL,

    distance_km INT NOT NULL CHECK (distance_km > 0),
    estimated_duration_minutes INT NOT NULL CHECK (estimated_duration_minutes > 0),

    is_international BOOLEAN NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP


);
 CREATE INDEX idx_routes_origin_destination ON routes(origin_airport, destination_airport);
-- =========================
-- AIRCRAFT
-- =========================
CREATE TABLE aircraft (
    id UUID PRIMARY KEY,

    registration_number VARCHAR(20) NOT NULL UNIQUE,
    model VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(50) NOT NULL,
    total_seats INT NOT NULL CHECK (total_seats > 0),

    configuration JSONB NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- FLIGHTS
-- =========================
CREATE TABLE flights (
    id UUID PRIMARY KEY,

    flight_number VARCHAR(10) NOT NULL,
    aircraft_id UUID NOT NULL,
    route_id UUID NOT NULL,

    departure_time TIMESTAMPTZ NOT NULL,
    arrival_time TIMESTAMPTZ NOT NULL,

    status VARCHAR(20) NOT NULL CHECK (status IN ('SCHEDULED', 'CANCELLED', 'DELAYED', 'COMPLETED')),
    base_price DECIMAL(10,2) NOT NULL CHECK (base_price >= 0),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_flight_times CHECK (arrival_time > departure_time),

    CONSTRAINT fk_flights_aircraft FOREIGN KEY (aircraft_id) REFERENCES aircraft(id),
    CONSTRAINT fk_flights_routes   FOREIGN KEY (route_id)   REFERENCES routes(id)
);

-- Important indexes for search
CREATE INDEX idx_flights_route_departure_time ON flights (route_id, departure_time);

-- =========================
-- FARE CLASSES
-- =========================
CREATE TABLE fare_classes (
    id UUID PRIMARY KEY,

    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,

    baggage_allowance_kg INT NOT NULL CHECK (baggage_allowance_kg >= 0),
    carry_on_allowed BOOLEAN NOT NULL,

    refundable BOOLEAN NOT NULL,
    changeable BOOLEAN NOT NULL,
    change_fee_percentage DECIMAL(5,2) CHECK (change_fee_percentage >= 0 AND change_fee_percentage <= 100),

    priority_boarding BOOLEAN NOT NULL,
    meal_service BOOLEAN NOT NULL,
    seat_selection_free BOOLEAN NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- =========================
-- SEAT INVENTORY
-- =========================
CREATE TABLE seat_inventory (
    id UUID PRIMARY KEY,

    flight_id UUID NOT NULL,
    seat_number VARCHAR(5) NOT NULL,

    fare_class VARCHAR(30) NOT NULL, -- store fare class code
    status VARCHAR(20) NOT NULL CHECK (status IN ('AVAILABLE', 'LOCKED', 'BOOKED')),

    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_seat_inventory_flight_seat UNIQUE (flight_id, seat_number),
    CONSTRAINT fk_seat_inventory_flight FOREIGN KEY (flight_id) REFERENCES flights(id) ON DELETE CASCADE

);

-- Important index for seat availability by fare class
CREATE INDEX idx_seat_inventory_flight_status_fare
    ON seat_inventory (flight_id, status, fare_class);



-- =========================
-- BOOKINGS
-- =========================
CREATE TABLE bookings (
    id UUID PRIMARY KEY,

    booking_reference VARCHAR(10) NOT NULL UNIQUE,

    flight_id UUID NOT NULL,     -- do NOT add FK if Flight is another microservice DB
    customer_id UUID NOT NULL,

    total_amount DECIMAL(10,2) NOT NULL CHECK (total_amount >= 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),

    booking_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bookings_flight FOREIGN KEY (flight_id) REFERENCES flights(id)
);

-- Important indexes
CREATE INDEX idx_bookings_customer_id ON bookings (customer_id);
CREATE INDEX idx_bookings_flight_id ON bookings (flight_id);
CREATE INDEX idx_bookings_status_booking_date ON bookings (status, booking_date);

-- =========================
-- PASSENGERS
-- =========================
CREATE TABLE passengers (
    id UUID PRIMARY KEY,

    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,

    email VARCHAR(255),
    phone VARCHAR(20),

    passport_number VARCHAR(20),
    date_of_birth DATE,

    passenger_type VARCHAR(20) NOT NULL CHECK (passenger_type IN ('ADULT', 'CHILD', 'INFANT')),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Important index (booking summary)
CREATE INDEX idx_passengers_booking_id ON passengers (booking_id);

-- =========================
-- BOOKING SEATS
-- =========================
CREATE TABLE booking_seats (
    id UUID PRIMARY KEY,

    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    passenger_id UUID NOT NULL REFERENCES passengers(id) ON DELETE CASCADE,

    seat_number VARCHAR(5) NOT NULL,
    fare_class VARCHAR(20) NOT NULL, -- store fare class code
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP

);

-- Important indexes
CREATE INDEX idx_booking_seats_booking_id ON booking_seats (booking_id);
CREATE INDEX idx_booking_seats_passenger_id ON booking_seats (passenger_id);


-- =========================
-- TICKETS
-- =========================
CREATE TABLE tickets (
    id UUID PRIMARY KEY,

    ticket_number VARCHAR(20) NOT NULL UNIQUE,

    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    passenger_id UUID NOT NULL REFERENCES passengers(id) ON DELETE CASCADE,

    status VARCHAR(20) NOT NULL CHECK (status IN ('ISSUED', 'CANCELLED', 'REFUNDED')),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_ticket_booking_passenger UNIQUE (booking_id, passenger_id)
);

-- Important index (fetch tickets for booking)
CREATE INDEX idx_tickets_booking_id ON tickets (booking_id);



-- =========================
-- PAYMENTS
-- =========================
CREATE TABLE payments (
    id UUID PRIMARY KEY,

    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,

    amount DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
    currency CHAR(3) NOT NULL,

    payment_method VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL CHECK (payment_status IN ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'REFUNDED')),

    transaction_id VARCHAR(100),
    gateway_response JSONB,

    redirect_url TEXT,
    return_url TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Important indexes
CREATE INDEX idx_payments_booking_id ON payments (booking_id);
CREATE INDEX idx_payments_status_created_at ON payments (payment_status, created_at);
CREATE INDEX idx_payments_transaction_id ON payments (transaction_id);

--  enforce only one active pending payment per booking
CREATE UNIQUE INDEX uk_payments_one_pending_per_booking
 ON payments (booking_id) WHERE payment_status = 'PENDING';

 -- Add lock fields to seat_inventory for better performance and simpler logic
ALTER TABLE seat_inventory
  ADD COLUMN locked_by_booking_id UUID NULL,
  ADD COLUMN lock_expires_at TIMESTAMPTZ NULL;

-- Ensure that lock fields are consistent with status
   ALTER TABLE seat_inventory
    ADD CONSTRAINT chk_lock_fields
    CHECK (
      (status = 'LOCKED' AND locked_by_booking_id IS NOT NULL AND lock_expires_at IS NOT NULL)
      OR
      (status <> 'LOCKED' AND locked_by_booking_id IS NULL AND lock_expires_at IS NULL)
    );

 -- Index to quickly find locked seats that have expired
    CREATE INDEX idx_seat_inventory_lock_expiry
    ON seat_inventory (flight_id, lock_expires_at)
    WHERE status = 'LOCKED';

    ALTER TABLE bookings
    ADD COLUMN currency CHAR(3) NOT NULL DEFAULT 'INR';

    ALTER TABLE bookings
    ADD COLUMN hold_expires_at TIMESTAMPTZ;