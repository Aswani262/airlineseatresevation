-- Enum types
CREATE TYPE booking_status AS ENUM ('DRAFT', 'CONFIRMED', 'CANCELLED', 'EXPIRED');

CREATE TYPE passenger_type AS ENUM ('ADULT', 'CHILD', 'INFANT');

CREATE TYPE seat_status AS ENUM ('AVAILABLE', 'LOCKED', 'BOOKED');

-- Assumed enum for FareClassCode/FareClass based on examples (ECONOMY, BUSINESS, FIRST)
CREATE TYPE fare_class_type AS ENUM ('ECONOMY', 'BUSINESS', 'FIRST');

-- Assumed enum for TicketStatus (common values for tickets)
CREATE TYPE ticket_status AS ENUM ('ISSUED', 'CANCELLED', 'PENDING');

CREATE TYPE flight_status AS ENUM ('SCHEDULED', 'DELAYED', 'CANCELLED', 'DEPARTED', 'ARRIVED');

-- Enum for PaymentMethod (assumed common values since not provided)
CREATE TYPE payment_method AS ENUM ('CREDIT_CARD', 'DEBIT_CARD', 'UPI', 'NET_BANKING', 'WALLET', 'PAYPAL');

-- Enum for PaymentStatus (assumed common values since not provided)
CREATE TYPE payment_status AS ENUM ('INITIATED', 'PENDING', 'SUCCESSFUL', 'FAILED', 'REFUNDED', 'CANCELLED');

-- Tables

