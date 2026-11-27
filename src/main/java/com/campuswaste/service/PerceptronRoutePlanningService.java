package com.campuswaste.service;

import com.campuswaste.model.BinPrediction;
import com.campuswaste.model.RouteLocation;
import com.campuswaste.model.RoutePlanningResult;
import com.campuswaste.model.WasteBin;
import com.campuswaste.optimization.RouteOptimizationResult;
import com.campuswaste.optimization.SimulatedAnnealingRouteOptimizer;
import com.campuswaste.perceptron.PerceptronModel;
import com.campuswaste.perceptron.PerceptronTrainingResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PerceptronRoutePlanningService {

    private final SimulatedAnnealingRouteOptimizer optimizer;

    public PerceptronRoutePlanningService(SimulatedAnnealingRouteOptimizer optimizer) {
        this.optimizer = Objects.requireNonNull(optimizer, "optimizer must not be null");
    }

    public RoutePlanningResult planRoute(
            PerceptronTrainingResult trainingResult,
            RouteLocation depot,
            List<WasteBin> bins,
            Map<String, RouteLocation> locationsByBinId) {
        Objects.requireNonNull(trainingResult, "trainingResult must not be null");
        Objects.requireNonNull(depot, "depot must not be null");
        Objects.requireNonNull(bins, "bins must not be null");
        Objects.requireNonNull(locationsByBinId, "locationsByBinId must not be null");

        List<WasteBin> binSnapshot = new ArrayList<>(bins.size());
        Set<String> binIds = new HashSet<>();
        for (WasteBin bin : bins) {
            Objects.requireNonNull(bin, "bins must not contain null elements");
            if (!binIds.add(bin.id())) {
                throw new IllegalArgumentException("bin ids must be unique");
            }
            if (bin.id().equals(depot.id())) {
                throw new IllegalArgumentException("depot id must not equal a bin id");
            }
            binSnapshot.add(bin);
        }

        Map<String, RouteLocation> locationSnapshot = new HashMap<>();
        for (Map.Entry<String, RouteLocation> entry : locationsByBinId.entrySet()) {
            String binId = Objects.requireNonNull(entry.getKey(), "location map must not contain null keys");
            RouteLocation location = Objects.requireNonNull(
                    entry.getValue(), "location map must not contain null values");
            if (!binId.equals(location.id())) {
                throw new IllegalArgumentException("location map key must equal the location id");
            }
            locationSnapshot.put(binId, location);
        }
        for (WasteBin bin : binSnapshot) {
            if (!locationSnapshot.containsKey(bin.id())) {
                throw new IllegalArgumentException("every bin must have a location");
            }
        }
        if (locationSnapshot.size() != binSnapshot.size()) {
            throw new IllegalArgumentException("location map must not contain extra entries");
        }

        PerceptronModel model = trainingResult.model();
        List<BinPrediction> predictions = new ArrayList<>(binSnapshot.size());
        List<RouteLocation> pickupLocations = new ArrayList<>();
        for (WasteBin bin : binSnapshot) {
            int predictedLabel = model.predict(bin.toInputVector());
            RouteLocation location = locationSnapshot.get(bin.id());
            BinPrediction prediction = new BinPrediction(bin, location, predictedLabel);
            predictions.add(prediction);
            if (prediction.needsPickup()) {
                pickupLocations.add(location);
            }
        }

        RouteOptimizationResult optimizationResult = optimizer.optimize(depot, pickupLocations);
        return new RoutePlanningResult(predictions, optimizationResult);
    }
}