

-- BOOKING MICROSERVICE

-- ENUM TYPES

CREATE TYPE booking_status AS ENUM ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'CHECKED_IN');
CREATE TYPE passenger_type AS ENUM ('ADULT', 'CHILD', 'INFANT');
CREATE TYPE ticket_status AS ENUM ('ISSUED', 'CANCELLED', 'REFUNDED');

-- Bookings (Aggregate Root)
CREATE TABLE bookings (
    id                  UUID NOT NULL,
    booking_reference   VARCHAR(50)  NOT NULL,
    flight_id           UUID         NOT NULL,
    customer_id         UUID         NOT NULL,
    total_amount        NUMERIC(12,2) NOT NULL,
    currency            CHAR(3)      NOT NULL,
    status              booking_status NOT NULL DEFAULT 'PENDING',
    booking_date_time   TIMESTAMPTZ  NOT NULL,
    booking_date        DATE         NOT NULL,               -- Partition key
    version             BIGINT       NOT NULL DEFAULT 0,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    PRIMARY KEY (id,booking_date)
) PARTITION BY RANGE (booking_date);

CREATE TABLE bookings_2026_03 PARTITION OF bookings
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

ALTER TABLE bookings ADD CONSTRAINT uk_bookings_booking_reference
    UNIQUE (booking_reference, booking_date);

CREATE INDEX idx_bookings_customer_id ON bookings (customer_id);
CREATE INDEX idx_bookings_flight_id ON bookings (flight_id);
CREATE INDEX idx_bookings_booking_date ON bookings (booking_date);

-- Create the parent table with partitioning
CREATE TABLE passengers (
    id               UUID NOT NULL,
    booking_id       UUID NOT NULL,
    passenger_order  INTEGER NOT NULL,

    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    phone            VARCHAR(50),
    date_of_birth    DATE NOT NULL,
    passenger_type   passenger_type NOT NULL,

    booking_date     DATE NOT NULL,  -- Denormalized partition key

    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (id, booking_date)
) PARTITION BY RANGE (booking_date);

CREATE TABLE passengers_2026_03 PARTITION OF passengers
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');


CREATE INDEX idx_passengers_booking_id ON passengers (booking_id);
CREATE INDEX idx_passengers_booking_date ON passengers (booking_date);

-- Create the parent table with partitioning
CREATE TABLE bookings_seats (
    booking_id        UUID NOT NULL,
    seat_order        INTEGER NOT NULL,

    passenger_id      UUID NOT NULL,
    seat_template_id  UUID NOT NULL,
    price             NUMERIC(12,2) NOT NULL,

    booking_date      DATE NOT NULL,  -- Denormalized partition key

    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (booking_id,booking_date)  -- Composite PK including partition key
) PARTITION BY RANGE (booking_date);

-- Create example monthly partitions
CREATE TABLE bookings_seats_2026_03 PARTITION OF bookings_seats
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE INDEX idx_bookings_seats_passenger_id ON bookings_seats (passenger_id);
CREATE INDEX idx_bookings_seats_booking_date ON bookings_seats (booking_date);


CREATE TABLE tickets (
    booking_id     UUID NOT NULL,
    ticket_order   INTEGER NOT NULL,

    ticket_number  VARCHAR(50) NOT NULL,
    passenger_id   UUID NOT NULL,
    status         ticket_status NOT NULL DEFAULT 'ISSUED',
    issued_at      TIMESTAMPTZ NOT NULL,

    booking_date   DATE NOT NULL,  -- Denormalized partition key

    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (booking_id, booking_date)  -- Composite PK including partition key
) PARTITION BY RANGE (booking_date);

-- Create example monthly partitions
CREATE TABLE tickets_2026_03 PARTITION OF tickets
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE INDEX idx_tickets_passenger_id ON tickets (passenger_id);
CREATE INDEX idx_tickets_booking_date ON tickets (booking_date);

ALTER TABLE tickets ADD CONSTRAINT uk_tickets_ticket_number
    UNIQUE (ticket_number, booking_date);



-- Flight Managment and Inventory Service

-- =============================================
-- ENUM TYPES
-- =============================================
CREATE TYPE fare_class AS ENUM ('ECONOMY', 'BUSINESS', 'FIRST');
CREATE TYPE flight_status AS ENUM ('SCHEDULED', 'DELAYED', 'CANCELLED', 'DEPARTED', 'ARRIVED');
CREATE TYPE hold_stage AS ENUM ('SEAT_SELECTION', 'PASSANGER_DETAILS', 'MEAL_SELECTION', 'PAYMENT');
CREATE TYPE seat_status AS ENUM ('AVAILABLE', 'HOLD', 'BOOKED');
CREATE TYPE seat_type AS ENUM ('WINDOW', 'AISLE', 'MIDDLE');


