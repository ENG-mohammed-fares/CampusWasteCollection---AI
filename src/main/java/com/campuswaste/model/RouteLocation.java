package com.campuswaste.model;

import java.util.Objects;

public record RouteLocation(String id, double xMeters, double yMeters) {

    public RouteLocation {
        Objects.requireNonNull(id, "Route location id must not be null");
        id = id.trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Route location id must not be blank");
        }
        requireFinite(xMeters, "xMeters");
        requireFinite(yMeters, "yMeters");
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be a finite number but was " + value);
        }
    }
}