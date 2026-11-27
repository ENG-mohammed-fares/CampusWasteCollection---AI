package com.campuswaste.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PerceptronDataset(List<String> featureColumns, List<PerceptronTrainingSample> samples) {

    private static final String LABEL_COLUMN = "pickup_label";
    private static final int FEATURE_COUNT = 3;

    public PerceptronDataset {
        Objects.requireNonNull(featureColumns, "featureColumns must not be null");
        Objects.requireNonNull(samples, "samples must not be null");
        if (featureColumns.size() != FEATURE_COUNT) {
            throw new IllegalArgumentException(
                    "featureColumns must contain exactly " + FEATURE_COUNT + " names but had " + featureColumns.size());
        }
        List<String> trimmedNames = new ArrayList<>(FEATURE_COUNT);
        Set<String> seen = new HashSet<>();
        for (String name : featureColumns) {
            Objects.requireNonNull(name, "feature column name must not be null");
            String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("feature column name must not be blank");
            }
            if (trimmed.equals(LABEL_COLUMN)) {
                throw new IllegalArgumentException("feature column name must not be " + LABEL_COLUMN);
            }
            if (!seen.add(trimmed)) {
                throw new IllegalArgumentException("duplicate feature column name: " + trimmed);
            }
            trimmedNames.add(trimmed);
        }
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("samples must not be empty");
        }
        List<PerceptronTrainingSample> sampleCopy = new ArrayList<>(samples.size());
        for (PerceptronTrainingSample sample : samples) {
            Objects.requireNonNull(sample, "samples must not contain null");
            sampleCopy.add(sample);
        }
        featureColumns = List.copyOf(trimmedNames);
        samples = List.copyOf(sampleCopy);
    }
}