-- Aircrafts
CREATE TABLE aircrafts (
    id                   UUID PRIMARY KEY,
    registration_number  VARCHAR(50) NOT NULL UNIQUE,
    model                VARCHAR(100) NOT NULL,
    manufacturer         VARCHAR(100) NOT NULL,
    total_seats          INTEGER NOT NULL,
    configuration        JSONB NOT NULL,  -- Map<String, Integer> for fare class to seats

    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Routes
CREATE TABLE routes (
    id                           UUID PRIMARY KEY,
    origin_airport               VARCHAR(10) NOT NULL,
    destination_airport          VARCHAR(10) NOT NULL,
    distance_km                  INTEGER NOT NULL,
    estimated_duration_minutes   INTEGER NOT NULL,
    is_international             BOOLEAN NOT NULL DEFAULT FALSE,

    created_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_route_origin_destination UNIQUE (origin_airport, destination_airport)
);

-- Seat Templates
CREATE TABLE seat_templates (
    seat_template_id  UUID PRIMARY KEY,
    aircraft_id       UUID NOT NULL,
    seat_number       VARCHAR(10) NOT NULL,
    seat_type         seat_type NOT NULL,
    fare_class        fare_class NOT NULL,
    row_number        INTEGER NOT NULL,
    is_blocked        BOOLEAN NOT NULL DEFAULT FALSE,
    version           BIGINT    NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_seat_template_per_aircraft
        UNIQUE (aircraft_id, seat_number)
);

CREATE TABLE flights (
    id                   UUID NOT NULL,
    flight_number        VARCHAR(20) NOT NULL,
    aircraft_id          UUID NOT NULL,
    route_id             UUID NOT NULL,
    departure_time       TIMESTAMPTZ NOT NULL,
    arrival_time         TIMESTAMPTZ NOT NULL,
    status               flight_status NOT NULL DEFAULT 'SCHEDULED',
    flight_date          DATE NOT NULL,  -- Partition key
    total_seats          INTEGER NOT NULL,
    available_seats      INTEGER NOT NULL,

    version              INTEGER NOT NULL DEFAULT 0,

    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (id, flight_date)  -- Composite PK including partition key
) PARTITION BY RANGE (flight_date);

-- Create example monthly partitions
CREATE TABLE flights_2026_03 PARTITION OF flights
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

ALTER TABLE flights ADD CONSTRAINT uk_flights_flight_number
    UNIQUE (flight_number, flight_date);

CREATE INDEX idx_flights_aircraft_id ON flights (aircraft_id);
CREATE INDEX idx_flights_route_id ON flights (route_id);
CREATE INDEX idx_flights_flight_date ON flights (flight_date);

CREATE TABLE seat_assignments (
    id                 UUID NOT NULL,
    flight_id          UUID NOT NULL,
    seat_template_id   UUID NOT NULL,
    status             seat_status,
    booking_id         UUID,
    customer_id        UUID,
    lock_expires_at    TIMESTAMPTZ,
    hold_stage         hold_stage,
    flight_date        DATE NOT NULL,  -- Partition key

    version            INTEGER NOT NULL DEFAULT 0,

    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (id, flight_date)  -- Composite PK including partition key
) PARTITION BY RANGE (flight_date);

ALTER TABLE seat_assignments ADD CONSTRAINT uk_seat_assignment_per_flight_seat
    UNIQUE (flight_id, seat_template_id, flight_date);

CREATE TABLE seat_assignments_2026_03 PARTITION OF seat_assignments
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

CREATE INDEX idx_seat_assignments_flight_id ON seat_assignments (flight_id);
CREATE INDEX idx_seat_assignments_seat_template_id ON seat_assignments (seat_template_id);
CREATE INDEX idx_seat_assignments_flight_date ON seat_assignments (flight_date);
CREATE INDEX idx_seat_assignments_status ON seat_assignments (status);
CREATE INDEX idx_seat_assignments_lock_expires_at ON seat_assignments (lock_expires_at);


-- Payment Microservice

-- =============================================
-- ENUM TYPES
-- =============================================
CREATE TYPE payment_method AS ENUM (
    'CREDIT_CARD',
    'DEBIT_CARD',
    'UPI',
    'NET_BANKING',
    'WALLET'
);

CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'SUCCESS',
    'FAILED',
    'REFUNDED',
    'CANCELLED',
    'EXPIRED'
);

-- =============================================
--  PAYMENTS TABLE
-- =============================================
CREATE TABLE payments (
    id                 UUID PRIMARY KEY,
    booking_id         UUID NOT NULL,                    -- Reference to Booking microservice (no FK)
    amount             NUMERIC(15,2) NOT NULL,
    currency           CHAR(3) NOT NULL,                 -- ISO-4217 e.g. INR, USD
    payment_method     payment_method NOT NULL,
    status             payment_status NOT NULL DEFAULT 'PENDING',
    transaction_id     VARCHAR(100),                     -- Gateway transaction ID (Razorpay, Stripe, etc.)
    gateway_response   JSONB,                            -- Full raw response from payment gateway
    redirect_url       VARCHAR(500),
    return_url         VARCHAR(500),

    version            INTEGER NOT NULL DEFAULT 0,

    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================
 --  INDEXES
-- =============================================
CREATE INDEX idx_payments_booking_id     ON payments(booking_id);
CREATE INDEX idx_payments_status         ON payments(status);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payments_created_at     ON payments(created_at);