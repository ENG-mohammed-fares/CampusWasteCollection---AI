package com.campuswaste.model;

import java.util.Objects;

public record BinPrediction(WasteBin bin, RouteLocation location, int predictedLabel) {

    private static final int PICKUP_LABEL = 1;
    private static final int NO_PICKUP_LABEL = -1;

    public BinPrediction {
        Objects.requireNonNull(bin, "bin must not be null");
        Objects.requireNonNull(location, "location must not be null");
        if (predictedLabel != PICKUP_LABEL && predictedLabel != NO_PICKUP_LABEL) {
            throw new IllegalArgumentException("predictedLabel must be +1 or -1");
        }
        if (!bin.id().equals(location.id())) {
            throw new IllegalArgumentException("bin and location must have the same id");
        }
    }

    public boolean needsPickup() {
        return predictedLabel == PICKUP_LABEL;
    }
}