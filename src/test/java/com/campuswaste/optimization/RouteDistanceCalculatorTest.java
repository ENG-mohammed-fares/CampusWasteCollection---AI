package com.campuswaste.optimization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.campuswaste.model.RouteLocation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteDistanceCalculatorTest {

    private static final double DELTA = 1e-9;

    private final RouteDistanceCalculator calculator = new RouteDistanceCalculator();

    private static RouteLocation depot() {
        return new RouteLocation("D", 0, 0);
    }

    private static RouteLocation binA() {
        return new RouteLocation("A", 0, 3);
    }

    private static RouteLocation binB() {
        return new RouteLocation("B", 4, 3);
    }

    private static RouteLocation binC() {
        return new RouteLocation("C", 4, 0);
    }

    @Test
    void nullDepotIsRejected() {
        assertThrows(NullPointerException.class,
                () -> calculator.calculateClosedRouteDistance(null, List.of(binA())));
    }

    @Test
    void nullPickupBinListIsRejected() {
        assertThrows(NullPointerException.class,
                () -> calculator.calculateClosedRouteDistance(depot(), null));
    }

    @Test
    void emptyPickupBinListReturnsExactlyZero() {
        assertEquals(0.0, calculator.calculateClosedRouteDistance(depot(), List.of()), 0.0);
    }

    @Test
    void singlePickupBinCalculatesRoundTrip() {
        RouteLocation bin = new RouteLocation("A", 3, 4);

        double distance = calculator.calculateClosedRouteDistance(depot(), List.of(bin));

        assertEquals(10.0, distance, DELTA);
    }

    @Test
    void multiplePickupBinsIncludeEverySegmentAndReturnToDepot() {
        double distance = calculator.calculateClosedRouteDistance(depot(), List.of(binA(), binB(), binC()));

        assertEquals(3.0 + 4.0 + 3.0 + 4.0, distance, DELTA);
    }

    @Test
    void suppliedOrderIsPreservedAndNotSorted() {
        double distance = calculator.calculateClosedRouteDistance(depot(), List.of(binC(), binA(), binB()));

        assertEquals(4.0 + 5.0 + 4.0 + 5.0, distance, DELTA);
    }

    @Test
    void changingOrderCanChangeDistance() {
        double rectangleOrder = calculator.calculateClosedRouteDistance(depot(), List.of(binA(), binB(), binC()));
        double crossingOrder = calculator.calculateClosedRouteDistance(depot(), List.of(binA(), binC(), binB()));

        assertEquals(14.0, rectangleOrder, DELTA);
        assertEquals(16.0, crossingOrder, DELTA);
    }

    @Test
    void differentIdsAtIdenticalCoordinatesProduceZeroLengthSegments() {
        List<RouteLocation> bins = List.of(new RouteLocation("A", 0, 0), new RouteLocation("B", 0, 0));

        assertEquals(0.0, calculator.calculateClosedRouteDistance(depot(), bins), 0.0);
    }

    @Test
    void consecutiveBinsAtSameCoordinatesAddOnlyTheRemainingSegments() {
        List<RouteLocation> bins = List.of(new RouteLocation("A", 3, 4), new RouteLocation("B", 3, 4));

        assertEquals(10.0, calculator.calculateClosedRouteDistance(depot(), bins), DELTA);
    }

    @Test
    void nullPickupBinElementIsRejected() {
        List<RouteLocation> bins = Arrays.asList(binA(), null);

        assertThrows(NullPointerException.class, () -> calculator.calculateClosedRouteDistance(depot(), bins));
    }

    @Test
    void duplicatePickupBinIdsAreRejected() {
        List<RouteLocation> bins = List.of(binA(), new RouteLocation("A", 9, 9));

        assertThrows(IllegalArgumentException.class, () -> calculator.calculateClosedRouteDistance(depot(), bins));
    }

    @Test
    void duplicateIdsAreDetectedAfterTrimming() {
        List<RouteLocation> bins = List.of(binA(), new RouteLocation("  A  ", 9, 9));

        assertThrows(IllegalArgumentException.class, () -> calculator.calculateClosedRouteDistance(depot(), bins));
    }

    @Test
    void pickupBinWithDepotIdIsRejected() {
        List<RouteLocation> bins = List.of(binA(), new RouteLocation(" D ", 5, 5));

        assertThrows(IllegalArgumentException.class, () -> calculator.calculateClosedRouteDistance(depot(), bins));
    }

    @Test
    void coordinateDifferenceOverflowIsRejected() {
        RouteLocation farDepot = new RouteLocation("D", -Double.MAX_VALUE, 0);
        List<RouteLocation> bins = List.of(new RouteLocation("A", Double.MAX_VALUE, 0));

        assertThrows(IllegalArgumentException.class, () -> calculator.calculateClosedRouteDistance(farDepot, bins));
    }

    @Test
    void segmentOverflowIsRejected() {
        List<RouteLocation> bins = List.of(new RouteLocation("A", Double.MAX_VALUE, Double.MAX_VALUE));

        assertThrows(IllegalArgumentException.class, () -> calculator.calculateClosedRouteDistance(depot(), bins));
    }

    @Test
    void totalOverflowIsRejected() {
        List<RouteLocation> bins = List.of(
                new RouteLocation("A", Double.MAX_VALUE, 0),
                new RouteLocation("B", 0, 0));

        assertThrows(IllegalArgumentException.class, () -> calculator.calculateClosedRouteDistance(depot(), bins));
    }

    @Test
    void callerListAndLocationsAreNotModified() {
        List<RouteLocation> bins = new ArrayList<>(List.of(binC(), binA(), binB()));
        List<RouteLocation> binsBefore = List.copyOf(bins);
        RouteLocation depot = depot();
        RouteLocation depotBefore = new RouteLocation(depot.id(), depot.xMeters(), depot.yMeters());

        calculator.calculateClosedRouteDistance(depot, bins);

        assertEquals(binsBefore, bins);
        assertEquals(depotBefore, depot);
    }

    @Test
    void repeatedCalculationsReturnExactlyTheSameResult() {
        List<RouteLocation> bins = List.of(
                new RouteLocation("A", 1.1, 2.2),
                new RouteLocation("B", -3.3, 4.4),
                new RouteLocation("C", 5.5, -6.6));

        double first = calculator.calculateClosedRouteDistance(depot(), bins);
        double second = calculator.calculateClosedRouteDistance(depot(), bins);

        assertEquals(first, second, 0.0);
    }
}