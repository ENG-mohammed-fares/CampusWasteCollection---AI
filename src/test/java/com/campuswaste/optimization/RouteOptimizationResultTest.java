package com.campuswaste.optimization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.campuswaste.model.RouteLocation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteOptimizationResultTest {

    private static final RouteLocation FIRST = new RouteLocation("A", 1.0, 1.0);
    private static final RouteLocation SECOND = new RouteLocation("B", 2.0, 2.0);

    private static List<RouteLocation> route() {
        return List.of(FIRST, SECOND);
    }

    private static List<Double> currentHistory() {
        return List.of(10.0, 8.0, 9.0);
    }

    private static List<Double> bestHistory() {
        return List.of(10.0, 8.0, 8.0);
    }

    @Test
    void storesValidBestRouteIterationCountAndHistories() {
        RouteOptimizationResult result = new RouteOptimizationResult(route(), 2, currentHistory(), bestHistory());

        assertEquals(route(), result.bestRoute());
        assertEquals(2, result.iterationsCompleted());
        assertEquals(currentHistory(), result.currentDistanceHistoryMeters());
        assertEquals(bestHistory(), result.bestDistanceHistoryMeters());
    }

    @Test
    void bestDistanceMetersReturnsLastBestHistoryValue() {
        RouteOptimizationResult result = new RouteOptimizationResult(route(), 2, currentHistory(), bestHistory());

        assertEquals(8.0, result.bestDistanceMeters(), 1e-12);
    }

    @Test
    void finalCurrentDistanceMetersReturnsLastCurrentHistoryValue() {
        RouteOptimizationResult result = new RouteOptimizationResult(route(), 2, currentHistory(), bestHistory());

        assertEquals(9.0, result.finalCurrentDistanceMeters(), 1e-12);
    }

    @Test
    void defensivelyCopiesRouteAndHistories() {
        List<RouteLocation> mutableRoute = new ArrayList<>(route());
        List<Double> mutableCurrent = new ArrayList<>(currentHistory());
        List<Double> mutableBest = new ArrayList<>(bestHistory());

        RouteOptimizationResult result = new RouteOptimizationResult(mutableRoute, 2, mutableCurrent, mutableBest);

        mutableRoute.clear();
        mutableCurrent.clear();
        mutableBest.clear();

        assertEquals(route(), result.bestRoute());
        assertEquals(currentHistory(), result.currentDistanceHistoryMeters());
        assertEquals(bestHistory(), result.bestDistanceHistoryMeters());
        assertThrows(UnsupportedOperationException.class, () -> result.bestRoute().add(FIRST));
        assertThrows(UnsupportedOperationException.class, () -> result.currentDistanceHistoryMeters().add(1.0));
        assertThrows(UnsupportedOperationException.class, () -> result.bestDistanceHistoryMeters().add(1.0));
    }

    @Test
    void rejectsNullListArguments() {
        assertThrows(NullPointerException.class,
                () -> new RouteOptimizationResult(null, 2, currentHistory(), bestHistory()));
        assertThrows(NullPointerException.class,
                () -> new RouteOptimizationResult(route(), 2, null, bestHistory()));
        assertThrows(NullPointerException.class,
                () -> new RouteOptimizationResult(route(), 2, currentHistory(), null));
    }

    @Test
    void rejectsNullBestRouteElements() {
        List<RouteLocation> routeWithNull = Arrays.asList(FIRST, null);

        assertThrows(NullPointerException.class,
                () -> new RouteOptimizationResult(routeWithNull, 2, currentHistory(), bestHistory()));
    }

    @Test
    void rejectsNegativeIterationsCompleted() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteOptimizationResult(route(), -1, currentHistory(), bestHistory()));
    }

    @Test
    void rejectsEmptyHistories() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteOptimizationResult(route(), 0, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new RouteOptimizationResult(route(), 0, List.of(), List.of(1.0)));
        assertThrows(IllegalArgumentException.class,
                () -> new RouteOptimizationResult(route(), 0, List.of(1.0), List.of()));
    }

    @Test
    void rejectsHistoryLengthsInconsistentWithIterationsCompleted() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteOptimizationResult(route(), 2, List.of(10.0, 8.0), List.of(10.0, 8.0)));
        assertThrows(IllegalArgumentException.class,
                () -> new RouteOptimizationResult(route(), 1, currentHistory(), bestHistory()));
        assertThrows(IllegalArgumentException.class,
                () -> new RouteOptimizationResult(route(), 2, currentHistory(), List.of(10.0, 8.0)));
    }

    @Test
    void rejectsNullHistoryValues() {
        List<Double> currentWithNull = Arrays.asList(10.0, null, 9.0);
        List<Double> bestWithNull = Arrays.asList(10.0, null, 8.0);

        assertThrows(NullPointerException.class,
                () -> new RouteOptimizationResult(route(), 2, currentWithNull, bestHistory()));
        assertThrows(NullPointerException.class,
                () -> new RouteOptimizationResult(route(), 2, currentHistory(), bestWithNull));
    }

    @Test
    void rejectsNonFiniteHistoryValues() {
        double[] invalidValues = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double invalidValue : invalidValues) {
            List<Double> invalidCurrent = List.of(10.0, invalidValue, 9.0);
            List<Double> invalidBest = List.of(10.0, invalidValue, 8.0);
            assertThrows(IllegalArgumentException.class,
                    () -> new RouteOptimizationResult(route(), 2, invalidCurrent, bestHistory()));
            assertThrows(IllegalArgumentException.class,
                    () -> new RouteOptimizationResult(route(), 2, currentHistory(), invalidBest));
        }
    }

    @Test
    void rejectsNegativeDistances() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteOptimizationResult(route(), 2, List.of(10.0, 8.0, -9.0), bestHistory()));
        assertThrows(IllegalArgumentException.class,
                () -> new RouteOptimizationResult(route(), 2, currentHistory(), List.of(10.0, 8.0, -8.0)));
    }

    @Test
    void rejectsDifferentInitialCurrentAndBestDistances() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteOptimizationResult(route(), 2, currentHistory(), List.of(9.0, 8.0, 8.0)));
    }

    @Test
    void rejectsBestDistanceGreaterThanCurrentDistance() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteOptimizationResult(route(), 2, currentHistory(), List.of(10.0, 9.0, 9.0)));
    }

    @Test
    void rejectsIncreasingBestDistanceHistory() {
        assertThrows(IllegalArgumentException.class,
                () -> new RouteOptimizationResult(
                        route(),
                        2,
                        List.of(10.0, 12.0, 13.0),
                        List.of(10.0, 11.0, 12.0)));
    }
}