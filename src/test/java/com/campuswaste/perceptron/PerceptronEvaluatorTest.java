package com.campuswaste.perceptron;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campuswaste.model.PerceptronDataset;
import com.campuswaste.model.PerceptronTrainingSample;
import java.util.List;
import org.junit.jupiter.api.Test;

class PerceptronEvaluatorTest {

    private static final double DELTA = 1e-12;
    private static final List<String> COLUMNS = List.of("a", "b", "c");

    private final PerceptronEvaluator evaluator = new PerceptronEvaluator();

    private static PerceptronDataset datasetOf(PerceptronTrainingSample... samples) {
        return new PerceptronDataset(COLUMNS, List.of(samples));
    }

    private static PerceptronDataset datasetWithColumns(List<String> columns) {
        return new PerceptronDataset(columns, List.of(new PerceptronTrainingSample(1, 1, 1, 1)));
    }

    private static PerceptronModel firstReadingModel() {
        return new PerceptronModel(new double[] {1.0, 0.0, 0.0}, 0.0);
    }

    private static PerceptronModel alwaysPositiveModel() {
        return new PerceptronModel(new double[] {0.0, 0.0, 0.0}, 0.0);
    }

    private static PerceptronModel alwaysNegativeModel() {
        return new PerceptronModel(new double[] {0.0, 0.0, 0.0}, -1.0);
    }

    private static PerceptronDataset oneOfEachTestDataset() {
        return datasetOf(
                new PerceptronTrainingSample(1, 0, 0, 1),
                new PerceptronTrainingSample(-1, 0, 0, -1),
                new PerceptronTrainingSample(2, 0, 0, -1),
                new PerceptronTrainingSample(-2, 0, 0, 1));
    }

    private static PerceptronDataset anyTrainingDataset() {
        return datasetOf(new PerceptronTrainingSample(5, 5, 5, 1));
    }

    @Test
    void nullModelIsRejected() {
        assertThrows(NullPointerException.class,
                () -> evaluator.evaluate(null, anyTrainingDataset(), oneOfEachTestDataset()));
    }

    @Test
    void nullTrainingDatasetIsRejected() {
        assertThrows(NullPointerException.class,
                () -> evaluator.evaluate(firstReadingModel(), null, oneOfEachTestDataset()));
    }

    @Test
    void nullTestDatasetIsRejected() {
        assertThrows(NullPointerException.class,
                () -> evaluator.evaluate(firstReadingModel(), anyTrainingDataset(), null));
    }

    @Test
    void differentFeatureColumnNamesAreRejected() {
        PerceptronDataset training = datasetWithColumns(List.of("a", "b", "c"));
        PerceptronDataset test = datasetWithColumns(List.of("a", "b", "x"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(firstReadingModel(), training, test));

        assertTrue(exception.getMessage().contains("feature columns"));
    }

    @Test
    void differentFeatureColumnOrderIsRejected() {
        PerceptronDataset training = datasetWithColumns(List.of("a", "b", "c"));
        PerceptronDataset test = datasetWithColumns(List.of("c", "b", "a"));

        assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(firstReadingModel(), training, test));
    }

    @Test
    void evaluatesOneOfEachConfusionMatrixCategory() {
        PerceptronEvaluationResult result =
                evaluator.evaluate(firstReadingModel(), anyTrainingDataset(), oneOfEachTestDataset());

        assertEquals(4, result.totalSamples());
        assertEquals(1, result.truePositives());
        assertEquals(1, result.trueNegatives());
        assertEquals(1, result.falsePositives());
        assertEquals(1, result.falseNegatives());
    }

    @Test
    void calculatesAllMetricsForOneOfEachDataset() {
        PerceptronEvaluationResult result =
                evaluator.evaluate(firstReadingModel(), anyTrainingDataset(), oneOfEachTestDataset());

        assertEquals(2, result.correctPredictions());
        assertEquals(2, result.incorrectPredictions());
        assertEquals(0.5, result.accuracy(), DELTA);
        assertEquals(0.5, result.precision(), DELTA);
        assertEquals(0.5, result.recall(), DELTA);
        assertEquals(0.5, result.f1Score(), DELTA);
    }

    @Test
    void usesTestSamplesNotTrainingSamples() {
        PerceptronDataset training = datasetOf(
                new PerceptronTrainingSample(1, 0, 0, 1),
                new PerceptronTrainingSample(1, 0, 0, 1),
                new PerceptronTrainingSample(1, 0, 0, 1),
                new PerceptronTrainingSample(1, 0, 0, 1),
                new PerceptronTrainingSample(1, 0, 0, 1),
                new PerceptronTrainingSample(1, 0, 0, 1));
        PerceptronDataset test = datasetOf(
                new PerceptronTrainingSample(-1, 0, 0, 1),
                new PerceptronTrainingSample(-1, 0, 0, 1));

        PerceptronEvaluationResult result = evaluator.evaluate(firstReadingModel(), training, test);

        assertEquals(2, result.totalSamples());
        assertEquals(0, result.truePositives());
        assertEquals(2, result.falseNegatives());
        assertEquals(0.0, result.accuracy(), DELTA);
    }