-- Flight Management Bounded Context
CREATE TABLE fare_classes (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100),
    description TEXT,
    baggage_allowance_kg INTEGER,
    carry_on_allowed BOOLEAN,
    refundable BOOLEAN,
    changeable BOOLEAN,
    change_fee_percentage NUMERIC(5, 2),
    priority_boarding BOOLEAN,
    meal_service BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE seat_inventory (
    id UUID PRIMARY KEY,
    flight_id UUID NOT NULL REFERENCES flights(id) ON DELETE CASCADE,
    seat_number VARCHAR(10) NOT NULL,
    fare_class fare_class_type NOT NULL,
    status seat_status NOT NULL,
    locked_by_booking_id UUID,
    lock_expires_at TIMESTAMP WITH TIME ZONE,
    price NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE aircrafts (
    id UUID PRIMARY KEY,
    registration_number VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    manufacturer VARCHAR(255) NOT NULL,
    total_seats INTEGER NOT NULL,
    seat_configuration JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE flights (
    id UUID PRIMARY KEY,
    flight_number VARCHAR(255) NOT NULL,
    aircraft_id UUID NOT NULL REFERENCES aircrafts(id),
    route_id UUID NOT NULL REFERENCES routes(id),
    departure_time TIMESTAMP WITH TIME ZONE NOT NULL,
    arrival_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status flight_status NOT NULL,
    total_seats INTEGER NOT NULL,
    available_seats INTEGER NOT NULL,
    seat_configuration JSONB NOT NULL,
    base_price NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);


CREATE TABLE routes (
    id UUID PRIMARY KEY,
    origin_airport VARCHAR(255) NOT NULL,
    destination_airport VARCHAR(255) NOT NULL,
    distance_km INTEGER NOT NULL,
    estimated_duration_minutes INTEGER NOT NULL,
    is_international BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Booking Management Bounded Context
CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    booking_reference VARCHAR(20) NOT NULL,
    flight_id UUID NOT NULL ,
    customer_id UUID NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status booking_status_type NOT NULL,
    hold_expires_at TIMESTAMP WITH TIME ZONE,
    booking_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

-- Passengers Table
CREATE TABLE passengers (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    passenger_order INTEGER NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    date_of_birth DATE,
    passenger_type passenger_type_type NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_passengers_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    UNIQUE (booking_id, passenger_order)
);

-- Booking Seats Table
CREATE TABLE bookings_seats (
    booking_id UUID NOT NULL,
    seat_order INTEGER NOT NULL,
    passenger_id UUID,
    seat_number VARCHAR(10) NOT NULL,
    fare_class fare_class_type NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_booking_seats_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_seats_passenger FOREIGN KEY (passenger_id) REFERENCES passengers(id) ON DELETE SET NULL,
    PRIMARY KEY (booking_id, seat_order),
    UNIQUE (booking_id, seat_number)  -- Enforce unique seat numbers per booking
);

-- Tickets Table
CREATE TABLE tickets (
    booking_id UUID NOT NULL,
    ticket_order INTEGER NOT NULL,
    ticket_number VARCHAR(20) NOT NULL,
    passenger_id UUID,
    status ticket_status_type NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_tickets_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_tickets_passenger FOREIGN KEY (passenger_id) REFERENCES passengers(id) ON DELETE SET NULL,
    PRIMARY KEY (booking_id, ticket_order),
    UNIQUE (ticket_number)  -- Assuming ticket numbers are globally unique
);


-- Enum for FlightStatus


-- Payments Management Bounded Context
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_method payment_method NOT NULL,
    status payment_status NOT NULL,
    transaction_id VARCHAR(255),
    gateway_response JSONB,
    redirect_url VARCHAR(512),
    return_url VARCHAR(512),
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

---------------------------------

-- Indexes for fare_classes
CREATE INDEX idx_fare_classes_code ON fare_classes(code);

-- Indexes for seat_inventory
CREATE INDEX idx_seat_inventory_flight_id ON seat_inventory(flight_id);
CREATE INDEX idx_seat_inventory_available ON seat_inventory(flight_id, fare_class, status);
CREATE INDEX idx_seat_inventory_locked_by ON seat_inventory(locked_by_booking_id);

-- For schedule-based cleanup of expired locks, an index on lock_expires_at can help
CREATE INDEX idx_seat_inventory_lock_expires_at ON seat_inventory(lock_expires_at);

-- Indexes for bookings
CREATE INDEX idx_bookings_booking_reference ON bookings(booking_reference);

CREATE INDEX idx_bookings_flight_id ON bookings(flight_id);

CREATE INDEX idx_bookings_customer_id ON bookings(customer_id);

CREATE INDEX idx_bookings_status ON bookings(status);

CREATE INDEX idx_bookings_hold_expires_at ON bookings(hold_expires_at);

CREATE INDEX idx_bookings_booking_date ON bookings(booking_date);

-- Indexes for passengers
CREATE INDEX idx_passengers_booking_id ON passengers(booking_id);
CREATE INDEX idx_passengers_email ON passengers(email);

-- Indexes for booking_seats
CREATE INDEX idx_booking_seats_booking_id ON booking_seats(booking_id);
CREATE INDEX idx_booking_seats_passenger_id ON booking_seats(passenger_id);
CREATE INDEX idx_booking_seats_seat_number ON booking_seats(seat_number);

-- Indexes for tickets
CREATE INDEX idx_tickets_ticket_number ON tickets(ticket_number);
CREATE INDEX idx_tickets_booking_id ON tickets(booking_id);
CREATE INDEX idx_tickets_passenger_id ON tickets(passenger_id);
CREATE INDEX idx_tickets_status ON tickets(status);

-- Indexes for routes
CREATE INDEX idx_routes_origin_airport ON routes(origin_airport);
CREATE INDEX idx_routes_destination_airport ON routes(destination_airport);
CREATE INDEX idx_routes_origin_destination ON routes(origin_airport, destination_airport);

-- Indexes for aircrafts
CREATE INDEX idx_aircrafts_registration_number ON aircrafts(registration_number);
CREATE INDEX idx_aircrafts_model ON aircrafts(model);

-- Indexes for flights
CREATE INDEX idx_flights_flight_number ON flights(flight_number);
CREATE INDEX idx_flights_aircraft_id ON flights(aircraft_id);
CREATE INDEX idx_flights_route_id ON flights(route_id);
CREATE INDEX idx_flights_departure_time ON flights(departure_time);
CREATE INDEX idx_flights_arrival_time ON flights(arrival_time);
CREATE INDEX idx_flights_status ON flights(status);
CREATE INDEX idx_flights_search ON flights(route_id, departure_time, status);

-- Indexes for payments
CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_payment_status ON payments(payment_status);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payments_created_at ON payments(created_at);

ALTER TABLE seat_inventory
ADD CONSTRAINT unique_flight_seat UNIQUE (flight_id, seat_number);