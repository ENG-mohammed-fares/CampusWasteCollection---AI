package com.campuswaste.model;

import com.campuswaste.optimization.RouteOptimizationResult;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record RoutePlanningResult(
        List<BinPrediction> predictions,
        RouteOptimizationResult optimizationResult) {

    public RoutePlanningResult {
        Objects.requireNonNull(predictions, "predictions must not be null");
        Objects.requireNonNull(optimizationResult, "optimizationResult must not be null");
        predictions = List.copyOf(predictions);

        Set<String> predictedBinIds = new HashSet<>();
        Set<String> pickupBinIds = new HashSet<>();
        for (BinPrediction prediction : predictions) {
            String binId = prediction.bin().id();
            if (!predictedBinIds.add(binId)) {
                throw new IllegalArgumentException("prediction bin ids must be unique");
            }
            if (prediction.needsPickup()) {
                pickupBinIds.add(binId);
            }
        }

        Set<String> routeBinIds = new HashSet<>();
        for (RouteLocation stop : optimizationResult.bestRoute()) {
            if (!routeBinIds.add(stop.id())) {
                throw new IllegalArgumentException("best route must not contain duplicate ids");
            }
            if (!pickupBinIds.contains(stop.id())) {
                throw new IllegalArgumentException("best route must contain only pickup bins");
            }
        }
        if (routeBinIds.size() != pickupBinIds.size()) {
            throw new IllegalArgumentException("best route must contain every pickup bin");
        }
    }

    public List<BinPrediction> pickupPredictions() {
        return predictions.stream().filter(BinPrediction::needsPickup).toList();
    }

    public List<BinPrediction> noPickupPredictions() {
        return predictions.stream().filter(prediction -> !prediction.needsPickup()).toList();
    }
}