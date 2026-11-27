package com.campuswaste.optimization;

import com.campuswaste.model.RouteLocation;
import java.util.List;
import java.util.Objects;

public record RouteOptimizationResult(
        List<RouteLocation> bestRoute,
        int iterationsCompleted,
        List<Double> currentDistanceHistoryMeters,
        List<Double> bestDistanceHistoryMeters) {

    public RouteOptimizationResult {
        Objects.requireNonNull(bestRoute, "bestRoute must not be null");
        Objects.requireNonNull(currentDistanceHistoryMeters, "currentDistanceHistoryMeters must not be null");
        Objects.requireNonNull(bestDistanceHistoryMeters, "bestDistanceHistoryMeters must not be null");
        if (iterationsCompleted < 0) {
            throw new IllegalArgumentException("iterationsCompleted must not be negative");
        }
        bestRoute = List.copyOf(bestRoute);
        currentDistanceHistoryMeters = List.copyOf(currentDistanceHistoryMeters);
        bestDistanceHistoryMeters = List.copyOf(bestDistanceHistoryMeters);
        if (currentDistanceHistoryMeters.isEmpty() || bestDistanceHistoryMeters.isEmpty()) {
            throw new IllegalArgumentException("distance histories must not be empty");
        }
        int expectedHistorySize = iterationsCompleted + 1;
        if (currentDistanceHistoryMeters.size() != expectedHistorySize
                || bestDistanceHistoryMeters.size() != expectedHistorySize) {
            throw new IllegalArgumentException("distance histories must contain iterationsCompleted + 1 entries");
        }
        requireValidDistances(currentDistanceHistoryMeters);
        requireValidDistances(bestDistanceHistoryMeters);
        if (currentDistanceHistoryMeters.get(0).doubleValue() != bestDistanceHistoryMeters.get(0).doubleValue()) {
            throw new IllegalArgumentException("initial current and best distances must be equal");
        }
        for (int index = 0; index < expectedHistorySize; index++) {
            if (bestDistanceHistoryMeters.get(index) > currentDistanceHistoryMeters.get(index)) {
                throw new IllegalArgumentException("best distance must not exceed current distance");
            }
            if (index > 0 && bestDistanceHistoryMeters.get(index) > bestDistanceHistoryMeters.get(index - 1)) {
                throw new IllegalArgumentException("best distance history must not increase");
            }
        }
    }

    public double bestDistanceMeters() {
        return bestDistanceHistoryMeters.get(bestDistanceHistoryMeters.size() - 1);
    }

    public double finalCurrentDistanceMeters() {
        return currentDistanceHistoryMeters.get(currentDistanceHistoryMeters.size() - 1);
    }

    private static void requireValidDistances(List<Double> distances) {
        for (Double distance : distances) {
            if (!Double.isFinite(distance) || distance < 0.0) {
                throw new IllegalArgumentException("distances must be finite and non-negative");
            }
        }
    }
}