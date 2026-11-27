package com.campuswaste.perceptron;

import java.util.Objects;

public final class PerceptronModel {

    private static final int INPUT_COUNT = 3;

    private final double[] weights;
    private final double bias;

    public PerceptronModel(double[] weights, double bias) {
        Objects.requireNonNull(weights, "weights must not be null");
        double[] copy = weights.clone();
        requireInputCount(copy, "weights");
        requireAllFinite(copy, "weight");
        if (!Double.isFinite(bias)) {
            throw new IllegalArgumentException("bias must be finite but was " + bias);
        }
        this.weights = copy;
        this.bias = bias;
    }

    public double[] weights() {
        return weights.clone();
    }

    public double bias() {
        return bias;
    }

    public int predict(double[] inputs) {
        Objects.requireNonNull(inputs, "inputs must not be null");
        requireInputCount(inputs, "inputs");
        requireAllFinite(inputs, "input");
        double activation = weights[0] * inputs[0]
                + weights[1] * inputs[1]
                + weights[2] * inputs[2]
                + bias;
        if (!Double.isFinite(activation)) {
            throw new IllegalArgumentException("activation is not finite: " + activation);
        }
        return activation >= 0 ? 1 : -1;
    }

    private static void requireInputCount(double[] values, String name) {
        if (values.length != INPUT_COUNT) {
            throw new IllegalArgumentException(
                    name + " must contain exactly " + INPUT_COUNT + " values but had " + values.length);
        }
    }

    private static void requireAllFinite(double[] values, String name) {
        for (int i = 0; i < values.length; i++) {
            if (!Double.isFinite(values[i])) {
                throw new IllegalArgumentException(
                        name + " at position " + i + " must be finite but was " + values[i]);
            }
        }
    }
}