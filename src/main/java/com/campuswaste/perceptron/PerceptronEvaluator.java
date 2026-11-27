package com.campuswaste.perceptron;

import com.campuswaste.model.PerceptronDataset;
import com.campuswaste.model.PerceptronTrainingSample;
import java.util.Objects;

public final class PerceptronEvaluator {

    public PerceptronEvaluationResult evaluate(
            PerceptronModel model,
            PerceptronDataset trainingDataset,
            PerceptronDataset testDataset) {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(trainingDataset, "trainingDataset must not be null");
        Objects.requireNonNull(testDataset, "testDataset must not be null");
        if (!trainingDataset.featureColumns().equals(testDataset.featureColumns())) {
            throw new IllegalArgumentException(
                    "training and test feature columns must match exactly in names and order: training="
                            + trainingDataset.featureColumns() + ", test=" + testDataset.featureColumns());
        }

        int truePositives = 0;
        int trueNegatives = 0;
        int falsePositives = 0;
        int falseNegatives = 0;
        for (PerceptronTrainingSample sample : testDataset.samples()) {
            int predicted = model.predict(sample.toInputVector());
            int actual = sample.pickupLabel();
            if (actual == 1) {
                if (predicted == 1) {
                    truePositives++;
                } else {
                    falseNegatives++;
                }
            } else {
                if (predicted == 1) {
                    falsePositives++;
                } else {
                    trueNegatives++;
                }
            }
        }
        return new PerceptronEvaluationResult(
                testDataset.samples().size(), truePositives, trueNegatives, falsePositives, falseNegatives);
    }
}