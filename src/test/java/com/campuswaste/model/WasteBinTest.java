package com.campuswaste.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WasteBinTest {

    private static final double DELTA = 0.0;

    @Test
    void validBinStoresIdAndAllReadings() {
        WasteBin bin = new WasteBin("BIN-1", 1.5, 2.5, 3.5);

        assertEquals("BIN-1", bin.id());
        assertEquals(1.5, bin.reading1(), DELTA);
        assertEquals(2.5, bin.reading2(), DELTA);
        assertEquals(3.5, bin.reading3(), DELTA);
    }

    @Test
    void idIsTrimmed() {
        WasteBin bin = new WasteBin("  \tBIN-7 \n", 1, 2, 3);

        assertEquals("BIN-7", bin.id());
    }

    @Test
    void nullIdIsRejected() {
        assertThrows(NullPointerException.class, () -> new WasteBin(null, 1, 2, 3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", "\n", " \t \n "})
    void blankIdIsRejected(String blankId) {
        assertThrows(IllegalArgumentException.class, () -> new WasteBin(blankId, 1, 2, 3));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteReading1IsRejected(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> new WasteBin("B", invalid, 2, 3));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteReading2IsRejected(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> new WasteBin("B", 1, invalid, 3));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteReading3IsRejected(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> new WasteBin("B", 1, 2, invalid));
    }

    @Test
    void finiteReadingsOutsideZeroToHundredAreAccepted() {
        WasteBin bin = new WasteBin("B", -250.75, 1_000_000.0, Double.MAX_VALUE);

        assertEquals(-250.75, bin.reading1(), DELTA);
        assertEquals(1_000_000.0, bin.reading2(), DELTA);
        assertEquals(Double.MAX_VALUE, bin.reading3(), DELTA);
    }

    @Test
    void inputVectorContainsReadingsInRequiredOrder() {
        WasteBin bin = new WasteBin("B", 10.0, 20.0, 30.0);

        assertArrayEquals(new double[] {10.0, 20.0, 30.0}, bin.toInputVector(), DELTA);
    }

    @Test
    void inputVectorIsDefensiveCopyOnEachCall() {
        WasteBin bin = new WasteBin("B", 10.0, 20.0, 30.0);

        double[] first = bin.toInputVector();
        first[0] = 999.0;
        first[1] = 999.0;
        first[2] = 999.0;
        double[] second = bin.toInputVector();

        assertNotSame(first, second);
        assertArrayEquals(new double[] {10.0, 20.0, 30.0}, second, DELTA);
        assertEquals(10.0, bin.reading1(), DELTA);
    }
}