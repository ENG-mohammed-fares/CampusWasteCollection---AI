package com.campuswaste.data;

import com.campuswaste.model.PerceptronDataset;
import com.campuswaste.model.PerceptronTrainingSample;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class PerceptronCsvLoader {

    private static final String LABEL_COLUMN = "pickup_label";
    private static final int COLUMN_COUNT = 4;
    private static final int BOM = 0xFEFF;
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?");
    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .build();

    public PerceptronDataset load(Path csvPath) throws IOException {
        Objects.requireNonNull(csvPath, "csvPath must not be null");
        try (BufferedReader reader = openReader(csvPath);
             CSVParser parser = FORMAT.parse(reader)) {
            return read(csvPath, parser.iterator());
        } catch (UncheckedIOException e) {
            throw new IOException("Failed to read CSV file " + csvPath + ": " + e.getMessage(), e.getCause());
        }
    }

    private static BufferedReader openReader(Path path) throws IOException {
        BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        try {
            reader.mark(1);
            if (reader.read() != BOM) {
                reader.reset();
            }
            return reader;
        } catch (IOException | RuntimeException e) {
            reader.close();
            throw e;
        }
    }

    private static PerceptronDataset read(Path path, Iterator<CSVRecord> records) throws IOException {
        if (!records.hasNext()) {
            throw new IOException("Invalid CSV file " + path + ": the file is empty and has no header row");
        }
        CSVRecord header = records.next();
        List<String> names = readHeaderNames(path, header);
        int labelIndex = names.indexOf(LABEL_COLUMN);
        if (labelIndex < 0) {
            throw error(path, header.getRecordNumber(), null, "header must contain the column '" + LABEL_COLUMN + "'");
        }
        List<String> featureColumns = new ArrayList<>(names);
        featureColumns.remove(labelIndex);

        List<PerceptronTrainingSample> samples = new ArrayList<>();
        while (records.hasNext()) {
            samples.add(readSample(path, records.next(), names, labelIndex));
        }
        if (samples.isEmpty()) {
            throw new IOException("Invalid CSV file " + path + ": the file contains a header but no data rows");
        }
        return new PerceptronDataset(featureColumns, samples);
    }

    private static List<String> readHeaderNames(Path path, CSVRecord header) throws IOException {
        long recordNumber = header.getRecordNumber();
        if (header.size() != COLUMN_COUNT) {
            throw error(path, recordNumber, null,
                    "header must have exactly " + COLUMN_COUNT + " columns but has " + header.size());
        }
        List<String> names = new ArrayList<>(COLUMN_COUNT);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < COLUMN_COUNT; i++) {
            String name = header.get(i);
            if (name.isEmpty()) {
                throw error(path, recordNumber, null, "header column " + (i + 1) + " is blank");
            }
            if (!seen.add(name)) {
                throw error(path, recordNumber, null, "duplicate header name '" + name + "'");
            }
            names.add(name);
        }
        return names;
    }

    private static PerceptronTrainingSample readSample(
            Path path, CSVRecord record, List<String> names, int labelIndex) throws IOException {
        if (record.size() != COLUMN_COUNT) {
            throw error(path, record.getRecordNumber(), null,
                    "expected " + COLUMN_COUNT + " fields but found " + record.size());
        }
        double[] readings = new double[COLUMN_COUNT - 1];
        int target = 0;
        for (int i = 0; i < COLUMN_COUNT; i++) {
            if (i != labelIndex) {
                readings[target] = parseReading(path, record, i, names.get(i));
                target++;
            }
        }
        int label = parseLabel(path, record, labelIndex);
        return new PerceptronTrainingSample(readings[0], readings[1], readings[2], label);
    }

    private static double parseReading(Path path, CSVRecord record, int index, String column) throws IOException {
        String value = record.get(index);
        if (value.isEmpty()) {
            throw error(path, record.getRecordNumber(), column, "value must not be blank");
        }
        if (!DECIMAL.matcher(value).matches()) {
            throw error(path, record.getRecordNumber(), column, "value is not a decimal number: '" + value + "'");
        }
        double parsed = Double.parseDouble(value);
        if (!Double.isFinite(parsed)) {
            throw error(path, record.getRecordNumber(), column, "value must be finite: '" + value + "'");
        }
        return parsed;
    }

    private static int parseLabel(Path path, CSVRecord record, int index) throws IOException {
        String value = record.get(index);
        if (!INTEGER.matcher(value).matches()) {
            throw error(path, record.getRecordNumber(), LABEL_COLUMN, "label must be +1 or -1 but was '" + value + "'");
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw error(path, record.getRecordNumber(), LABEL_COLUMN, "label must be +1 or -1 but was '" + value + "'");
        }
        if (parsed != 1 && parsed != -1) {
            throw error(path, record.getRecordNumber(), LABEL_COLUMN, "label must be +1 or -1 but was '" + value + "'");
        }
        return parsed;
    }

    private static IOException error(Path path, long recordNumber, String column, String detail) {
        String location = column == null ? "" : ", column '" + column + "'";
        return new IOException("Invalid CSV file " + path + " at record " + recordNumber + location + ": " + detail);
    }
}