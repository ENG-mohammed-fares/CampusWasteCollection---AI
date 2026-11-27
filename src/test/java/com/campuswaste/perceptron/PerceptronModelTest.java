package com.campuswaste.perceptron;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PerceptronModelTest {

    private static final double DELTA = 0.0;

    static Stream<Arguments> nonFiniteValuesAtEveryPosition() {
        List<Arguments> cases = new ArrayList<>();
        for (int position = 0; position < 3; position++) {
            for (double value : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
                cases.add(Arguments.of(position, value));
            }
        }
        return cases.stream();
    }

    private static PerceptronModel sampleModel() {
        return new PerceptronModel(new double[] {1.0, 2.0, 3.0}, -4.0);
    }

    @Test
    void constructorStoresWeightsAndBias() {
        PerceptronModel model = new PerceptronModel(new double[] {1.5, -2.5, 3.5}, 0.25);

        assertArrayEquals(new double[] {1.5, -2.5, 3.5}, model.weights(), DELTA);
        assertEquals(0.25, model.bias(), DELTA);
    }

    @Test
    void nullWeightsAreRejected() {
        assertThrows(NullPointerException.class, () -> new PerceptronModel(null, 0.0));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 4, 5})
    void weightArraysWithWrongLengthAreRejected(int length) {
        assertThrows(IllegalArgumentException.class, () -> new PerceptronModel(new double[length], 0.0));
    }

    @ParameterizedTest
    @MethodSource("nonFiniteValuesAtEveryPosition")
    void nonFiniteWeightsAreRejectedInEveryPosition(int position, double value) {
        double[] weights = {1.0, 2.0, 3.0};
        weights[position] = value;

        assertThrows(IllegalArgumentException.class, () -> new PerceptronModel(weights, 0.0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonFiniteBiasIsRejected(double bias) {
        assertThrows(IllegalArgumentException.class, () -> new PerceptronModel(new double[] {1, 2, 3}, bias));
    }

    @Test
    void changingConstructorArrayAfterConstructionDoesNotAffectModel() {
        double[] weights = {1.0, 2.0, 3.0};
        PerceptronModel model = new PerceptronModel(weights, 0.0);

        weights[0] = 99.0;

        assertArrayEquals(new double[] {1.0, 2.0, 3.0}, model.weights(), DELTA);
    }

    @Test
    void weightsReturnsDefensiveCopyOnEveryCall() {
        PerceptronModel model = sampleModel();

        double[] first = model.weights();
        first[0] = 99.0;
        first[1] = 99.0;
        first[2] = 99.0;
        double[] second = model.weights();

        assertNotSame(first, second);
        assertArrayEquals(new double[] {1.0, 2.0, 3.0}, second, DELTA);
    }

    @Test
    void predictsPlusOneForPositiveActivation() {
        assertEquals(1, sampleModel().predict(new double[] {1.0, 1.0, 1.0}));
    }

    @Test
    void predictsMinusOneForNegativeActivation() {
        assertEquals(-1, sampleModel().predict(new double[] {0.0, 0.0, 0.0}));
    }

    @Test
    void predictsPlusOneWhenActivationIsExactlyZero() {
        assertEquals(1, sampleModel().predict(new double[] {1.0, 0.0, 1.0}));
        assertEquals(1, new PerceptronModel(new double[3], 0.0).predict(new double[] {5.0, -5.0, 5.0}));
    }

    @Test
    void nullInputVectorIsRejected() {
        assertThrows(NullPointerException.class, () -> sampleModel().predict(null));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 4, 5})
    void inputVectorsWithWrongLengthAreRejected(int length) {
        assertThrows(IllegalArgumentException.class, () -> sampleModel().predict(new double[length]));
    }

    @ParameterizedTest
    @MethodSource("nonFiniteValuesAtEveryPosition")
    void nonFiniteInputsAreRejectedInEveryPosition(int position, double value) {
        double[] inputs = {1.0, 2.0, 3.0};
        inputs[position] = value;

        assertThrows(IllegalArgumentException.class, () -> sampleModel().predict(inputs));
    }

    @Test
    void nonFiniteActivationFromOverflowIsRejected() {
        PerceptronModel model = new PerceptronModel(new double[] {Double.MAX_VALUE, 0.0, 0.0}, 0.0);

        assertThrows(IllegalArgumentException.class,
                () -> model.predict(new double[] {Double.MAX_VALUE, 0.0, 0.0}));
    }
}