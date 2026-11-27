package com.campuswaste.perceptron;

public record PerceptronEvaluationResult(
        int totalSamples,
        int truePositives,
        int trueNegatives,
        int falsePositives,
        int falseNegatives) {

    public PerceptronEvaluationResult {
        if (totalSamples <= 0) {
            throw new IllegalArgumentException("totalSamples must be greater than zero but was " + totalSamples);
        }
        requireNotNegative(truePositives, "truePositives");
        requireNotNegative(trueNegatives, "trueNegatives");
        requireNotNegative(falsePositives, "falsePositives");
        requireNotNegative(falseNegatives, "falseNegatives");
        long countSum = (long) truePositives + trueNegatives + falsePositives + falseNegatives;
        if (countSum != totalSamples) {
            throw new IllegalArgumentException(
                    "confusion matrix counts sum to " + countSum + " but totalSamples is " + totalSamples);
        }
    }

    public int correctPredictions() {
        return truePositives + trueNegatives;
    }

    public int incorrectPredictions() {
        return falsePositives + falseNegatives;
    }

    public double accuracy() {
        return (double) correctPredictions() / totalSamples;
    }

    public double precision() {
        long predictedPositives = (long) truePositives + falsePositives;
        if (predictedPositives == 0) {
            return 0.0;
        }
        return (double) truePositives / predictedPositives;
    }

    public double recall() {
        long actualPositives = (long) truePositives + falseNegatives;
        if (actualPositives == 0) {
            return 0.0;
        }
        return (double) truePositives / actualPositives;
    }

    public double f1Score() {
        double precision = precision();
        double recall = recall();
        double sum = precision + recall;
        if (sum == 0.0) {
            return 0.0;
        }
        return 2.0 * precision * recall / sum;
    }

    private static void requireNotNegative(int count, String name) {
        if (count < 0) {
            throw new IllegalArgumentException(name + " must not be negative but was " + count);
        }
    }
}