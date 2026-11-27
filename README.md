# Campus Waste Collection AI

JavaFX application for the Campus Waste Collection assignment. It trains a three-input Perceptron to predict which bins need pickup, evaluates it on a separate test CSV, and plans a depot-returning route through bins predicted as `Pickup` using Simulated Annealing.

## Implemented assignment requirements

### Part 1 — Perceptron

- Three input weights and one bias.
- Labels `+1` (`Pickup`) and `-1` (`No pickup`).
- Separate training and test CSV selection.
- `Train and Test` workflow.
- Test accuracy, missed pickups, false pickup alerts, confusion-matrix counts, epochs, and convergence status.
- Production training uses a positive-class update weight of `110.0` to prioritize detecting pickup cases; the value is defined as `PerceptronTrainer.PRODUCTION_POSITIVE_CLASS_WEIGHT`.
- Manual entry of the three readings for one bin.
- `Pickup` / `No pickup` prediction.
- Current fill value shown from the first reading. The assignment does not define categorical fill thresholds, so the UI displays the supplied dataset value rather than inventing thresholds.

### Part 2 — Simulated Annealing

- Route Planning tab with a default bin count of `10`.
- `Create Bins` creates editable bin IDs and three readings.
- `Plan Route` predicts every bin and routes only bins predicted as `Pickup`.
- Route starts and ends at `DEPOT`.
- Initial temperature field, default `60`.
- Cooling rate field, default `0.997`.
- Best route, route distance, and each bin prediction are displayed.
- A visual route map connects the depot and pickup containers.
- Line chart showing current distance and best distance against iteration number.

## Data files

The project includes the supplied datasets under `src/main/resources/data`:

- `waste_bin_perceptron_train.csv`
- `waste_bin_perceptron_test.csv`

Each file has three numeric feature columns and a `pickup_label` column. Route-planning bins are entered through the UI because the assignment asks the user to create and edit a bin count.

## Technology

- Java 21
- JavaFX 21.0.6
- Apache Commons CSV 1.13.0
- JUnit Jupiter 5.11.4
- Maven 3.9.x

## Run in IntelliJ IDEA

1. Open the folder containing `pom.xml`.
2. Import it as a Maven project.
3. Select JDK 21 as the project SDK.
4. Run `com.campuswaste.app.MainApp`.
5. In the Perceptron tab, choose the training and test CSV files and press `Train and Test`.
6. Open Route Planning, create the bins, edit their readings, and press `Plan Route`.

The JavaFX Maven command is:

```bash
mvn javafx:run
```

## Build and test

```bash
mvn -B clean verify
```

The project also contains `run-perceptron.ps1`, which builds the project and runs a command-line training/evaluation summary when Maven is available at the configured path.

## Project structure

```text
src/main/java/com/campuswaste/
  app/           JavaFX entry point
  data/          CSV loading and validation
  model/         Bins, predictions, locations, and route results
  perceptron/    Model, trainer, evaluator, and metrics
  optimization/ Simulated Annealing and route distance calculation
  service/       Perceptron-to-route integration
  ui/            JavaFX controller
src/main/resources/
  data/          Training and test CSV files
  fxml/          Dashboard layout
  css/           Dashboard styling
src/test/java/   Unit tests for data, model, Perceptron, routing, and optimization
```

## Notes

- Test accuracy is calculated only from the selected test dataset.
- Missed pickups are false negatives; false pickup alerts are false positives.
- The route optimizer is stochastic by default. Its backend also supports an injected random generator for deterministic tests.
- The JavaFX window requires a graphical environment; headless CI runs the build and unit tests only.
