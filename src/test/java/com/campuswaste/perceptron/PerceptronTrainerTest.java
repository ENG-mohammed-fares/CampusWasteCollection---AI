package com.campuswaste.perceptron;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.campuswaste.data.PerceptronCsvLoader;
import com.campuswaste.model.PerceptronDataset;
import com.campuswaste.model.PerceptronTrainingSample;
import java.util.List;
import java.nio.file.Path;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PerceptronTrainerTest {

    private static final double DELTA = 0.0;

    private static PerceptronDataset datasetOf(PerceptronTrainingSample... samples) {
        return new PerceptronDataset(List.of("a", "b", "c"), List.of(samples));
    }

    private static PerceptronDataset separableDataset() {
        return datasetOf(
                new PerceptronTrainingSample(1, 0, 0, 1),
                new PerceptronTrainingSample(0, 1, 0, 1),
                new PerceptronTrainingSample(0, 0, 1, 1),
                new PerceptronTrainingSample(-1, -1, -1, -1));
    }

    private static PerceptronDataset nonSeparableDataset() {
        return datasetOf(
                new PerceptronTrainingSample(0, 0, 0, -1),
                new PerceptronTrainingSample(0, 1, 0, 1),
                new PerceptronTrainingSample(1, 0, 0, 1),
                new PerceptronTrainingSample(1, 1, 0, -1));
    }

    @Test
    void defaultConstantsHaveSpecifiedValues() {
        assertEquals(1.0, PerceptronTrainer.DEFAULT_LEARNING_RATE, DELTA);
        assertEquals(1000, PerceptronTrainer.DEFAULT_MAX_EPOCHS);
    }

    @Test
    void defaultConstructorUsesDefaultLearningRate() {
        PerceptronTrainingResult result = new PerceptronTrainer().train(separableDataset());

        assertArrayEquals(new double[] {1.0, 1.0, 1.0}, result.model().weights(), DELTA);
        assertEquals(-1.0, result.model().bias(), DELTA);
    }

    @Test
    void defaultConstructorUsesDefaultMaxEpochs() {
        PerceptronTrainingResult result = new PerceptronTrainer().train(nonSeparableDataset());

        assertEquals(1000, result.epochsCompleted());
        assertFalse(result.converged());
    }

    @Test
    void customLearningRateScalesUpdates() {
        PerceptronTrainingResult result = new PerceptronTrainer(0.5, 100).train(separableDataset());

        assertArrayEquals(new double[] {0.5, 0.5, 0.5}, result.model().weights(), DELTA);
        assertEquals(-0.5, result.model().bias(), DELTA);
        assertEquals(2, result.epochsCompleted());
        assertTrue(result.converged());
    }

    @Test
    void customMaxEpochsLimitsTraining() {
        PerceptronTrainingResult result = new PerceptronTrainer(1.0, 7).train(nonSeparableDataset());

        assertEquals(7, result.epochsCompleted());
        assertFalse(result.converged());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.5, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidLearningRatesAreRejected(double learningRate) {
        assertThrows(IllegalArgumentException.class, () -> new PerceptronTrainer(learningRate, 10));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void invalidMaxEpochsAreRejected(int maxEpochs) {
        assertThrows(IllegalArgumentException.class, () -> new PerceptronTrainer(1.0, maxEpochs));
    }

    @Test
    void nullDatasetIsRejected() {
        assertThrows(NullPointerException.class, () -> new PerceptronTrainer().train(null));
    }

    @Test
    void learnsLinearlySeparableDataset() {
        PerceptronDataset dataset = separableDataset();

        PerceptronTrainingResult result = new PerceptronTrainer().train(dataset);

        assertArrayEquals(new double[] {1.0, 1.0, 1.0}, result.model().weights(), DELTA);
        assertEquals(-1.0, result.model().bias(), DELTA);
        for (PerceptronTrainingSample sample : dataset.samples()) {
            assertEquals(sample.pickupLabel(), result.model().predict(sample.toInputVector()));
        }
    }

    @Test
    void convergesWhenFullEpochMakesNoUpdates() {
        PerceptronTrainingResult result = new PerceptronTrainer().train(separableDataset());

        assertTrue(result.converged());
        assertEquals(2, result.epochsCompleted());
    }

    @Test
    void convergesInFirstEpochWhenNoSampleIsMisclassified() {
        PerceptronDataset dataset = datasetOf(
                new PerceptronTrainingSample(1, 2, 3, 1),
                new PerceptronTrainingSample(4, 5, 6, 1));

        PerceptronTrainingResult result = new PerceptronTrainer().train(dataset);

        assertTrue(result.converged());
        assertEquals(1, result.epochsCompleted());
        assertArrayEquals(new double[] {0.0, 0.0, 0.0}, result.model().weights(), DELTA);
        assertEquals(0.0, result.model().bias(), DELTA);
    }

    @Test
    void doesNotConvergeOnNonSeparableDataset() {
        PerceptronTrainingResult result = new PerceptronTrainer(1.0, 25).train(nonSeparableDataset());

        assertFalse(result.converged());
        assertEquals(25, result.epochsCompleted());
    }

    @Test
    void stopsAtMaxEpochsWhenUpdatesOccurInFinalEpochOnSeparableData() {
        PerceptronTrainingResult result = new PerceptronTrainer(1.0, 1).train(separableDataset());

        assertFalse(result.converged());
        assertEquals(1, result.epochsCompleted());
        assertArrayEquals(new double[] {1.0, 1.0, 1.0}, result.model().weights(), DELTA);
        assertEquals(-1.0, result.model().bias(), DELTA);
    }

    @Test
    void repeatedTrainingProducesIdenticalResults() {
        PerceptronTrainer trainer = new PerceptronTrainer(0.3, 50);
        PerceptronDataset dataset = nonSeparableDataset();

        PerceptronTrainingResult first = trainer.train(dataset);
        PerceptronTrainingResult second = trainer.train(dataset);

        assertArrayEquals(first.model().weights(), second.model().weights(), DELTA);
        assertEquals(first.model().bias(), second.model().bias(), DELTA);
        assertEquals(first.epochsCompleted(), second.epochsCompleted());
        assertEquals(first.converged(), second.converged());
    }

    @Test
    void trainingStartsFromZeroOnEveryCall() {
        PerceptronTrainer trainer = new PerceptronTrainer();

        trainer.train(nonSeparableDataset());
        PerceptronTrainingResult result = trainer.train(separableDataset());

        assertArrayEquals(new double[] {1.0, 1.0, 1.0}, result.model().weights(), DELTA);
        assertEquals(-1.0, result.model().bias(), DELTA);
        assertEquals(2, result.epochsCompleted());
    }

    @Test
    void trainingDoesNotMutateDatasetOrSamples() {
        PerceptronDataset dataset = nonSeparableDataset();
        List<String> columnsBefore = List.copyOf(dataset.featureColumns());
        List<PerceptronTrainingSample> samplesBefore = List.copyOf(dataset.samples());

        new PerceptronTrainer(1.0, 10).train(dataset);

        assertEquals(columnsBefore, dataset.featureColumns());
        assertEquals(samplesBefore, dataset.samples());
    }

    @Test
    void nonFiniteUpdateFromOverflowIsRejected() {
        PerceptronDataset dataset = datasetOf(new PerceptronTrainingSample(Double.MAX_VALUE, 0, 0, -1));
        PerceptronTrainer trainer = new PerceptronTrainer(Double.MAX_VALUE, 10);

        assertThrows(IllegalArgumentException.class, () -> trainer.train(dataset));
    }

    @Test
    void realDatasetMeetsRequiredTestMetrics() throws IOException {
        Path dataDirectory = Path.of("src", "main", "resources", "data");
        PerceptronCsvLoader loader = new PerceptronCsvLoader();
        PerceptronDataset trainingDataset = loader.load(dataDirectory.resolve("waste_bin_perceptron_train.csv"));
        PerceptronDataset testDataset = loader.load(dataDirectory.resolve("waste_bin_perceptron_test.csv"));

        PerceptronTrainingResult training = new PerceptronTrainer().train(trainingDataset);
        PerceptronEvaluationResult evaluation = new PerceptronEvaluator()
                .evaluate(training.model(), trainingDataset, testDataset);

        assertAll(
                () -> assertEquals(2113, evaluation.totalSamples()),
                () -> assertEquals(10, evaluation.truePositives()),
                () -> assertEquals(1911, evaluation.trueNegatives()),
                () -> assertEquals(4, evaluation.falsePositives()),
                () -> assertEquals(188, evaluation.falseNegatives()),
                () -> assertEquals(1921.0 / 2113.0, evaluation.accuracy(), 1.0e-12),
                () -> assertTrue(training.epochsCompleted() <= PerceptronTrainer.DEFAULT_MAX_EPOCHS));
    }
}
