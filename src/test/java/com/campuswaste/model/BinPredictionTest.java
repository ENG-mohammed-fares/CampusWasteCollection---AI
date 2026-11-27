package com.campuswaste.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BinPredictionTest {

    private static WasteBin bin(String id) {
        return new WasteBin(id, 1.0, 2.0, 3.0);
    }

    private static RouteLocation location(String id) {
        return new RouteLocation(id, 10.0, -20.0);
    }

    @Test
    void storesValidPickupPrediction() {
        WasteBin bin = bin("B1");
        RouteLocation location = location("B1");

        BinPrediction prediction = new BinPrediction(bin, location, 1);

        assertSame(bin, prediction.bin());
        assertSame(location, prediction.location());
        assertEquals(1, prediction.predictedLabel());
    }

    @Test
    void storesValidNoPickupPrediction() {
        WasteBin bin = bin("B1");
        RouteLocation location = location("B1");

        BinPrediction prediction = new BinPrediction(bin, location, -1);

        assertSame(bin, prediction.bin());
        assertSame(location, prediction.location());
        assertEquals(-1, prediction.predictedLabel());
    }

    @Test
    void needsPickupReturnsTrueForPositiveLabel() {
        assertTrue(new BinPrediction(bin("B1"), location("B1"), 1).needsPickup());
    }

    @Test
    void needsPickupReturnsFalseForNegativeLabel() {
        assertFalse(new BinPrediction(bin("B1"), location("B1"), -1).needsPickup());
    }

    @Test
    void rejectsNullBin() {
        assertThrows(NullPointerException.class, () -> new BinPrediction(null, location("B1"), 1));
    }

    @Test
    void rejectsNullLocation() {
        assertThrows(NullPointerException.class, () -> new BinPrediction(bin("B1"), null, 1));
    }

    @Test
    void rejectsLabelsOtherThanPlusOneAndMinusOne() {
        int[] invalidLabels = {0, 2, -2, Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (int invalidLabel : invalidLabels) {
            assertThrows(IllegalArgumentException.class,
                    () -> new BinPrediction(bin("B1"), location("B1"), invalidLabel));
        }
    }

    @Test
    void rejectsBinAndLocationWithDifferentIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new BinPrediction(bin("B1"), location("B2"), 1));
    }

    @Test
    void acceptsMatchingTrimmedIds() {
        BinPrediction prediction = new BinPrediction(bin("  B1  "), location("B1 "), 1);

        assertEquals("B1", prediction.bin().id());
        assertEquals("B1", prediction.location().id());
    }

    @Test
    void doesNotModifySuppliedBinOrLocation() {
        WasteBin bin = bin("B1");
        RouteLocation location = location("B1");

        BinPrediction prediction = new BinPrediction(bin, location, 1);
        prediction.needsPickup();

        assertEquals(bin("B1"), bin);
        assertEquals(location("B1"), location);
        assertArrayEquals(new double[] {1.0, 2.0, 3.0}, bin.toInputVector(), 0.0);
        assertEquals(10.0, location.xMeters(), 0.0);
        assertEquals(-20.0, location.yMeters(), 0.0);
    }
}