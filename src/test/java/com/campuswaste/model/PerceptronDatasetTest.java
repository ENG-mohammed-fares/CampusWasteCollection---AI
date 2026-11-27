package com.campuswaste.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PerceptronDatasetTest {

    private static PerceptronTrainingSample sample() {
        return new PerceptronTrainingSample(1, 2, 3, 1);
    }

    @Test
    void validDatasetStoresFeatureNamesAndSamples() {
        PerceptronTrainingSample first = new PerceptronTrainingSample(1, 2, 3, 1);
        PerceptronTrainingSample second = new PerceptronTrainingSample(4, 5, 6, -1);

        PerceptronDataset dataset = new PerceptronDataset(List.of("a", "b", "c"), List.of(first, second));

        assertEquals(List.of("a", "b", "c"), dataset.featureColumns());
        assertEquals(List.of(first, second), dataset.samples());
    }

    @Test
    void featureNamesAreTrimmed() {
        PerceptronDataset dataset = new PerceptronDataset(List.of(" a ", "b\t", "\nc"), List.of(sample()));

        assertEquals(List.of("a", "b", "c"), dataset.featureColumns());
    }

    @Test
    void nullFeatureColumnsAreRejected() {
        assertThrows(NullPointerException.class, () -> new PerceptronDataset(null, List.of(sample())));
    }

    @Test
    void nullSamplesListIsRejected() {
        assertThrows(NullPointerException.class, () -> new PerceptronDataset(List.of("a", "b", "c"), null));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 4, 5})
    void featureNameCountOtherThanThreeIsRejected(int size) {
        List<String> names = IntStream.range(0, size).mapToObj(i -> "f" + i).toList();

        assertThrows(IllegalArgumentException.class, () -> new PerceptronDataset(names, List.of(sample())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", " \n "})
    void blankFeatureNameIsRejected(String blank) {
        List<String> names = List.of("a", blank, "c");

        assertThrows(IllegalArgumentException.class, () -> new PerceptronDataset(names, List.of(sample())));
    }

    @Test
    void nullFeatureNameIsRejected() {
        List<String> names = Arrays.asList("a", null, "c");

        assertThrows(NullPointerException.class, () -> new PerceptronDataset(names, List.of(sample())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", " a", "a ", " a "})
    void duplicateFeatureNameAfterTrimmingIsRejected(String duplicate) {
        List<String> names = List.of("a", duplicate, "c");

        assertThrows(IllegalArgumentException.class, () -> new PerceptronDataset(names, List.of(sample())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pickup_label", " pickup_label "})
    void labelColumnAsFeatureNameIsRejected(String labelName) {
        List<String> names = List.of("a", "b", labelName);

        assertThrows(IllegalArgumentException.class, () -> new PerceptronDataset(names, List.of(sample())));
    }

    @Test
    void emptySamplesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new PerceptronDataset(List.of("a", "b", "c"), List.of()));
    }

    @Test
    void nullSampleIsRejected() {
        List<PerceptronTrainingSample> samples = Arrays.asList(sample(), null);

        assertThrows(NullPointerException.class, () -> new PerceptronDataset(List.of("a", "b", "c"), samples));
    }

    @Test
    void changingOriginalListsDoesNotAffectDataset() {
        List<String> names = new ArrayList<>(List.of("a", "b", "c"));
        List<PerceptronTrainingSample> samples = new ArrayList<>(List.of(sample()));
        PerceptronDataset dataset = new PerceptronDataset(names, samples);

        names.set(0, "changed");
        samples.clear();
        samples.add(new PerceptronTrainingSample(9, 9, 9, -1));

        assertEquals(List.of("a", "b", "c"), dataset.featureColumns());
        assertEquals(List.of(sample()), dataset.samples());
    }

    @Test
    void listsReturnedByAccessorsCannotBeModified() {
        PerceptronDataset dataset = new PerceptronDataset(List.of("a", "b", "c"), List.of(sample()));

        assertThrows(UnsupportedOperationException.class, () -> dataset.featureColumns().add("d"));
        assertThrows(UnsupportedOperationException.class, () -> dataset.featureColumns().set(0, "x"));
        assertThrows(UnsupportedOperationException.class, () -> dataset.samples().clear());
        assertThrows(UnsupportedOperationException.class, () -> dataset.samples().add(sample()));
    }
}