package com.campuswaste.perceptron;

import com.campuswaste.model.PerceptronDataset;
import com.campuswaste.model.PerceptronTrainingSample;
import java.util.Objects;

public final class PerceptronTrainer {

    public static final double DEFAULT_LEARNING_RATE = 1.0;
    public static final int DEFAULT_MAX_EPOCHS = 1000;

    private static final int INPUT_COUNT = 3;

    private final double learningRate;
    private final int maxEpochs;

    public PerceptronTrainer() {
        this(DEFAULT_LEARNING_RATE, DEFAULT_MAX_EPOCHS);
    }

    public PerceptronTrainer(double learningRate, int maxEpochs) {
        if (!Double.isFinite(learningRate) || learningRate <= 0) {
            throw new IllegalArgumentException("learningRate must be finite and greater than zero but was " + learningRate);
        }
        if (maxEpochs <= 0) {
            throw new IllegalArgumentException("maxEpochs must be greater than zero but was " + maxEpochs);
        }
        this.learningRate = learningRate;
        this.maxEpochs = maxEpochs;
    }

    public PerceptronTrainingResult train(PerceptronDataset dataset) {
        Objects.requireNonNull(dataset, "dataset must not be null");
        PerceptronModel model = new PerceptronModel(new double[INPUT_COUNT], 0.0);
        for (int epoch = 1; epoch <= maxEpochs; epoch++) {
            boolean updated = false;
            for (PerceptronTrainingSample sample : dataset.samples()) {
                double[] inputs = sample.toInputVector();
                if (model.predict(inputs) != sample.pickupLabel()) {
                    model = updatedModel(model, inputs, sample.pickupLabel(), epoch);
                    updated = true;
                }
            }
            if (!updated) {
                return new PerceptronTrainingResult(model, epoch, true);
            }
        }
        return new PerceptronTrainingResult(model, maxEpochs, false);
    }

    private PerceptronModel updatedModel(PerceptronModel model, double[] inputs, int label, int epoch) {
        double[] weights = model.weights();
        for (int i = 0; i < INPUT_COUNT; i++) {
            weights[i] = weights[i] + learningRate * label * inputs[i];
            if (!Double.isFinite(weights[i])) {
                throw new IllegalArgumentException(
                        "weight " + i + " became non-finite during epoch " + epoch + ": " + weights[i]);
            }
        }
        double bias = model.bias() + learningRate * label;
        if (!Double.isFinite(bias)) {
            throw new IllegalArgumentException("bias became non-finite during epoch " + epoch + ": " + bias);
        }
        return new PerceptronModel(weights, bias);
    }
}
