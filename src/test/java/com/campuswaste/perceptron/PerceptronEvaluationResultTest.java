package com.campuswaste.perceptron;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PerceptronEvaluationResultTest {

    private static final double DELTA = 1e-12;

    static Stream<Arguments> representativeMatrices() {
        return Stream.of(
                Arguments.of(3, 4, 2, 1),
                Arguments.of(1, 1, 1, 1),
                Arguments.of(10, 0, 0, 0),
                Arguments.of(0, 10, 0, 0),
                Arguments.of(0, 0, 5, 5),
                Arguments.of(0, 0, 0, 7),
                Arguments.of(0, 0, 7, 0),
                Arguments.of(5, 5, 0, 0),
                Arguments.of(1, 0, 99, 0),
                Arguments.of(0, 1, 0, 99));
    }

    private static PerceptronEvaluationResult sampleResult() {
        return new PerceptronEvaluationResult(10, 3, 4, 2, 1);
    }

    @Test
    void storesValidConfusionMatrixCounts() {
        PerceptronEvaluationResult result = sampleResult();

        assertEquals(10, result.totalSamples());
        assertEquals(3, result.truePositives());
        assertEquals(4, result.trueNegatives());
        assertEquals(2, result.falsePositives());
        assertEquals(1, result.falseNegatives());
    }

    @Test
    void zeroTotalSamplesIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PerceptronEvaluationResult(0, 0, 0, 0, 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -10, Integer.MIN_VALUE})
    void negativeTotalSamplesIsRejected(int total) {
        assertThrows(IllegalArgumentException.class, () -> new PerceptronEvaluationResult(total, 1, 1, 1, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void eachNegativeCountIsRejected(int position) {
        int[] counts = {2, 2, 2, 2};
        counts[position] = -1;
        int total = counts[0] + counts[1] + counts[2] + counts[3];

        assertThrows(IllegalArgumentException.class,
                () -> new PerceptronEvaluationResult(total, counts[0], counts[1], counts[2], counts[3]));
    }

    @Test
    void countsSummingToLessThanTotalAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PerceptronEvaluationResult(10, 3, 3, 2, 1));
    }

    @Test
    void countsSummingToMoreThanTotalAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PerceptronEvaluationResult(10, 3, 5, 2, 1));
    }

    @Test
    void countSumIsComputedWithoutIntegerOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> new PerceptronEvaluationResult(1, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 2));
    }

    @Test
    void correctPredictionsIsTruePositivesPlusTrueNegatives() {
        assertEquals(7, sampleResult().correctPredictions());
    }

    @Test
    void incorrectPredictionsIsFalsePositivesPlusFalseNegatives() {
        assertEquals(3, sampleResult().incorrectPredictions());
    }

    @Test
    void accuracyUsesSpecifiedFormula() {
        assertEquals(0.7, sampleResult().accuracy(), DELTA);
    }

    @Test
    void precisionUsesSpecifiedFormula() {
        assertEquals(0.6, sampleResult().precision(), DELTA);
    }

    @Test
    void recallUsesSpecifiedFormula() {
        assertEquals(0.75, sampleResult().recall(), DELTA);
    }

    @Test
    void f1ScoreUsesSpecifiedFormula() {
        assertEquals(2.0 / 3.0, sampleResult().f1Score(), DELTA);
    }

    @Test
    void metricsUseFloatingPointDivision() {
        PerceptronEvaluationResult result = new PerceptronEvaluationResult(3, 1, 0, 1, 1);

        assertEquals(1.0 / 3.0, result.accuracy(), DELTA);
        assertEquals(0.5, result.precision(), DELTA);
        assertEquals(0.5, result.recall(), DELTA);
    }

    @Test
    void precisionIsZeroWhenThereAreNoPredictedPositives() {
        PerceptronEvaluationResult result = new PerceptronEvaluationResult(5, 0, 2, 0, 3);

        assertEquals(0.0, result.precision(), DELTA);
    }

    @Test
    void recallIsZeroWhenThereAreNoActualPositives() {
        PerceptronEvaluationResult result = new PerceptronEvaluationResult(5, 0, 3, 2, 0);

        assertEquals(0.0, result.recall(), DELTA);
    }

    @Test
    void f1ScoreIsZeroWhenPrecisionAndRecallSumToZero() {
        PerceptronEvaluationResult result = new PerceptronEvaluationResult(3, 0, 1, 1, 1);

        assertEquals(0.0, result.precision(), DELTA);
        assertEquals(0.0, result.recall(), DELTA);
        assertEquals(0.0, result.f1Score(), DELTA);
    }

    @Test
    void allMetricsAreZeroWhenThereAreNoPositivesAtAll() {
        PerceptronEvaluationResult result = new PerceptronEvaluationResult(5, 0, 5, 0, 0);

        assertEquals(0.0, result.precision(), DELTA);
        assertEquals(0.0, result.recall(), DELTA);
        assertEquals(0.0, result.f1Score(), DELTA);
        assertEquals(1.0, result.accuracy(), DELTA);
    }

    @ParameterizedTest
    @MethodSource("representativeMatrices")
    void allMetricsAreFiniteAndWithinZeroAndOne(int tp, int tn, int fp, int fn) {
        PerceptronEvaluationResult result = new PerceptronEvaluationResult(tp + tn + fp + fn, tp, tn, fp, fn);

        for (double metric : new double[] {
                result.accuracy(), result.precision(), result.recall(), result.f1Score()}) {
            assertTrue(Double.isFinite(metric));
            assertTrue(metric >= 0.0 && metric <= 1.0);
        }
    }
}