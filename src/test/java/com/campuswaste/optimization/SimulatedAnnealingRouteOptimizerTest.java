package com.campuswaste.optimization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campuswaste.model.RouteLocation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SimulatedAnnealingRouteOptimizerTest {

    private static final double TOLERANCE = 1e-9;
    private static final RouteLocation DEPOT = new RouteLocation("DEPOT", 0.0, 0.0);

    private static List<RouteLocation> sampleBins() {
        List<RouteLocation> bins = new ArrayList<>();
        bins.add(new RouteLocation("B1", 40.0, 10.0));
        bins.add(new RouteLocation("B2", -20.0, 35.0));
        bins.add(new RouteLocation("B3", 15.0, -30.0));
        bins.add(new RouteLocation("B4", 60.0, 55.0));
        bins.add(new RouteLocation("B5", -45.0, -15.0));
        bins.add(new RouteLocation("B6", 5.0, 70.0));
        return bins;
    }

    private static SimulatedAnnealingRouteOptimizer seededOptimizer(long seed, int maxIterations) {
        return new SimulatedAnnealingRouteOptimizer(
                SimulatedAnnealingRouteOptimizer.DEFAULT_INITIAL_TEMPERATURE,
                SimulatedAnnealingRouteOptimizer.DEFAULT_COOLING_RATE,
                maxIterations,
                new Random(seed));
    }

    private static List<String> sortedIds(List<RouteLocation> locations) {
        List<String> ids = new ArrayList<>();
        for (RouteLocation location : locations) {
            ids.add(location.id());
        }
        Collections.sort(ids);
        return ids;
    }

    @Test
    void exposesAssignmentAndEngineeringDefaults() {
        assertEquals(60.0, SimulatedAnnealingRouteOptimizer.DEFAULT_INITIAL_TEMPERATURE, 0.0);
        assertEquals(0.997, SimulatedAnnealingRouteOptimizer.DEFAULT_COOLING_RATE, 0.0);
        assertEquals(10000, SimulatedAnnealingRouteOptimizer.DEFAULT_MAX_ITERATIONS);
    }

    @Test
    void defaultConstructorCanOptimizeValidRoute() {
        SimulatedAnnealingRouteOptimizer optimizer = new SimulatedAnnealingRouteOptimizer();

        RouteOptimizationResult result = assertDoesNotThrow(() -> optimizer.optimize(DEPOT, sampleBins()));

        assertNotNull(result);
        assertEquals(SimulatedAnnealingRouteOptimizer.DEFAULT_MAX_ITERATIONS, result.iterationsCompleted());
        assertEquals(sortedIds(sampleBins()), sortedIds(result.bestRoute()));
    }

    @Test
    void rejectsNonFiniteOrNonPositiveInitialTemperature() {
        double[] invalidTemperatures = {
                0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
        };
        for (double invalidTemperature : invalidTemperatures) {
            assertThrows(IllegalArgumentException.class,
                    () -> new SimulatedAnnealingRouteOptimizer(invalidTemperature, 0.997, 100, new Random(1)));
        }
    }

    @Test
    void rejectsCoolingRateEqualToZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulatedAnnealingRouteOptimizer(60.0, 0.0, 100, new Random(1)));
    }

    @Test
    void rejectsNegativeCoolingRate() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulatedAnnealingRouteOptimizer(60.0, -0.5, 100, new Random(1)));
    }

    @Test
    void rejectsCoolingRateEqualToOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulatedAnnealingRouteOptimizer(60.0, 1.0, 100, new Random(1)));
    }

    @Test
    void rejectsCoolingRateGreaterThanOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulatedAnnealingRouteOptimizer(60.0, 1.5, 100, new Random(1)));
    }

    @Test
    void rejectsNonFiniteCoolingRate() {
        double[] invalidRates = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double invalidRate : invalidRates) {
            assertThrows(IllegalArgumentException.class,
                    () -> new SimulatedAnnealingRouteOptimizer(60.0, invalidRate, 100, new Random(1)));
        }
    }

    @Test
    void rejectsZeroOrNegativeMaxIterations() {
        assertThrows(IllegalArgumentException.class,
                () -> new SimulatedAnnealingRouteOptimizer(60.0, 0.997, 0, new Random(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new SimulatedAnnealingRouteOptimizer(60.0, 0.997, -10, new Random(1)));
    }

    @Test
    void rejectsNullRandomGenerator() {
        assertThrows(NullPointerException.class,
                () -> new SimulatedAnnealingRouteOptimizer(60.0, 0.997, 100, null));
    }

    @Test
    void rejectsNullDepot() {
        SimulatedAnnealingRouteOptimizer optimizer = seededOptimizer(1L, 100);

        assertThrows(NullPointerException.class, () -> optimizer.optimize(null, sampleBins()));
    }

    @Test
    void rejectsNullPickupBinList() {
        SimulatedAnnealingRouteOptimizer optimizer = seededOptimizer(1L, 100);

        assertThrows(NullPointerException.class, () -> optimizer.optimize(DEPOT, null));
    }

    @Test
    void rejectsNullPickupBinElement() {
        SimulatedAnnealingRouteOptimizer optimizer = seededOptimizer(1L, 100);
        List<RouteLocation> binsWithNull = Arrays.asList(
                new RouteLocation("B1", 1.0, 1.0),
                null,
                new RouteLocation("B2", 2.0, 2.0));

        assertThrows(NullPointerException.class, () -> optimizer.optimize(DEPOT, binsWithNull));
    }

    @Test
    void rejectsDuplicatePickupBinIds() {
        SimulatedAnnealingRouteOptimizer optimizer = seededOptimizer(1L, 100);
        List<RouteLocation> duplicateBins = List.of(
                new RouteLocation("B1", 1.0, 1.0),
                new RouteLocation("B1", 2.0, 2.0));

        assertThrows(IllegalArgumentException.class, () -> optimizer.optimize(DEPOT, duplicateBins));
    }

    @Test
    void rejectsPickupBinIdEqualToDepotId() {
        SimulatedAnnealingRouteOptimizer optimizer = seededOptimizer(1L, 100);
        List<RouteLocation> bins = List.of(
                new RouteLocation("DEPOT", 5.0, 5.0),
                new RouteLocation("B2", 2.0, 2.0));

        assertThrows(IllegalArgumentException.class, () -> optimizer.optimize(DEPOT, bins));
    }

    @Test
    void emptyPickupBinListReturnsEmptyRouteAndZeroDistanceWithZeroIterations() {
        RouteOptimizationResult result = seededOptimizer(1L, 100).optimize(DEPOT, new ArrayList<>());

        assertTrue(result.bestRoute().isEmpty());
        assertEquals(0.0, result.bestDistanceMeters(), 0.0);
        assertEquals(0.0, result.finalCurrentDistanceMeters(), 0.0);
        assertEquals(0, result.iterationsCompleted());
        assertEquals(List.of(0.0), result.currentDistanceHistoryMeters());
        assertEquals(List.of(0.0), result.bestDistanceHistoryMeters());
    }

    @Test
    void singlePickupBinReturnsDepotToBinToDepotDistanceWithZeroIterations() {
        RouteLocation bin = new RouteLocation("B1", 3.0, 4.0);

        RouteOptimizationResult result = seededOptimizer(1L, 100).optimize(DEPOT, List.of(bin));

        assertEquals(List.of(bin), result.bestRoute());
        assertEquals(10.0, result.bestDistanceMeters(), TOLERANCE);
        assertEquals(10.0, result.finalCurrentDistanceMeters(), TOLERANCE);
        assertEquals(0, result.iterationsCompleted());
        assertEquals(1, result.currentDistanceHistoryMeters().size());
        assertEquals(1, result.bestDistanceHistoryMeters().size());
    }

    @Test
    void multiplePickupBinsProduceBestRouteContainingEveryInputIdExactlyOnce() {
        List<RouteLocation> bins = sampleBins();

        RouteOptimizationResult result = seededOptimizer(7L, 500).optimize(DEPOT, bins);

        assertEquals(bins.size(), result.bestRoute().size());
        assertEquals(sortedIds(bins), sortedIds(result.bestRoute()));
    }

    @Test
    void depotIsNotIncludedInBestRoute() {
        RouteOptimizationResult result = seededOptimizer(7L, 500).optimize(DEPOT, sampleBins());

        assertFalse(sortedIds(result.bestRoute()).contains(DEPOT.id()));
    }

    @Test
    void bestDistanceIsNeverGreaterThanInitialDistance() {
        List<RouteLocation> bins = sampleBins();
        double initialDistance = new RouteDistanceCalculator().calculateClosedRouteDistance(DEPOT, bins);

        RouteOptimizationResult result = seededOptimizer(11L, 500).optimize(DEPOT, bins);

        assertTrue(result.bestDistanceMeters() <= initialDistance);
        assertEquals(initialDistance, result.bestDistanceHistoryMeters().get(0), TOLERANCE);
        assertEquals(initialDistance, result.currentDistanceHistoryMeters().get(0), TOLERANCE);
    }

    @Test
    void historiesContainInitialEntryPlusOneEntryPerCompletedIteration() {
        RouteOptimizationResult result = seededOptimizer(3L, 300).optimize(DEPOT, sampleBins());

        assertEquals(300, result.iterationsCompleted());
        assertEquals(301, result.currentDistanceHistoryMeters().size());
        assertEquals(301, result.bestDistanceHistoryMeters().size());
    }

    @Test
    void bestDistanceHistoryIsNonIncreasing() {
        RouteOptimizationResult result = seededOptimizer(5L, 500).optimize(DEPOT, sampleBins());
        List<Double> bestHistory = result.bestDistanceHistoryMeters();

        for (int index = 1; index < bestHistory.size(); index++) {
            assertTrue(bestHistory.get(index) <= bestHistory.get(index - 1));
        }
    }

    @Test
    void bestDistanceDoesNotExceedCurrentDistanceAtEveryHistoryIndex() {
        RouteOptimizationResult result = seededOptimizer(5L, 500).optimize(DEPOT, sampleBins());
        List<Double> currentHistory = result.currentDistanceHistoryMeters();
        List<Double> bestHistory = result.bestDistanceHistoryMeters();

        for (int index = 0; index < currentHistory.size(); index++) {
            assertTrue(bestHistory.get(index) <= currentHistory.get(index));
        }
    }

    @Test
    void finalBestDistanceEqualsFinalBestHistoryEntry() {
        RouteOptimizationResult result = seededOptimizer(9L, 400).optimize(DEPOT, sampleBins());
        List<Double> bestHistory = result.bestDistanceHistoryMeters();

        assertEquals(bestHistory.get(bestHistory.size() - 1), result.bestDistanceMeters(), 0.0);
    }

    @Test
    void finalCurrentDistanceEqualsFinalCurrentHistoryEntry() {
        RouteOptimizationResult result = seededOptimizer(9L, 400).optimize(DEPOT, sampleBins());
        List<Double> currentHistory = result.currentDistanceHistoryMeters();

        assertEquals(currentHistory.get(currentHistory.size() - 1), result.finalCurrentDistanceMeters(), 0.0);
    }

    @Test
    void suppliedInputListRemainsUnchanged() {
        List<RouteLocation> bins = sampleBins();
        List<RouteLocation> snapshot = new ArrayList<>(bins);

        seededOptimizer(13L, 500).optimize(DEPOT, bins);

        assertEquals(snapshot, bins);
    }

    @Test
    void identicalConfigurationAndSeedProduceIdenticalResults() {
        SimulatedAnnealingRouteOptimizer firstOptimizer = seededOptimizer(2024L, 600);
        SimulatedAnnealingRouteOptimizer secondOptimizer = seededOptimizer(2024L, 600);

        RouteOptimizationResult firstResult = firstOptimizer.optimize(DEPOT, sampleBins());
        RouteOptimizationResult secondResult = secondOptimizer.optimize(DEPOT, sampleBins());

        assertEquals(firstResult, secondResult);
    }

    @Test
    void differentIdsAtIdenticalCoordinatesAreAccepted() {
        List<RouteLocation> bins = List.of(
                new RouteLocation("A", 5.0, 5.0),
                new RouteLocation("B", 5.0, 5.0),
                new RouteLocation("C", 1.0, 1.0));

        RouteOptimizationResult result = seededOptimizer(17L, 200).optimize(DEPOT, bins);

        assertEquals(List.of("A", "B", "C"), sortedIds(result.bestRoute()));
    }

    @Test
    void routeWithTwoOrMorePickupBinsCompletesConfiguredNumberOfIterations() {
        List<RouteLocation> twoBins = List.of(
                new RouteLocation("B1", 10.0, 0.0),
                new RouteLocation("B2", 0.0, 10.0));

        RouteOptimizationResult twoBinResult = seededOptimizer(19L, 250).optimize(DEPOT, twoBins);
        RouteOptimizationResult sixBinResult = seededOptimizer(19L, 250).optimize(DEPOT, sampleBins());

        assertEquals(250, twoBinResult.iterationsCompleted());
        assertEquals(250, sixBinResult.iterationsCompleted());
    }

    @Test
    void improvesCrossingRouteOnSquareLayout() {
        RouteLocation farCorner = new RouteLocation("A", 10.0, 10.0);
        RouteLocation rightCorner = new RouteLocation("B", 10.0, 0.0);
        RouteLocation topCorner = new RouteLocation("C", 0.0, 10.0);
        List<RouteLocation> crossingOrder = List.of(farCorner, rightCorner, topCorner);
        double initialDistance = new RouteDistanceCalculator().calculateClosedRouteDistance(DEPOT, crossingOrder);

        RouteOptimizationResult result = seededOptimizer(42L, 500).optimize(DEPOT, crossingOrder);

        assertTrue(result.bestDistanceMeters() < initialDistance);
        assertEquals(40.0, result.bestDistanceMeters(), TOLERANCE);
    }
}