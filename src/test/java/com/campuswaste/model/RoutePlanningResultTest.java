package com.campuswaste.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campuswaste.optimization.RouteOptimizationResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoutePlanningResultTest {

    private static BinPrediction prediction(String id, int label) {
        return new BinPrediction(
                new WasteBin(id, 1.0, 2.0, 3.0),
                new RouteLocation(id, 5.0, 5.0),
                label);
    }

    private static RouteOptimizationResult optimization(List<RouteLocation> route) {
        return new RouteOptimizationResult(route, 0, List.of(5.0), List.of(5.0));
    }

    @Test
    void storesValidPredictionsAndOptimizationResult() {
        BinPrediction pickup = prediction("P1", 1);
        BinPrediction noPickup = prediction("N1", -1);
        RouteOptimizationResult optimization = optimization(List.of(pickup.location()));

        RoutePlanningResult result = new RoutePlanningResult(List.of(pickup, noPickup), optimization);

        assertEquals(List.of(pickup, noPickup), result.predictions());
        assertSame(optimization, result.optimizationResult());
    }

    @Test
    void defensivelyCopiesPredictionsList() {
        BinPrediction pickup = prediction("P1", 1);
        List<BinPrediction> mutablePredictions = new ArrayList<>(List.of(pickup));

        RoutePlanningResult result = new RoutePlanningResult(
                mutablePredictions, optimization(List.of(pickup.location())));
        mutablePredictions.clear();

        assertEquals(List.of(pickup), result.predictions());
        assertThrows(UnsupportedOperationException.class,
                () -> result.predictions().add(prediction("P2", 1)));
    }

    @Test
    void rejectsNullPredictionsList() {
        assertThrows(NullPointerException.class,
                () -> new RoutePlanningResult(null, optimization(List.of())));
    }

    @Test
    void rejectsNullOptimizationResult() {
        assertThrows(NullPointerException.class,
                () -> new RoutePlanningResult(List.of(prediction("N1", -1)), null));
    }

    @Test
    void rejectsNullPredictionElements() {
        List<BinPrediction> predictionsWithNull = Arrays.asList(prediction("N1", -1), null);

        assertThrows(NullPointerException.class,
                () -> new RoutePlanningResult(predictionsWithNull, optimization(List.of())));
    }

    @Test
    void rejectsDuplicatePredictionBinIds() {
        List<BinPrediction> duplicatePredictions = List.of(prediction("N1", -1), prediction("N1", -1));

        assertThrows(IllegalArgumentException.class,
                () -> new RoutePlanningResult(duplicatePredictions, optimization(List.of())));
    }

    @Test
    void rejectsOptimizedRouteContainingNoPickupBin() {
        BinPrediction noPickup = prediction("N1", -1);

        assertThrows(IllegalArgumentException.class,
                () -> new RoutePlanningResult(
                        List.of(noPickup), optimization(List.of(noPickup.location()))));
    }

    @Test
    void rejectsOptimizedRouteMissingPickupBin() {
        BinPrediction firstPickup = prediction("P1", 1);
        BinPrediction secondPickup = prediction("P2", 1);

        assertThrows(IllegalArgumentException.class,
                () -> new RoutePlanningResult(
                        List.of(firstPickup, secondPickup), optimization(List.of(firstPickup.location()))));
    }

    @Test
    void rejectsDuplicateIdsInOptimizedBestRoute() {
        BinPrediction pickup = prediction("P1", 1);

        assertThrows(IllegalArgumentException.class,
                () -> new RoutePlanningResult(
                        List.of(pickup), optimization(List.of(pickup.location(), pickup.location()))));
    }

    @Test
    void acceptsEmptyPredictionListWithEmptyOptimizationResult() {
        RoutePlanningResult result = new RoutePlanningResult(List.of(), optimization(List.of()));

        assertTrue(result.predictions().isEmpty());
        assertTrue(result.optimizationResult().bestRoute().isEmpty());
        assertTrue(result.pickupPredictions().isEmpty());
        assertTrue(result.noPickupPredictions().isEmpty());
    }

    @Test
    void pickupPredictionsReturnsOnlyPickupPredictionsInOriginalOrder() {
        BinPrediction firstPickup = prediction("P1", 1);
        BinPrediction firstNoPickup = prediction("N1", -1);
        BinPrediction secondPickup = prediction("P2", 1);
        BinPrediction secondNoPickup = prediction("N2", -1);

        RoutePlanningResult result = new RoutePlanningResult(
                List.of(firstPickup, firstNoPickup, secondPickup, secondNoPickup),
                optimization(List.of(secondPickup.location(), firstPickup.location())));

        assertEquals(List.of(firstPickup, secondPickup), result.pickupPredictions());
    }

    @Test
    void noPickupPredictionsReturnsOnlyNoPickupPredictionsInOriginalOrder() {
        BinPrediction firstPickup = prediction("P1", 1);
        BinPrediction firstNoPickup = prediction("N1", -1);
        BinPrediction secondPickup = prediction("P2", 1);
        BinPrediction secondNoPickup = prediction("N2", -1);

        RoutePlanningResult result = new RoutePlanningResult(
                List.of(firstPickup, firstNoPickup, secondPickup, secondNoPickup),
                optimization(List.of(firstPickup.location(), secondPickup.location())));

        assertEquals(List.of(firstNoPickup, secondNoPickup), result.noPickupPredictions());
    }

    @Test
    void returnedFilteredListsAreImmutable() {
        BinPrediction pickup = prediction("P1", 1);
        BinPrediction noPickup = prediction("N1", -1);
        RoutePlanningResult result = new RoutePlanningResult(
                List.of(pickup, noPickup), optimization(List.of(pickup.location())));

        assertThrows(UnsupportedOperationException.class,
                () -> result.pickupPredictions().add(prediction("P2", 1)));
        assertThrows(UnsupportedOperationException.class,
                () -> result.noPickupPredictions().add(prediction("N2", -1)));
    }
}