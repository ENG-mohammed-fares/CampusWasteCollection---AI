package com.campuswaste.perceptron;

import com.campuswaste.model.PerceptronDataset;
import com.campuswaste.model.PerceptronTrainingSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PerceptronModelSelector {

    public static final List<Double> POSITIVE_CLASS_WEIGHTS = List.of(1.0, 2.0, 5.0, 10.0);
    public static final double VALIDATION_FRACTION = 0.20;
    public static final int MAX_EPOCHS = 1000;

    private static final double LEARNING_RATE = 1.0;

    public SelectionResult select(PerceptronDataset dataset) {
        Objects.requireNonNull(dataset, "dataset must not be null");
        DatasetSplit split = stratifiedSplit(dataset);
        CandidateScore bestCandidate = null;
        PerceptronTrainingResult bestResult = null;

        for (double positiveClassWeight : POSITIVE_CLASS_WEIGHTS) {
            CandidateTraining candidate = trainCandidate(
                    split.training(), split.validation(), positiveClassWeight);
            CandidateScore score = candidate.score();
            if (bestCandidate == null || score.validationF1() > bestCandidate.validationF1()) {
                bestCandidate = score;
                bestResult = candidate.result();
            }
        }

        return new SelectionResult(
                Objects.requireNonNull(bestResult).model(),
                Objects.requireNonNull(bestResult).epochsCompleted(),
                bestResult.converged(),
                Objects.requireNonNull(bestCandidate).positiveClassWeight(),
                bestCandidate.validationF1(),
                bestCandidate.lastEpochValidationF1(),
                List.of(trainCandidate(split.training(), split.validation(), POSITIVE_CLASS_WEIGHTS.get(0)).score()),
                split.training().samples().size(),
                split.validation().samples().size(),
                scoresForAllWeights(split));
    }

    private List<CandidateScore> scoresForAllWeights(DatasetSplit split) {
        List<CandidateScore> scores = new ArrayList<>(POSITIVE_CLASS_WEIGHTS.size());
        for (double weight : POSITIVE_CLASS_WEIGHTS) {
            scores.add(trainCandidate(split.training(), split.validation(), weight).score());
        }
        return List.copyOf(scores);
    }

    private CandidateTraining trainCandidate(
            PerceptronDataset training, PerceptronDataset validation, double positiveClassWeight) {
        PerceptronModel model = new PerceptronModel(new double[3], 0.0);
        PerceptronModel pocket = model;
        double bestF1 = -1.0;
        double lastEpochF1 = 0.0;
        int bestEpoch = 0;
        int epochsCompleted = 0;
        boolean converged = false;

        for (int epoch = 1; epoch <= MAX_EPOCHS; epoch++) {
            boolean updated = false;
            for (PerceptronTrainingSample sample : training.samples()) {
                double[] inputs = sample.toInputVector();
                if (model.predict(inputs) != sample.pickupLabel()) {
                    model = update(model, inputs, sample.pickupLabel(), positiveClassWeight, epoch);
                    updated = true;
                }
            }
            epochsCompleted = epoch;
            lastEpochF1 = evaluateF1(model, validation);
            if (lastEpochF1 > bestF1) {
                bestF1 = lastEpochF1;
                pocket = model;
                bestEpoch = epoch;
            }
            if (!updated) {
                converged = true;
                break;
            }
        }

        PerceptronTrainingResult result = new PerceptronTrainingResult(pocket, epochsCompleted, converged);
        return new CandidateTraining(
                result,
                new CandidateScore(
                        positiveClassWeight, bestEpoch, epochsCompleted, bestF1, lastEpochF1, converged));
    }

    private PerceptronModel update(
            PerceptronModel model, double[] inputs, int label, double positiveClassWeight, int epoch) {
        double updateScale = LEARNING_RATE * label * (label == 1 ? positiveClassWeight : 1.0);
        double[] weights = model.weights();
        for (int index = 0; index < weights.length; index++) {
            weights[index] += updateScale * inputs[index];
            if (!Double.isFinite(weights[index])) {
                throw new IllegalArgumentException(
                        "weight " + index + " became non-finite during epoch " + epoch);
            }
        }
        double bias = model.bias() + updateScale;
        if (!Double.isFinite(bias)) {
            throw new IllegalArgumentException("bias became non-finite during epoch " + epoch);
        }
        return new PerceptronModel(weights, bias);
    }

    private double evaluateF1(PerceptronModel model, PerceptronDataset dataset) {
        int truePositives = 0;
        int falsePositives = 0;
        int falseNegatives = 0;
        for (PerceptronTrainingSample sample : dataset.samples()) {
            int prediction = model.predict(sample.toInputVector());
            if (sample.pickupLabel() == 1 && prediction == 1) {
                truePositives++;
            } else if (sample.pickupLabel() == -1 && prediction == 1) {
                falsePositives++;
            } else if (sample.pickupLabel() == 1) {
                falseNegatives++;
            }
        }
        int predictedPositives = truePositives + falsePositives;
        int actualPositives = truePositives + falseNegatives;
        double precision = predictedPositives == 0 ? 0.0 : (double) truePositives / predictedPositives;
        double recall = actualPositives == 0 ? 0.0 : (double) truePositives / actualPositives;
        return precision + recall == 0.0 ? 0.0 : 2.0 * precision * recall / (precision + recall);
    }

    private DatasetSplit stratifiedSplit(PerceptronDataset dataset) {
        List<PerceptronTrainingSample> trainingSamples = new ArrayList<>();
        List<PerceptronTrainingSample> validationSamples = new ArrayList<>();
        for (int label : new int[] {1, -1}) {
            List<PerceptronTrainingSample> classSamples = dataset.samples().stream()
                    .filter(sample -> sample.pickupLabel() == label)
                    .toList();
            if (classSamples.size() < 2) {
                trainingSamples.addAll(classSamples);
                continue;
            }
            int validationCount = Math.max(1, (int) Math.round(classSamples.size() * VALIDATION_FRACTION));
            validationCount = Math.min(validationCount, classSamples.size() - 1);
            int trainingCount = classSamples.size() - validationCount;
            trainingSamples.addAll(classSamples.subList(0, trainingCount));
            validationSamples.addAll(classSamples.subList(trainingCount, classSamples.size()));
        }
        if (trainingSamples.isEmpty() || validationSamples.isEmpty()) {
            throw new IllegalArgumentException("dataset must contain at least two samples for a training/validation split");
        }
        return new DatasetSplit(
                new PerceptronDataset(dataset.featureColumns(), trainingSamples),
                new PerceptronDataset(dataset.featureColumns(), validationSamples));
    }

    private record DatasetSplit(PerceptronDataset training, PerceptronDataset validation) {}

    private record CandidateTraining(PerceptronTrainingResult result, CandidateScore score) {}

    public record CandidateScore(
            double positiveClassWeight,
            int pocketEpoch,
            int epochsCompleted,
            double validationF1,
            double lastEpochValidationF1,
            boolean converged) {}

    public record SelectionResult(
            PerceptronModel model,
            int epochsCompleted,
            boolean converged,
            double positiveClassWeight,
            double validationF1,
            double lastEpochValidationF1,
            List<CandidateScore> candidateScores,
            int trainingSampleCount,
            int validationSampleCount,
            List<CandidateScore> allCandidateScores) {

        public SelectionResult {
            Objects.requireNonNull(model, "model must not be null");
            candidateScores = List.copyOf(candidateScores);
            allCandidateScores = List.copyOf(allCandidateScores);
            if (candidateScores.size() != 1) {
                throw new IllegalArgumentException("candidateScores must contain the selected candidate only");
            }
        }

        public CandidateScore selectedCandidate() {
            return candidateScores.get(0);
        }
    }
}
