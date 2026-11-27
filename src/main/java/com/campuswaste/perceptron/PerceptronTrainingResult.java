package com.campuswaste.perceptron;

import java.util.Objects;

public record PerceptronTrainingResult(PerceptronModel model, int epochsCompleted, boolean converged) {

    public PerceptronTrainingResult {
        Objects.requireNonNull(model, "model must not be null");
        if (epochsCompleted <= 0) {
            throw new IllegalArgumentException("epochsCompleted must be greater than zero but was " + epochsCompleted);
        }
    }
}