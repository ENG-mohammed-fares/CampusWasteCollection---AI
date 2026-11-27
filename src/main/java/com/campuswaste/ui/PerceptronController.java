package com.campuswaste.ui;

import com.campuswaste.data.PerceptronCsvLoader;
import com.campuswaste.model.PerceptronDataset;
import com.campuswaste.model.BinPrediction;
import com.campuswaste.model.RouteLocation;
import com.campuswaste.model.RoutePlanningResult;
import com.campuswaste.model.WasteBin;
import com.campuswaste.perceptron.PerceptronEvaluationResult;
import com.campuswaste.perceptron.PerceptronEvaluator;
import com.campuswaste.perceptron.PerceptronTrainer;
import com.campuswaste.perceptron.PerceptronTrainingResult;
import com.campuswaste.optimization.SimulatedAnnealingRouteOptimizer;
import com.campuswaste.service.PerceptronRoutePlanningService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class PerceptronController {

    private static final int REQUIRED_FEATURE_COUNT = 3;
    private static final int PICKUP_PREDICTION = 1;
    private static final String NO_PREDICTION_TEXT = "No prediction yet";
    private static final String PLACEHOLDER = "--";
    private static final Pattern DECIMAL_NUMBER =
            Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

    @FXML private Label systemStatusLabel;
    @FXML private Label perceptronOverviewStatusLabel;
    @FXML private Label trainingFileLabel;
    @FXML private Label trainingSampleCountLabel;
    @FXML private Label testFileLabel;
    @FXML private Label testSampleCountLabel;
    @FXML private Button trainAndTestButton;
    @FXML private Label modelStatusLabel;
    @FXML private Label workflowStatusLabel;
    @FXML private Label convergenceWarningLabel;
    @FXML private Label accuracyValue;
    @FXML private Label missedPickupsValue;
    @FXML private Label falseAlertsValue;
    @FXML private Label testSamplesValue;
    @FXML private Label truePositivesValue;
    @FXML private Label trueNegativesValue;
    @FXML private Label falsePositivesValue;
    @FXML private Label falseNegativesValue;
    @FXML private Label epochsValue;
    @FXML private Label convergenceValue;
    @FXML private Label reading1Label;
    @FXML private Label reading2Label;
    @FXML private Label reading3Label;
    @FXML private TextField reading1Field;
    @FXML private TextField reading2Field;
    @FXML private TextField reading3Field;
    @FXML private Button predictButton;
    @FXML private Label predictionValue;
    @FXML private Label currentFillStatusValue;
    @FXML private Label predictionMessageLabel;
    @FXML private Label errorMessageLabel;
    @FXML private TextField binCountField;
    @FXML private TextField initialTemperatureField;
    @FXML private TextField coolingRateField;
    @FXML private VBox binsEditorBox;
    @FXML private VBox routePredictionsBox;
    @FXML private Label routeSummaryLabel;
    @FXML private LineChart<Number, Number> distanceChart;
    @FXML private Pane routeMapPane;

    private final PerceptronCsvLoader csvLoader = new PerceptronCsvLoader();

    private PerceptronDataset trainingDataset;
    private PerceptronDataset testDataset;
    private Path trainingPath;
    private Path testPath;
    private File lastDirectory;
    private PerceptronTrainingResult trainingResult;
    private final List<BinEditorRow> binEditorRows = new java.util.ArrayList<>();

    @FXML
    private void initialize() {
        hideMessage(errorMessageLabel);
        hideMessage(predictionMessageLabel);
        hideMessage(convergenceWarningLabel);
        reading1Field.textProperty().addListener((observable, oldValue, newValue) -> resetPredictionDisplay());
        reading2Field.textProperty().addListener((observable, oldValue, newValue) -> resetPredictionDisplay());
        reading3Field.textProperty().addListener((observable, oldValue, newValue) -> resetPredictionDisplay());
        applyFeatureLabels();
        invalidateModelState();
        createBins();
    }

    @FXML
    private void selectTrainingDataset() {
        Path selectedPath = chooseCsvPath("Select training CSV");
        if (selectedPath == null) {
            return;
        }
        if (selectedPath.equals(testPath)) {
            showError("The training and test datasets must be different files.");
            return;
        }
        PerceptronDataset loadedDataset = loadDataset(selectedPath);
        if (loadedDataset == null) {
            return;
        }
        if (loadedDataset.featureColumns().size() != REQUIRED_FEATURE_COUNT) {
            showError("The training dataset must contain exactly " + REQUIRED_FEATURE_COUNT
                    + " feature columns but contains " + loadedDataset.featureColumns().size() + ".");
            return;
        }
        trainingDataset = loadedDataset;
        trainingPath = selectedPath;
        trainingFileLabel.setText(selectedPath.getFileName().toString());
        trainingSampleCountLabel.setText(loadedDataset.samples().size() + " training samples");
        hideMessage(errorMessageLabel);
        applyFeatureLabels();
        invalidateModelState();
    }

    @FXML
    private void selectTestDataset() {
        Path selectedPath = chooseCsvPath("Select test CSV");
        if (selectedPath == null) {
            return;
        }
        if (selectedPath.equals(trainingPath)) {
            showError("The training and test datasets must be different files.");
            return;
        }
        PerceptronDataset loadedDataset = loadDataset(selectedPath);
        if (loadedDataset == null) {
            return;
        }
        testDataset = loadedDataset;
        testPath = selectedPath;
        testFileLabel.setText(selectedPath.getFileName().toString());
        testSampleCountLabel.setText(loadedDataset.samples().size() + " test samples");
        hideMessage(errorMessageLabel);
        invalidateModelState();
    }

    @FXML
    private void trainAndTest() {
        if (trainingDataset == null || testDataset == null) {
            return;
        }
        if (trainingPath.equals(testPath)) {
            showError("The training and test datasets must be different files.");
            return;
        }
        hideMessage(errorMessageLabel);
        invalidateModelState();

        PerceptronTrainingResult result;
        try {
            result = new PerceptronTrainer(
                    PerceptronTrainer.DEFAULT_LEARNING_RATE,
                    PerceptronTrainer.DEFAULT_MAX_EPOCHS,
                    PerceptronTrainer.PRODUCTION_POSITIVE_CLASS_WEIGHT).train(trainingDataset);
        } catch (IllegalArgumentException exception) {
            showError("Training failed: " + describe(exception));
            workflowStatusLabel.setText("Training failed. Check the training dataset.");
            return;
        }

        trainingResult = result;
        showTrainingOutcome(result);

        PerceptronEvaluationResult evaluation;
        try {
            evaluation = new PerceptronEvaluator().evaluate(result.model(), trainingDataset, testDataset);
        } catch (IllegalArgumentException exception) {
            showError("Evaluation failed: " + describe(exception));
            workflowStatusLabel.setText("Model trained. Evaluation is unavailable for the selected test dataset.");
            return;
        }

        showEvaluation(evaluation);
        workflowStatusLabel.setText("Training and evaluation completed.");
        systemStatusLabel.setText("EVALUATION COMPLETE");
    }

    @FXML
    private void predictSingleBin() {
        hideMessage(predictionMessageLabel);
        if (trainingResult == null) {
            showMessage(predictionMessageLabel, "Train the model before requesting a prediction.");
            return;
        }

        List<String> featureNames = trainingDataset.featureColumns();
        TextField[] fields = {reading1Field, reading2Field, reading3Field};
        double[] inputs = new double[REQUIRED_FEATURE_COUNT];

        for (int index = 0; index < REQUIRED_FEATURE_COUNT; index++) {
            try {
                inputs[index] = parseFiniteValue(fields[index].getText());
            } catch (IllegalArgumentException exception) {
                predictionValue.setText(NO_PREDICTION_TEXT);
                showMessage(predictionMessageLabel,
                        "\"" + featureNames.get(index) + "\" must be a finite decimal number.");
                return;
            }
        }

        int prediction = trainingResult.model().predict(inputs);
        predictionValue.setText(prediction == PICKUP_PREDICTION ? "PICKUP" : "NO PICKUP");
        currentFillStatusValue.setText(String.format(Locale.ROOT, "Current fill: %.2f (dataset value)", inputs[0]));
    }

    @FXML
    private void createBins() {
        int count;
        try {
            count = Integer.parseInt(binCountField.getText().trim());
            if (count <= 0 || count > 100) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            showError("Bin count must be an integer between 1 and 100.");
            return;
        }
        binEditorRows.clear();
        binsEditorBox.getChildren().clear();
        for (int index = 0; index < count; index++) {
            TextField id = new TextField("BIN-" + (index + 1));
            TextField reading1 = new TextField("0.0");
            TextField reading2 = new TextField("0.0");
            TextField reading3 = new TextField("0.0");
            GridPane row = new GridPane();
            row.setHgap(8);
            row.setVgap(4);
            row.add(new Label("Bin " + (index + 1)), 0, 0);
            row.add(id, 1, 0);
            row.add(reading1, 2, 0);
            row.add(reading2, 3, 0);
            row.add(reading3, 4, 0);
            binEditorRows.add(new BinEditorRow(id, reading1, reading2, reading3));
            binsEditorBox.getChildren().add(row);
        }
        routeSummaryLabel.setText("Bins created. Train the model, edit readings, then press Plan Route.");
    }

    @FXML
    private void planRoute() {
        if (trainingResult == null) {
            routeSummaryLabel.setText("Train the perceptron before planning a route.");
            return;
        }
        try {
            double initialTemperature = parsePositive(initialTemperatureField.getText());
            double coolingRate = parseFiniteValue(coolingRateField.getText());
            if (coolingRate <= 0.0 || coolingRate >= 1.0) {
                throw new IllegalArgumentException("Cooling rate must be strictly between 0 and 1.");
            }
            List<WasteBin> bins = new java.util.ArrayList<>();
            java.util.Map<String, RouteLocation> locations = new java.util.LinkedHashMap<>();
            for (int index = 0; index < binEditorRows.size(); index++) {
                BinEditorRow row = binEditorRows.get(index);
                WasteBin bin = new WasteBin(row.id.getText(), parseFiniteValue(row.reading1.getText()),
                        parseFiniteValue(row.reading2.getText()), parseFiniteValue(row.reading3.getText()));
                bins.add(bin);
                double angle = 2.0 * Math.PI * index / Math.max(1, binEditorRows.size());
                locations.put(bin.id(), new RouteLocation(bin.id(), 100.0 * Math.cos(angle), 100.0 * Math.sin(angle)));
            }
            RoutePlanningResult result = new PerceptronRoutePlanningService(
                    new SimulatedAnnealingRouteOptimizer(initialTemperature, coolingRate, 10000,
                            java.util.random.RandomGenerator.getDefault()))
                    .planRoute(trainingResult, new RouteLocation("DEPOT", 0.0, 0.0), bins, locations);
            showRouteResult(result);
        } catch (IllegalArgumentException exception) {
            routeSummaryLabel.setText("Route planning failed: " + describe(exception));
        }
    }

    private void showRouteResult(RoutePlanningResult result) {
        routePredictionsBox.getChildren().clear();
        for (BinPrediction prediction : result.predictions()) {
            routePredictionsBox.getChildren().add(new Label(
                    prediction.bin().id() + ": " + (prediction.needsPickup() ? "PICKUP" : "NO PICKUP")));
        }
        String route = result.optimizationResult().bestRoute().stream()
                .map(RouteLocation::id).collect(java.util.stream.Collectors.joining(" → "));
        routeSummaryLabel.setText(String.format(Locale.ROOT, "DEPOT → %s → DEPOT | Best distance: %.2f m",
                route.isBlank() ? "(no pickup bins)" : route,
                result.optimizationResult().bestDistanceMeters()));
        XYChart.Series<Number, Number> current = new XYChart.Series<>();
        current.setName("Current distance");
        XYChart.Series<Number, Number> best = new XYChart.Series<>();
        best.setName("Best distance");
        List<Double> currentHistory = result.optimizationResult().currentDistanceHistoryMeters();
        List<Double> bestHistory = result.optimizationResult().bestDistanceHistoryMeters();
        for (int index = 0; index < currentHistory.size(); index++) {
            current.getData().add(new XYChart.Data<>(index, currentHistory.get(index)));
            best.getData().add(new XYChart.Data<>(index, bestHistory.get(index)));
        }
        distanceChart.getData().setAll(current, best);
        drawRouteMap(result);
    }

    private void drawRouteMap(RoutePlanningResult result) {
        routeMapPane.getChildren().clear();
        java.util.Map<String, RouteLocation> locations = new java.util.HashMap<>();
        java.util.Set<String> pickupIds = new java.util.HashSet<>();
        for (BinPrediction prediction : result.predictions()) {
            locations.put(prediction.bin().id(), prediction.location());
            if (prediction.needsPickup()) {
                pickupIds.add(prediction.bin().id());
            }
        }
        RouteLocation depot = new RouteLocation("DEPOT", 0.0, 0.0);
        List<RouteLocation> route = new java.util.ArrayList<>();
        route.add(depot);
        route.addAll(result.optimizationResult().bestRoute());
        route.add(depot);
        double minX = route.stream().mapToDouble(RouteLocation::xMeters).min().orElse(0.0);
        double maxX = route.stream().mapToDouble(RouteLocation::xMeters).max().orElse(1.0);
        double minY = route.stream().mapToDouble(RouteLocation::yMeters).min().orElse(0.0);
        double maxY = route.stream().mapToDouble(RouteLocation::yMeters).max().orElse(1.0);
        double width = Math.max(1.0, maxX - minX);
        double height = Math.max(1.0, maxY - minY);
        double scale = Math.min(560.0 / width, 260.0 / height);
        double offsetX = 40.0 - minX * scale;
        double offsetY = 30.0 + maxY * scale;
        for (int index = 0; index < route.size() - 1; index++) {
            RouteLocation from = route.get(index);
            RouteLocation to = route.get(index + 1);
            Line line = new Line(offsetX + from.xMeters() * scale, offsetY - from.yMeters() * scale,
                    offsetX + to.xMeters() * scale, offsetY - to.yMeters() * scale);
            line.getStyleClass().add("route-line");
            routeMapPane.getChildren().add(line);
        }
        for (BinPrediction prediction : result.predictions()) {
            if (pickupIds.contains(prediction.bin().id())) {
                continue;
            }
            RouteLocation location = prediction.location();
            double x = offsetX + location.xMeters() * scale;
            double y = offsetY - location.yMeters() * scale;
            Circle marker = new Circle(x, y, 4.0);
            marker.getStyleClass().add("other-marker");
            routeMapPane.getChildren().add(marker);
        }
        for (RouteLocation location : route.subList(0, Math.max(1, route.size() - 1))) {
            double x = offsetX + location.xMeters() * scale;
            double y = offsetY - location.yMeters() * scale;
            Circle marker = new Circle(x, y, location.id().equals("DEPOT") ? 7.0 : 5.0);
            marker.getStyleClass().add(location.id().equals("DEPOT") ? "depot-marker" : "pickup-marker");
            Label label = new Label(location.id());
            label.setLayoutX(x + 8.0);
            label.setLayoutY(y - 12.0);
            routeMapPane.getChildren().addAll(marker, label);
        }
    }

    private double parsePositive(String text) {
        double value = parseFiniteValue(text);
        if (value <= 0.0) {
            throw new IllegalArgumentException("Value must be greater than zero.");
        }
        return value;
    }

    private record BinEditorRow(TextField id, TextField reading1, TextField reading2, TextField reading3) {}

    private Path chooseCsvPath(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        File selectedFile = chooser.showOpenDialog(trainAndTestButton.getScene().getWindow());
        if (selectedFile == null) {
            return null;
        }
        lastDirectory = selectedFile.getParentFile();
        return selectedFile.toPath().toAbsolutePath().normalize();
    }

    private PerceptronDataset loadDataset(Path path) {
        try {
            return csvLoader.load(path);
        } catch (IOException | IllegalArgumentException exception) {
            showError("Could not load " + path.getFileName() + ": " + describe(exception));
            return null;
        }
    }

    private void invalidateModelState() {
        trainingResult = null;
        predictButton.setDisable(true);
        predictionValue.setText(NO_PREDICTION_TEXT);
        hideMessage(predictionMessageLabel);
        hideMessage(convergenceWarningLabel);

        accuracyValue.setText(PLACEHOLDER);
        missedPickupsValue.setText(PLACEHOLDER);
        falseAlertsValue.setText(PLACEHOLDER);
        testSamplesValue.setText(PLACEHOLDER);
        truePositivesValue.setText(PLACEHOLDER);
        trueNegativesValue.setText(PLACEHOLDER);
        falsePositivesValue.setText(PLACEHOLDER);
        falseNegativesValue.setText(PLACEHOLDER);
        currentFillStatusValue.setText("Current fill: --");
        epochsValue.setText("Epochs: " + PLACEHOLDER);
        convergenceValue.setText("Training status: " + PLACEHOLDER);

        modelStatusLabel.setText("NOT TRAINED");
        perceptronOverviewStatusLabel.setText("NOT TRAINED");
        setReadyStyle(modelStatusLabel, "pending-tag-ready", false);
        setReadyStyle(perceptronOverviewStatusLabel, "pending-tag-ready", false);

        boolean bothSelected = trainingDataset != null && testDataset != null;
        trainAndTestButton.setDisable(!bothSelected);
        if (bothSelected) {
            workflowStatusLabel.setText("Both datasets are loaded. Press Train and Test.");
            systemStatusLabel.setText("DATASETS LOADED");
            setReadyStyle(systemStatusLabel, "status-pill-ready", true);
        } else {
            workflowStatusLabel.setText("Select both CSV files to begin.");
            systemStatusLabel.setText("WAITING FOR DATA");
            setReadyStyle(systemStatusLabel, "status-pill-ready", false);
        }
    }

    private void showTrainingOutcome(PerceptronTrainingResult result) {
        predictButton.setDisable(false);
        modelStatusLabel.setText("MODEL TRAINED");
        perceptronOverviewStatusLabel.setText("MODEL TRAINED");
        setReadyStyle(modelStatusLabel, "pending-tag-ready", true);
        setReadyStyle(perceptronOverviewStatusLabel, "pending-tag-ready", true);
        systemStatusLabel.setText("MODEL TRAINED");
        setReadyStyle(systemStatusLabel, "status-pill-ready", true);

        epochsValue.setText("Epochs: " + result.epochsCompleted());
        if (result.converged()) {
            convergenceValue.setText("Training status: Converged");
            hideMessage(convergenceWarningLabel);
        } else {
            convergenceValue.setText("Training status: Not converged");
            showMessage(convergenceWarningLabel,
                    "Warning: training reached the maximum epoch limit without converging. "
                            + "The trained model is still available, but it did not reach a stable solution.");
        }
    }

    private void showEvaluation(PerceptronEvaluationResult evaluation) {
        accuracyValue.setText(String.format(Locale.ROOT, "%.2f%%", evaluation.accuracy() * 100.0));
        missedPickupsValue.setText(Integer.toString(evaluation.falseNegatives()));
        falseAlertsValue.setText(Integer.toString(evaluation.falsePositives()));
        testSamplesValue.setText(Integer.toString(evaluation.totalSamples()));
        truePositivesValue.setText(Integer.toString(evaluation.truePositives()));
        trueNegativesValue.setText(Integer.toString(evaluation.trueNegatives()));
        falsePositivesValue.setText(Integer.toString(evaluation.falsePositives()));
        falseNegativesValue.setText(Integer.toString(evaluation.falseNegatives()));
    }

    private void applyFeatureLabels() {
        Label[] labels = {reading1Label, reading2Label, reading3Label};
        for (int index = 0; index < REQUIRED_FEATURE_COUNT; index++) {
            if (trainingDataset == null) {
                labels[index].setText("Reading " + (index + 1));
            } else {
                labels[index].setText(trainingDataset.featureColumns().get(index));
            }
        }
    }

    private void resetPredictionDisplay() {
        predictionValue.setText(NO_PREDICTION_TEXT);
        hideMessage(predictionMessageLabel);
    }

    private double parseFiniteValue(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (!DECIMAL_NUMBER.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Not a decimal number");
        }
        double value = Double.parseDouble(trimmed);
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Not a finite number");
        }
        return value;
    }

    private void showError(String message) {
        showMessage(errorMessageLabel, message);
    }

    private void showMessage(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideMessage(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }

    private void setReadyStyle(Label label, String styleClass, boolean ready) {
        label.getStyleClass().remove(styleClass);
        if (ready) {
            label.getStyleClass().add(styleClass);
        }
    }

    private String describe(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}
