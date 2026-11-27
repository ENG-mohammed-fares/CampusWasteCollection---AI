package com.campuswaste.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.campuswaste.model.PerceptronDataset;
import com.campuswaste.model.PerceptronTrainingSample;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PerceptronCsvLoaderTest {

    private static final String BOM = "\uFEFF";

    @TempDir
    Path tempDir;

    private static String csv(String... lines) {
        return String.join("\n", lines) + "\n";
    }

    private Path write(String content) throws IOException {
        Path file = tempDir.resolve("data.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private PerceptronDataset load(String content) throws IOException {
        return new PerceptronCsvLoader().load(write(content));
    }

    private IOException assertRejected(String content) throws IOException {
        Path file = write(content);
        return assertThrows(IOException.class, () -> new PerceptronCsvLoader().load(file));
    }

    static Stream<Arguments> nonFiniteCells() {
        List<Arguments> cases = new ArrayList<>();
        for (int position = 0; position < 3; position++) {
            for (String value : new String[] {"NaN", "nan", "Infinity", "-Infinity", "+Infinity", "1e999", "-1e999"}) {
                cases.add(Arguments.of(position, value));
            }
        }
        return cases.stream();
    }

    @Test
    void loadsMultipleRowsInOriginalOrder() throws IOException {
        PerceptronDataset dataset = load(csv(
                "a,b,c,pickup_label",
                "1.5,2.5,3.5,1",
                "-4,5e1,6.25,-1",
                "7,8,9,1"));

        assertEquals(List.of("a", "b", "c"), dataset.featureColumns());
        assertEquals(List.of(
                new PerceptronTrainingSample(1.5, 2.5, 3.5, 1),
                new PerceptronTrainingSample(-4, 50, 6.25, -1),
                new PerceptronTrainingSample(7, 8, 9, 1)), dataset.samples());
    }

    @Test
    void parsesPositiveAndNegativeLabels() throws IOException {
        PerceptronDataset dataset = load(csv(
                "a,b,c,pickup_label",
                "1,2,3,1",
                "1,2,3,-1",
                "1,2,3,+1"));

        List<Integer> labels = dataset.samples().stream().map(PerceptronTrainingSample::pickupLabel).toList();

        assertEquals(List.of(1, -1, 1), labels);
    }

    @Test
    void preservesFeatureColumnOrder() throws IOException {
        PerceptronDataset dataset = load(csv(
                "zeta,alpha,mid,pickup_label",
                "1,2,3,1"));

        assertEquals(List.of("zeta", "alpha", "mid"), dataset.featureColumns());
        assertEquals(new PerceptronTrainingSample(1, 2, 3, 1), dataset.samples().get(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void loadsCorrectlyWhenLabelColumnIsAtAnyPosition(int labelPosition) throws IOException {
        List<String> header = new ArrayList<>(List.of("f1", "f2", "f3"));
        List<String> row = new ArrayList<>(List.of("1.5", "2.5", "3.5"));
        header.add(labelPosition, "pickup_label");
        row.add(labelPosition, "-1");

        PerceptronDataset dataset = load(csv(String.join(",", header), String.join(",", row)));

        assertEquals(List.of("f1", "f2", "f3"), dataset.featureColumns());
        assertEquals(List.of(new PerceptronTrainingSample(1.5, 2.5, 3.5, -1)), dataset.samples());
    }

    @Test
    void trimsSurroundingSpacesFromHeadersAndValues() throws IOException {
        PerceptronDataset dataset = load(csv(
                " a , b ,c, pickup_label ",
                " 1.5 , 2.5 , 3.5 , 1 "));

        assertEquals(List.of("a", "b", "c"), dataset.featureColumns());
        assertEquals(List.of(new PerceptronTrainingSample(1.5, 2.5, 3.5, 1)), dataset.samples());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a,b,c,pickup_label", "\"a\",b,c,pickup_label"})
    void handlesUtf8BomBeforeFirstHeader(String header) throws IOException {
        PerceptronDataset dataset = load(BOM + csv(header, "1,2,3,1"));

        assertEquals(List.of("a", "b", "c"), dataset.featureColumns());
        assertEquals(List.of(new PerceptronTrainingSample(1, 2, 3, 1)), dataset.samples());
    }

    @Test
    void handlesUtf8BomWhenLabelIsFirstColumn() throws IOException {
        PerceptronDataset dataset = load(BOM + csv("pickup_label,a,b,c", "-1,1,2,3"));

        assertEquals(List.of("a", "b", "c"), dataset.featureColumns());
        assertEquals(List.of(new PerceptronTrainingSample(1, 2, 3, -1)), dataset.samples());
    }

    @Test
    void supportsQuotedFields() throws IOException {
        PerceptronDataset dataset = load(csv(
                "\"a,b\",second,\"third\",pickup_label",
                "\"1.5\",2.5,\"3.5\",\"-1\""));

        assertEquals(List.of("a,b", "second", "third"), dataset.featureColumns());
        assertEquals(List.of(new PerceptronTrainingSample(1.5, 2.5, 3.5, -1)), dataset.samples());
    }

    @Test
    void rejectsUnterminatedQuotedField() throws IOException {
        IOException exception = assertRejected(csv("a,b,c,pickup_label", "\"1,2,3,1"));

        assertTrue(exception.getMessage().contains("data.csv"));
    }

    @Test
    void ignoresEmptyLinesAndSupportsWindowsLineEndings() throws IOException {
        PerceptronDataset dataset = load("a,b,c,pickup_label\r\n\r\n1,2,3,1\r\n\r\n4,5,6,-1\r\n\r\n");

        assertEquals(List.of(
                new PerceptronTrainingSample(1, 2, 3, 1),
                new PerceptronTrainingSample(4, 5, 6, -1)), dataset.samples());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a,b,c,d", "a,b,c,Pickup_Label", "a,b,c,PICKUP_LABEL", "a,b,c,pickup_label_x"})
    void rejectsHeaderWithoutExactLabelColumn(String header) throws IOException {
        IOException exception = assertRejected(csv(header, "1,2,3,1"));

        assertTrue(exception.getMessage().contains("pickup_label"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pickup_label", "a,pickup_label", "a,b,pickup_label", "a,b,c,d,pickup_label"})
    void rejectsHeaderWithWrongColumnCount(String header) throws IOException {
        assertRejected(csv(header, "1,2,3,1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "a,,c,pickup_label",
            "a, ,c,pickup_label",
            "a,a,c,pickup_label",
            "a, a ,c,pickup_label",
            "a,b,pickup_label,pickup_label"
    })
    void rejectsBlankOrDuplicateHeaderNames(String header) throws IOException {
        assertRejected(csv(header, "1,2,3,1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1,2,3", "1,2", "1", "1,2,3,1,5", "1,2,3,1,"})
    void rejectsRowsWithMissingOrExtraFields(String row) throws IOException {
        IOException exception = assertRejected(csv("a,b,c,pickup_label", row));

        assertTrue(exception.getMessage().contains("record 2"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ",2,3,1",
            "1,,3,1",
            "1,2,,1",
            "1, ,3,1",
            "abc,2,3,1",
            "1,2x,3,1",
            "1,2,3d,1",
            "0x10,2,3,1",
            "1,2,3.4.5,1"
    })
    void rejectsBlankAndNonNumericFeatureValues(String row) throws IOException {
        assertRejected(csv("a,b,c,pickup_label", row));
    }

    @ParameterizedTest
    @MethodSource("nonFiniteCells")
    void rejectsNaNAndInfinityInEveryFeaturePosition(int position, String value) throws IOException {
        String[] fields = {"1", "2", "3"};
        fields[position] = value;
        String row = String.join(",", fields) + ",1";

        assertRejected(csv("a,b,c,pickup_label", row));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "2", "-2", "-0", "+0", "1.0", "abc", "", "  ", "++1", "1e0", "\u0661", "99999999999"})
    void rejectsLabelsOtherThanPlusOrMinusOne(String label) throws IOException {
        assertRejected(csv("a,b,c,pickup_label", "1,2,3," + label));
    }

    @Test
    void errorMessageIdentifiesPathRecordAndColumn() throws IOException {
        Path file = write(csv("a,b,c,pickup_label", "1,2,3,1", "4,oops,6,1"));

        IOException exception = assertThrows(IOException.class, () -> new PerceptronCsvLoader().load(file));

        assertTrue(exception.getMessage().contains(file.toString()));
        assertTrue(exception.getMessage().contains("record 3"));
        assertTrue(exception.getMessage().contains("'b'"));
    }

    @Test
    void errorMessageIdentifiesInvalidLabelColumn() throws IOException {
        IOException exception = assertRejected(csv("a,b,c,pickup_label", "1,2,3,5"));

        assertTrue(exception.getMessage().contains("record 2"));
        assertTrue(exception.getMessage().contains("'pickup_label'"));
    }

    @Test
    void oneInvalidRowFailsTheWholeLoad() throws IOException {
        assertRejected(csv("a,b,c,pickup_label", "1,2,3,1", "4,5,6,-1", "7,8,9,0", "1,1,1,1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a,b,c,pickup_label", "a,b,c,pickup_label\n", "a,b,c,pickup_label\n\n\n"})
    void rejectsHeaderOnlyCsv(String content) throws IOException {
        IOException exception = assertRejected(content);

        assertTrue(exception.getMessage().contains("no data rows"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\n", "\n\n"})
    void rejectsEmptyFile(String content) throws IOException {
        assertRejected(content);
    }

    @Test
    void throwsIoExceptionForMissingFile() {
        Path missing = tempDir.resolve("does-not-exist.csv");

        assertThrows(IOException.class, () -> new PerceptronCsvLoader().load(missing));
    }

    @Test
    void rejectsNullPath() {
        assertThrows(NullPointerException.class, () -> new PerceptronCsvLoader().load(null));
    }
}