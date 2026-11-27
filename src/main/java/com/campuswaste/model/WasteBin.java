package com.campuswaste.model;

import java.util.Objects;

public record WasteBin(String id, double reading1, double reading2, double reading3) {

    public WasteBin {
        Objects.requireNonNull(id, "Waste bin id must not be null");
        id = id.trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Waste bin id must not be blank");
        }
        requireFinite(reading1, "reading1");
        requireFinite(reading2, "reading2");
        requireFinite(reading3, "reading3");
    }

    public double[] toInputVector() {
        return new double[] {reading1, reading2, reading3};
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    name + " must be a finite number but was " + value);
        }
    }
}