    @Test
    void worksWhenAllPredictionsArePositive() {
        PerceptronDataset test = datasetOf(
                new PerceptronTrainingSample(1, 2, 3, 1),
                new PerceptronTrainingSample(4, 5, 6, 1),
                new PerceptronTrainingSample(7, 8, 9, -1));

        PerceptronEvaluationResult result = evaluator.evaluate(alwaysPositiveModel(), anyTrainingDataset(), test);

        assertEquals(2, result.truePositives());
        assertEquals(1, result.falsePositives());
        assertEquals(0, result.trueNegatives());
        assertEquals(0, result.falseNegatives());
        assertEquals(2.0 / 3.0, result.precision(), DELTA);
        assertEquals(1.0, result.recall(), DELTA);
        assertEquals(0.8, result.f1Score(), DELTA);
    }

    @Test
    void worksWhenAllPredictionsAreNegative() {
        PerceptronDataset test = datasetOf(
                new PerceptronTrainingSample(1, 2, 3, 1),
                new PerceptronTrainingSample(4, 5, 6, -1),
                new PerceptronTrainingSample(7, 8, 9, -1));

        PerceptronEvaluationResult result = evaluator.evaluate(alwaysNegativeModel(), anyTrainingDataset(), test);

        assertEquals(0, result.truePositives());
        assertEquals(2, result.trueNegatives());
        assertEquals(0, result.falsePositives());
        assertEquals(1, result.falseNegatives());
        assertEquals(2.0 / 3.0, result.accuracy(), DELTA);
    }

    @Test
    void precisionAndF1AreZeroWhenModelNeverPredictsPositive() {
        PerceptronDataset test = datasetOf(
                new PerceptronTrainingSample(1, 2, 3, 1),
                new PerceptronTrainingSample(4, 5, 6, -1));

        PerceptronEvaluationResult result = evaluator.evaluate(alwaysNegativeModel(), anyTrainingDataset(), test);

        assertEquals(0.0, result.precision(), DELTA);
        assertEquals(0.0, result.recall(), DELTA);
        assertEquals(0.0, result.f1Score(), DELTA);
    }

    @Test
    void recallPrecisionAndF1AreZeroWhenTestDataHasNoPositives() {
        PerceptronDataset test = datasetOf(
                new PerceptronTrainingSample(1, 2, 3, -1),
                new PerceptronTrainingSample(4, 5, 6, -1));

        PerceptronEvaluationResult result = evaluator.evaluate(alwaysNegativeModel(), anyTrainingDataset(), test);

        assertEquals(2, result.trueNegatives());
        assertEquals(0.0, result.precision(), DELTA);
        assertEquals(0.0, result.recall(), DELTA);
        assertEquals(0.0, result.f1Score(), DELTA);
        assertEquals(1.0, result.accuracy(), DELTA);
    }

    @Test
    void nonFiniteActivationExceptionIsPropagated() {
        PerceptronModel overflowingModel = new PerceptronModel(new double[] {Double.MAX_VALUE, 0.0, 0.0}, 0.0);
        PerceptronDataset test = datasetOf(new PerceptronTrainingSample(Double.MAX_VALUE, 0, 0, 1));

        assertThrows(IllegalArgumentException.class,
                () -> evaluator.evaluate(overflowingModel, anyTrainingDataset(), test));
    }

    @Test
    void evaluationDoesNotMutateDatasetsOrModel() {
        PerceptronDataset training = anyTrainingDataset();
        PerceptronDataset test = oneOfEachTestDataset();
        PerceptronModel model = firstReadingModel();
        List<String> trainingColumnsBefore = List.copyOf(training.featureColumns());
        List<PerceptronTrainingSample> trainingSamplesBefore = List.copyOf(training.samples());
        List<String> testColumnsBefore = List.copyOf(test.featureColumns());
        List<PerceptronTrainingSample> testSamplesBefore = List.copyOf(test.samples());

        evaluator.evaluate(model, training, test);

        assertEquals(trainingColumnsBefore, training.featureColumns());
        assertEquals(trainingSamplesBefore, training.samples());
        assertEquals(testColumnsBefore, test.featureColumns());
        assertEquals(testSamplesBefore, test.samples());
        assertArrayEquals(new double[] {1.0, 0.0, 0.0}, model.weights(), 0.0);
        assertEquals(0.0, model.bias(), 0.0);
    }

    @Test
    void repeatedEvaluationsReturnIdenticalResults() {
        PerceptronDataset training = anyTrainingDataset();
        PerceptronDataset test = oneOfEachTestDataset();
        PerceptronModel model = firstReadingModel();

        PerceptronEvaluationResult first = evaluator.evaluate(model, training, test);
        PerceptronEvaluationResult second = evaluator.evaluate(model, training, test);

        assertEquals(first, second);
        assertEquals(first.accuracy(), second.accuracy(), 0.0);
        assertEquals(first.precision(), second.precision(), 0.0);
        assertEquals(first.recall(), second.recall(), 0.0);
        assertEquals(first.f1Score(), second.f1Score(), 0.0);
    }
}