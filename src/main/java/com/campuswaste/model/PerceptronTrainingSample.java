package com.campuswaste.model;

public record PerceptronTrainingSample(double reading1, double reading2, double reading3, int pickupLabel) {

    public PerceptronTrainingSample {
        requireFinite(reading1, "reading1");
        requireFinite(reading2, "reading2");
        requireFinite(reading3, "reading3");
        if (pickupLabel != 1 && pickupLabel != -1) {
            throw new IllegalArgumentException("pickupLabel must be +1 or -1 but was " + pickupLabel);
        }
    }

    public double[] toInputVector() {
        return new double[] {reading1, reading2, reading3};
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be a finite number but was " + value);
        }
    }
}