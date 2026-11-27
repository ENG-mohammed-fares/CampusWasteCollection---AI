package com.campuswaste.optimization;

import com.campuswaste.model.RouteLocation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RouteDistanceCalculator {

    public double calculateClosedRouteDistance(RouteLocation depot, List<RouteLocation> orderedPickupBins) {
        Objects.requireNonNull(depot, "depot must not be null");
        Objects.requireNonNull(orderedPickupBins, "orderedPickupBins must not be null");
        requireUniqueIds(depot, orderedPickupBins);
        if (orderedPickupBins.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        RouteLocation previous = depot;
        for (RouteLocation stop : orderedPickupBins) {
            total = addSegment(total, previous, stop);
            previous = stop;
        }
        return addSegment(total, previous, depot);
    }

    private static void requireUniqueIds(RouteLocation depot, List<RouteLocation> orderedPickupBins) {
        Set<String> seenIds = new HashSet<>();
        seenIds.add(depot.id());
        for (RouteLocation stop : orderedPickupBins) {
            Objects.requireNonNull(stop, "orderedPickupBins must not contain null");
            if (!seenIds.add(stop.id())) {
                throw new IllegalArgumentException("duplicate route location id: " + stop.id());
            }
        }
    }

    private static double addSegment(double total, RouteLocation from, RouteLocation to) {
        double deltaX = to.xMeters() - from.xMeters();
        double deltaY = to.yMeters() - from.yMeters();
        if (!Double.isFinite(deltaX) || !Double.isFinite(deltaY)) {
            throw new IllegalArgumentException(
                    "coordinate difference is not finite between " + from.id() + " and " + to.id());
        }
        double segment = Math.hypot(deltaX, deltaY);
        if (!Double.isFinite(segment)) {
            throw new IllegalArgumentException(
                    "segment distance is not finite between " + from.id() + " and " + to.id());
        }
        double newTotal = total + segment;
        if (!Double.isFinite(newTotal)) {
            throw new IllegalArgumentException(
                    "total route distance is not finite after segment " + from.id() + " to " + to.id());
        }
        return newTotal;
    }
}