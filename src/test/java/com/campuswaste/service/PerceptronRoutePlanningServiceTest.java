package com.campuswaste.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campuswaste.model.BinPrediction;
import com.campuswaste.model.RouteLocation;
import com.campuswaste.model.RoutePlanningResult;
import com.campuswaste.model.WasteBin;
import com.campuswaste.optimization.SimulatedAnnealingRouteOptimizer;
import com.campuswaste.perceptron.PerceptronModel;
import com.campuswaste.perceptron.PerceptronTrainingResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PerceptronRoutePlanningServiceTest {

    private static final RouteLocation DEPOT = new RouteLocation("DEPOT", 0.0, 0.0);

    private static PerceptronTrainingResult trainingResult() {
        PerceptronModel model = new PerceptronModel(new double[] {1.0, 0.0, 0.0}, 0.0);
        return new PerceptronTrainingResult(model, 3, true);
    }

    private static SimulatedAnnealingRouteOptimizer optimizer(long seed) {
        return new SimulatedAnnealingRouteOptimizer(60.0, 0.997, 200, new Random(seed));
    }

    private static PerceptronRoutePlanningService service(long seed) {
        return new PerceptronRoutePlanningService(optimizer(seed));
    }

    private static WasteBin bin(String id, double reading1) {
        return new WasteBin(id, reading1, 0.5, 0.5);
    }

    private static RouteLocation location(String id, double x, double y) {
        return new RouteLocation(id, x, y);
    }

    private static Map<String, RouteLocation> locationsFor(List<WasteBin> bins) {
        Map<String, RouteLocation> locations = new LinkedHashMap<>();
        double offset = 10.0;
        for (WasteBin bin : bins) {
            locations.put(bin.id(), location(bin.id(), offset, offset * 2.0 - 7.0));
            offset += 13.0;
        }
        return locations;
    }

    private static List<String> sortedIds(List<RouteLocation> stops) {
        List<String> ids = new ArrayList<>();
        for (RouteLocation stop : stops) {
            ids.add(stop.id());
        }
        Collections.sort(ids);
        return ids;
    }

    private static List<WasteBin> mixedBins() {
        return List.of(
                bin("P1", 2.0),
                bin("N1", -2.0),
                bin("P2", 3.0),
                bin("N2", -1.0),
                bin("P3", 0.0));
    }

    @Test
    void constructorRejectsNullOptimizer() {
        assertThrows(NullPointerException.class, () -> new PerceptronRoutePlanningService(null));
    }

    @Test
    void planRouteRejectsNullTrainingResult() {
        List<WasteBin> bins = List.of(bin("P1", 1.0));

        assertThrows(NullPointerException.class,
                () -> service(1L).planRoute(null, DEPOT, bins, locationsFor(bins)));
    }

    @Test
    void planRouteRejectsNullDepot() {
        List<WasteBin> bins = List.of(bin("P1", 1.0));

        assertThrows(NullPointerException.class,
                () -> service(1L).planRoute(trainingResult(), null, bins, locationsFor(bins)));
    }

    @Test
    void planRouteRejectsNullBinsList() {
        assertThrows(NullPointerException.class,
                () -> service(1L).planRoute(trainingResult(), DEPOT, null, new HashMap<>()));
    }

    @Test
    void planRouteRejectsNullLocationMap() {
        List<WasteBin> bins = List.of(bin("P1", 1.0));

        assertThrows(NullPointerException.class,
                () -> service(1L).planRoute(trainingResult(), DEPOT, bins, null));
    }

    @Test
    void rejectsNullBinElement() {
        List<WasteBin> binsWithNull = Arrays.asList(bin("P1", 1.0), null);
        Map<String, RouteLocation> locations = locationsFor(List.of(bin("P1", 1.0)));

        assertThrows(NullPointerException.class,
                () -> service(1L).planRoute(trainingResult(), DEPOT, binsWithNull, locations));
    }

    @Test
    void rejectsDuplicateWasteBinIds() {
        List<WasteBin> bins = List.of(bin("P1", 1.0), bin("P1", 2.0));
        Map<String, RouteLocation> locations = locationsFor(List.of(bin("P1", 1.0)));

        assertThrows(IllegalArgumentException.class,
                () -> service(1L).planRoute(trainingResult(), DEPOT, bins, locations));
    }

    @Test
    void rejectsBinIdEqualToDepotId() {
        List<WasteBin> bins = List.of(bin("DEPOT", 1.0));
        Map<String, RouteLocation> locations = locationsFor(bins);

        assertThrows(IllegalArgumentException.class,
                () -> service(1L).planRoute(trainingResult(), DEPOT, bins, locations));
    }

    @Test
    void rejectsMissingLocationForBin() {
        List<WasteBin> bins = List.of(bin("P1", 1.0), bin("P2", 2.0));
        Map<String, RouteLocation> locations = locationsFor(List.of(bin("P1", 1.0)));

        assertThrows(IllegalArgumentException.class,
                () -> service(1L).planRoute(trainingResult(), DEPOT, bins, locations));
    }

    @Test
    void rejectsExtraLocationNotAssociatedWithSuppliedBin() {
        List<WasteBin> bins = List.of(bin("P1", 1.0));
        Map<String, RouteLocation> locations = locationsFor(List.of(bin("P1", 1.0), bin("EXTRA", 1.0)));

        assertThrows(IllegalArgumentException.class,
                () -> service(1L).planRoute(trainingResult(), DEPOT, bins, locations));
    }

    @Test
    void rejectsNullMapKeyOrValue() {
        List<WasteBin> bins = List.of(bin("P1", 1.0));
        Map<String, RouteLocation> nullKeyLocations = new HashMap<>();
        nullKeyLocations.put(null, location("P1", 1.0, 1.0));
        Map<String, RouteLocation> nullValueLocations = new HashMap<>();
        nullValueLocations.put("P1", null);

        assertThrows(NullPointerException.class,
                () -> service(1L).planRoute(trainingResult(), DEPOT, bins, nullKeyLocations));
        assertThrows(NullPointerException.class,
                () -> service(1L).planRoute(trainingResult(), DEPOT, bins, nullValueLocations));
    }

    @Test
    void rejectsMapKeyThatDoesNotEqualRouteLocationId() {
        List<WasteBin> bins = List.of(bin("P1", 1.0));
        Map<String, RouteLocation> locations = new HashMap<>();
        locations.put("P1", location("OTHER", 1.0, 1.0));

        assertThrows(IllegalArgumentException.class,
                () -> service(1L).planRoute(trainingResult(), DEPOT, bins, locations));
    }

    @Test
    void predictsPickupForPositiveActivation() {
        List<WasteBin> bins = List.of(bin("P1", 5.0));

        RoutePlanningResult result = service(1L).planRoute(trainingResult(), DEPOT, bins, locationsFor(bins));

        assertEquals(1, result.predictions().get(0).predictedLabel());
        assertTrue(result.predictions().get(0).needsPickup());
    }

    @Test
    void predictsNoPickupForNegativeActivation() {
        List<WasteBin> bins = List.of(bin("N1", -5.0));

        RoutePlanningResult result = service(1L).planRoute(trainingResult(), DEPOT, bins, locationsFor(bins));

        assertEquals(-1, result.predictions().get(0).predictedLabel());
        assertFalse(result.predictions().get(0).needsPickup());
    }

    @Test
    void sendsOnlyPickupBinsIntoOptimizedBestRoute() {
        List<WasteBin> bins = mixedBins();

        RoutePlanningResult result = service(5L).planRoute(trainingResult(), DEPOT, bins, locationsFor(bins));

        assertEquals(List.of("P1", "P2", "P3"), sortedIds(result.optimizationResult().bestRoute()));
    }

    @Test
    void keepsAllBinsIncludingNoPickupBinsInReturnedPredictions() {
        List<WasteBin> bins = mixedBins();

        RoutePlanningResult result = service(5L).planRoute(trainingResult(), DEPOT, bins, locationsFor(bins));

        assertEquals(5, result.predictions().size());
        assertEquals(3, result.pickupPredictions().size());
        assertEquals(2, result.noPickupPredictions().size());
    }

    @Test
    void preservesOriginalBinOrderInReturnedPredictions() {
        List<WasteBin> bins = mixedBins();

        RoutePlanningResult result = service(5L).planRoute(trainingResult(), DEPOT, bins, locationsFor(bins));

        List<String> predictedIds = new ArrayList<>();
        for (BinPrediction prediction : result.predictions()) {
            predictedIds.add(prediction.bin().id());
        }
        assertEquals(List.of("P1", "N1", "P2", "N2", "P3"), predictedIds);
    }

    @Test
    void usesMatchingLocationForEachBin() {
        List<WasteBin> bins = mixedBins();
        Map<String, RouteLocation> locations = locationsFor(bins);

        RoutePlanningResult result = service(5L).planRoute(trainingResult(), DEPOT, bins, locations);

        for (BinPrediction prediction : result.predictions()) {
            assertSame(locations.get(prediction.bin().id()), prediction.location());
        }
    }

    @Test
    void preservesSameIdsExactlyOnceInBestRouteWhenMultipleBinsArePickup() {
        List<WasteBin> bins = List.of(
                bin("P1", 1.0),
                bin("P2", 2.0),
                bin("P3", 3.0),
                bin("P4", 4.0),
                bin("P5", 5.0),
                bin("N1", -1.0));

        RoutePlanningResult result = service(8L).planRoute(trainingResult(), DEPOT, bins, locationsFor(bins));

        List<RouteLocation> bestRoute = result.optimizationResult().bestRoute();
        assertEquals(5, bestRoute.size());
        assertEquals(List.of("P1", "P2", "P3", "P4", "P5"), sortedIds(bestRoute));
    }

    @Test
    void emptyBinListReturnsEmptyPredictionsAndEmptyRoute() {
        RoutePlanningResult result = service(1L).planRoute(
                trainingResult(), DEPOT, new ArrayList<>(), new HashMap<>());

        assertTrue(result.predictions().isEmpty());
        assertTrue(result.optimizationResult().bestRoute().isEmpty());
        assertEquals(0.0, result.optimizationResult().bestDistanceMeters(), 0.0);
    }

    @Test
    void everyBinNoPickupYieldsEmptyRouteWithZeroDistance() {
        List<WasteBin> bins = List.of(bin("N1", -1.0), bin("N2", -4.0));

        RoutePlanningResult result = service(1L).planRoute(trainingResult(), DEPOT, bins, locationsFor(bins));

        assertEquals(2, result.predictions().size());
        assertEquals(2, result.noPickupPredictions().size());
        assertTrue(result.optimizationResult().bestRoute().isEmpty());
        assertEquals(0.0, result.optimizationResult().bestDistanceMeters(), 0.0);
    }

    @Test
    void singlePickupBinRouteContainsThatBinAndReturnsToDepot() {
        List<WasteBin> bins = List.of(bin("N1", -1.0), bin("P1", 2.0));
        Map<String, RouteLocation> locations = new LinkedHashMap<>();
        locations.put("N1", location("N1", 100.0, 100.0));
        locations.put("P1", location("P1", 3.0, 4.0));

        RoutePlanningResult result = service(1L).planRoute(trainingResult(), DEPOT, bins, locations);

        assertEquals(List.of(locations.get("P1")), result.optimizationResult().bestRoute());
        assertEquals(10.0, result.optimizationResult().bestDistanceMeters(), 1e-9);
        assertEquals(0, result.optimizationResult().iterationsCompleted());
    }

    @Test
    void doesNotMutateCallerBinsList() {
        List<WasteBin> bins = new ArrayList<>(mixedBins());
        List<WasteBin> snapshot = new ArrayList<>(bins);

        service(5L).planRoute(trainingResult(), DEPOT, bins, locationsFor(bins));

        assertEquals(snapshot, bins);
    }

    @Test
    void doesNotMutateCallerLocationMap() {
        List<WasteBin> bins = mixedBins();
        Map<String, RouteLocation> locations = locationsFor(bins);
        Map<String, RouteLocation> snapshot = new LinkedHashMap<>(locations);

        service(5L).planRoute(trainingResult(), DEPOT, bins, locations);

        assertEquals(snapshot, locations);
        assertEquals(new ArrayList<>(snapshot.keySet()), new ArrayList<>(locations.keySet()));
    }

    @Test
    void equivalentInputsWithSameSeedProduceIdenticalResults() {
        List<WasteBin> firstBins = mixedBins();
        List<WasteBin> secondBins = mixedBins();

        RoutePlanningResult firstResult = service(2024L).planRoute(
                trainingResult(), DEPOT, firstBins, locationsFor(firstBins));
        RoutePlanningResult secondResult = service(2024L).planRoute(
                trainingResult(), DEPOT, secondBins, locationsFor(secondBins));

        assertEquals(firstResult, secondResult);
    }

    @Test
    void usesSuppliedModelEvenWhenTrainingResultIsNotConverged() {
        PerceptronModel model = new PerceptronModel(new double[] {1.0, 0.0, 0.0}, 0.0);
        PerceptronTrainingResult notConverged = new PerceptronTrainingResult(model, 10, false);
        List<WasteBin> bins = List.of(bin("P1", 2.0), bin("N1", -2.0));

        RoutePlanningResult result = service(1L).planRoute(notConverged, DEPOT, bins, locationsFor(bins));

        assertEquals(1, result.predictions().get(0).predictedLabel());
        assertEquals(-1, result.predictions().get(1).predictedLabel());
        assertEquals(List.of("P1"), sortedIds(result.optimizationResult().bestRoute()));
    }
}