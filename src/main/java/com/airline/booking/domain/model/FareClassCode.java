package com.airline.booking.domain.model;

import lombok.EqualsAndHashCode;

import java.util.Objects;

@EqualsAndHashCode
public final class FareClassCode {
    private final String value;

    public FareClassCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("fareClass code cannot be blank");
        }
        this.value = value.trim().toUpperCase();
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
