package com.campuswaste.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PerceptronTrainingSampleTest {

    private static final double DELTA = 0.0;

    @Test
    void validSampleStoresReadingsAndLabel() {
        PerceptronTrainingSample sample = new PerceptronTrainingSample(1.5, 2.5, 3.5, -1);

        assertEquals(1.5, sample.reading1(), DELTA);
        assertEquals(2.5, sample.reading2(), DELTA);
        assertEquals(3.5, sample.reading3(), DELTA);
        assertEquals(-1, sample.pickupLabel());
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteReading1IsRejected(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> new PerceptronTrainingSample(invalid, 2, 3, 1));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteReading2IsRejected(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> new PerceptronTrainingSample(1, invalid, 3, 1));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteReading3IsRejected(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> new PerceptronTrainingSample(1, 2, invalid, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, -2, 100, Integer.MAX_VALUE, Integer.MIN_VALUE})
    void labelsOtherThanPlusOrMinusOneAreRejected(int invalidLabel) {
        assertThrows(IllegalArgumentException.class, () -> new PerceptronTrainingSample(1, 2, 3, invalidLabel));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, -1})
    void plusOneAndMinusOneLabelsAreAccepted(int label) {
        assertEquals(label, new PerceptronTrainingSample(1, 2, 3, label).pickupLabel());
    }

    @Test
    void finiteReadingsOutsideZeroToHundredAreAccepted() {
        PerceptronTrainingSample sample = new PerceptronTrainingSample(-250.75, 1_000_000.0, Double.MAX_VALUE, 1);

        assertEquals(-250.75, sample.reading1(), DELTA);
        assertEquals(1_000_000.0, sample.reading2(), DELTA);
        assertEquals(Double.MAX_VALUE, sample.reading3(), DELTA);
    }

    @Test
    void inputVectorContainsReadingsInRequiredOrder() {
        PerceptronTrainingSample sample = new PerceptronTrainingSample(10.0, 20.0, 30.0, 1);

        assertArrayEquals(new double[] {10.0, 20.0, 30.0}, sample.toInputVector(), DELTA);
    }

    @Test
    void inputVectorIsNewArrayOnEveryCall() {
        PerceptronTrainingSample sample = new PerceptronTrainingSample(10.0, 20.0, 30.0, 1);

        double[] first = sample.toInputVector();
        first[0] = 999.0;
        first[1] = 999.0;
        first[2] = 999.0;
        double[] second = sample.toInputVector();

        assertNotSame(first, second);
        assertArrayEquals(new double[] {10.0, 20.0, 30.0}, second, DELTA);
        assertEquals(10.0, sample.reading1(), DELTA);
    }
}