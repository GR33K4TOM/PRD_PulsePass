CREATE TABLE venues (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    active BOOLEAN NOT NULL
);

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    event_code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    event_date TIMESTAMP NOT NULL,
    minimum_age INTEGER,
    venue_id BIGINT NOT NULL REFERENCES venues(id)
);

CREATE TABLE artists (
    id BIGSERIAL PRIMARY KEY,
    stage_name VARCHAR(100) UNIQUE NOT NULL,
    country VARCHAR(100),
    genre VARCHAR(50),
    active BOOLEAN NOT NULL
);

CREATE TABLE event_artists (
    event_id BIGINT NOT NULL REFERENCES events(id),
    artist_id BIGINT NOT NULL REFERENCES artists(id),
    PRIMARY KEY (event_id, artist_id)
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    city VARCHAR(100),
    birth_date DATE,
    user_id BIGINT UNIQUE NOT NULL REFERENCES users(id)
);

CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_code VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(50) NOT NULL,
    price NUMERIC(19, 2) NOT NULL CHECK (price >= 0),
    status VARCHAR(50) NOT NULL,
    purchase_date TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    event_id BIGINT NOT NULL REFERENCES events(id)
);
