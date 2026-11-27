package com.campuswaste.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RouteLocationTest {

    private static final double DELTA = 0.0;

    @Test
    void validLocationStoresIdAndCoordinates() {
        RouteLocation location = new RouteLocation("BIN-1", 12.5, 40.25);

        assertEquals("BIN-1", location.id());
        assertEquals(12.5, location.xMeters(), DELTA);
        assertEquals(40.25, location.yMeters(), DELTA);
    }

    @Test
    void idIsTrimmed() {
        RouteLocation location = new RouteLocation("  \tDEPOT \n", 1, 2);

        assertEquals("DEPOT", location.id());
    }

    @Test
    void nullIdIsRejected() {
        assertThrows(NullPointerException.class, () -> new RouteLocation(null, 1, 2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", "\n", " \t \n "})
    void blankIdIsRejected(String blankId) {
        assertThrows(IllegalArgumentException.class, () -> new RouteLocation(blankId, 1, 2));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteXMetersIsRejected(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> new RouteLocation("L", invalid, 2));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteYMetersIsRejected(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> new RouteLocation("L", 1, invalid));
    }

    @Test
    void negativeCoordinatesAreAccepted() {
        RouteLocation location = new RouteLocation("L", -5.5, -10.25);

        assertEquals(-5.5, location.xMeters(), DELTA);
        assertEquals(-10.25, location.yMeters(), DELTA);
    }

    @Test
    void zeroCoordinatesAreAccepted() {
        RouteLocation location = new RouteLocation("L", 0.0, 0.0);

        assertEquals(0.0, location.xMeters(), DELTA);
        assertEquals(0.0, location.yMeters(), DELTA);
    }

    @Test
    void largeFiniteCoordinatesAreAcceptedWithoutRange() {
        RouteLocation location = new RouteLocation("L", Double.MAX_VALUE, -Double.MAX_VALUE);

        assertEquals(Double.MAX_VALUE, location.xMeters(), DELTA);
        assertEquals(-Double.MAX_VALUE, location.yMeters(), DELTA);
    }
}