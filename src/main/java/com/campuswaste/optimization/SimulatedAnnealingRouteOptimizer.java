package com.campuswaste.optimization;

import com.campuswaste.model.RouteLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class SimulatedAnnealingRouteOptimizer {

    public static final double DEFAULT_INITIAL_TEMPERATURE = 60.0;
    public static final double DEFAULT_COOLING_RATE = 0.997;
    public static final int DEFAULT_MAX_ITERATIONS = 10000;

    private final double initialTemperature;
    private final double coolingRate;
    private final int maxIterations;
    private final RandomGenerator randomGenerator;
    private final RouteDistanceCalculator distanceCalculator;

    public SimulatedAnnealingRouteOptimizer() {
        this(
                DEFAULT_INITIAL_TEMPERATURE,
                DEFAULT_COOLING_RATE,
                DEFAULT_MAX_ITERATIONS,
                RandomGenerator.getDefault());
    }

    public SimulatedAnnealingRouteOptimizer(
            double initialTemperature,
            double coolingRate,
            int maxIterations,
            RandomGenerator randomGenerator) {
        if (!Double.isFinite(initialTemperature) || initialTemperature <= 0.0) {
            throw new IllegalArgumentException("initialTemperature must be finite and greater than zero");
        }
        if (!Double.isFinite(coolingRate) || coolingRate <= 0.0 || coolingRate >= 1.0) {
            throw new IllegalArgumentException("coolingRate must be finite and strictly between zero and one");
        }
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("maxIterations must be greater than zero");
        }
        Objects.requireNonNull(randomGenerator, "randomGenerator must not be null");
        this.initialTemperature = initialTemperature;
        this.coolingRate = coolingRate;
        this.maxIterations = maxIterations;
        this.randomGenerator = randomGenerator;
        this.distanceCalculator = new RouteDistanceCalculator();
    }

    public RouteOptimizationResult optimize(RouteLocation depot, List<RouteLocation> pickupBins) {
        Objects.requireNonNull(depot, "depot must not be null");
        Objects.requireNonNull(pickupBins, "pickupBins must not be null");
        List<RouteLocation> currentRoute = new ArrayList<>(pickupBins.size());
        for (RouteLocation pickupBin : pickupBins) {
            currentRoute.add(Objects.requireNonNull(pickupBin, "pickupBins must not contain null elements"));
        }

        double currentDistance = distanceCalculator.calculateClosedRouteDistance(depot, currentRoute);

        if (currentRoute.size() < 2) {
            return new RouteOptimizationResult(
                    currentRoute,
                    0,
                    List.of(currentDistance),
                    List.of(currentDistance));
        }

        List<RouteLocation> bestRoute = new ArrayList<>(currentRoute);
        double bestDistance = currentDistance;
        double temperature = initialTemperature;

        List<Double> currentDistanceHistory = new ArrayList<>(maxIterations + 1);
        List<Double> bestDistanceHistory = new ArrayList<>(maxIterations + 1);
        currentDistanceHistory.add(currentDistance);
        bestDistanceHistory.add(bestDistance);

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            List<RouteLocation> candidateRoute = createSwappedNeighbor(currentRoute);
            double candidateDistance = distanceCalculator.calculateClosedRouteDistance(depot, candidateRoute);
            double delta = candidateDistance - currentDistance;

            if (shouldAccept(delta, temperature)) {
                currentRoute = candidateRoute;
                currentDistance = candidateDistance;
            }

            if (currentDistance < bestDistance) {
                bestRoute = new ArrayList<>(currentRoute);
                bestDistance = currentDistance;
            }

            currentDistanceHistory.add(currentDistance);
            bestDistanceHistory.add(bestDistance);
            temperature *= coolingRate;
        }

        return new RouteOptimizationResult(
                bestRoute,
                maxIterations,
                currentDistanceHistory,
                bestDistanceHistory);
    }

    private List<RouteLocation> createSwappedNeighbor(List<RouteLocation> route) {
        int size = route.size();
        int firstIndex = randomGenerator.nextInt(size);
        int secondIndex = randomGenerator.nextInt(size - 1);
        if (secondIndex >= firstIndex) {
            secondIndex++;
        }
        List<RouteLocation> neighbor = new ArrayList<>(route);
        RouteLocation firstStop = neighbor.get(firstIndex);
        neighbor.set(firstIndex, neighbor.get(secondIndex));
        neighbor.set(secondIndex, firstStop);
        return neighbor;
    }

    private boolean shouldAccept(double delta, double temperature) {
        if (delta <= 0.0) {
            return true;
        }
        if (temperature <= 0.0) {
            return false;
        }
        double probability = Math.exp(-delta / temperature);
        return randomGenerator.nextDouble() < probability;
    }
}