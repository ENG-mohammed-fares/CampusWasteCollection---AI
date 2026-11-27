# Campus Waste Collection AI

A university waste collection application intended to help collection teams determine which waste bins need collection and identify efficient collection routes.

> **Project status:** early stage. The JavaFX dashboard is a user-interface skeleton. The repository also contains tested foundations for CSV loading, domain models, a Perceptron classifier, and evaluation. These components are **not yet integrated into the dashboard**, and route optimization is **not implemented**. See [Planned Work](#planned-work).

## Table of Contents

1. [Current Project Status](#current-project-status)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Prerequisites](#prerequisites)
5. [Opening the Project in IntelliJ IDEA](#opening-the-project-in-intellij-idea)
6. [Build and Test](#build-and-test)
7. [Running the JavaFX Application](#running-the-javafx-application)
8. [Testing Status](#testing-status)
9. [Continuous Integration](#continuous-integration)
10. [Engineering Practices](#engineering-practices)
11. [Planned Work](#planned-work)

## Current Project Status

### What currently exists

- **JavaFX dashboard skeleton** (`MainApp`, `dashboard.fxml`, `dashboard.css`). It shows placeholder state such as "No dataset loaded" and "NOT TRAINED". It is not connected to the data or Perceptron code below.
- **CSV loading and validation** (`PerceptronCsvLoader`). It reads a caller-supplied CSV file with exactly four columns: three numeric feature columns and a `pickup_label` column.
  - `pickup_label` may appear in any column position.
  - Labels must be `+1` (Pickup) or `-1` (No pickup).
  - Feature values must be finite numbers.
  - Any invalid row fails the whole load with an `IOException` that names the file, record number, and column.
- **Domain models:**
  - `WasteBin`: an immutable bin identifier with three finite readings.
  - `PerceptronTrainingSample`: three finite readings and a `+1` or `-1` label.
  - `PerceptronDataset`: three feature-column names and a non-empty list of training samples.
- **Perceptron classifier** with three input weights and one bias:
  - `PerceptronModel`: immutable model that predicts `+1` when the activation is greater than or equal to zero, and `-1` otherwise.
  - `PerceptronTrainer`: deterministic training from zero weights and zero bias, in dataset order, with no shuffling and no feature normalization. The defaults are a learning rate of 1.0 and at most 1000 epochs, and both are configurable. Training stops early when an epoch makes no updates.
  - `PerceptronTrainingResult`: the trained model, the number of epochs completed, and whether training converged.
- **Evaluation** (`PerceptronEvaluator`, `PerceptronEvaluationResult`): evaluates a trained model on a separate test dataset and reports a confusion matrix with accuracy, precision, recall, and F1 score. The evaluator verifies that the training and test feature columns match and uses only the test samples for the counts.
- A Maven build configured for Java 21, plus GitHub Actions and Dependabot configuration.

### What is not implemented

- **No real campus dataset is supplied** with the repository.
- The CSV, model, Perceptron, and evaluation code is **not integrated into the JavaFX dashboard**.
- **Simulated Annealing**, route optimization, route display, reporting, data persistence, and end-to-end application integration are **not implemented**.
- The `optimization`, `service`, and `ui` packages contain no implementation files.
- This README makes no claim about prediction accuracy or route quality on any dataset.

## Technology Stack

| Technology | Version | Role in this project |
|---|---|---|
| Java | 21 | Implementation language and build target (`release` 21). |
| JavaFX Controls and FXML | 21.0.6 | Desktop user interface. The dashboard layout is defined in FXML and styled with CSS. |
| Apache Commons CSV | 1.13.0 | Used by `PerceptronCsvLoader` to parse CSV files. |
| JUnit Jupiter (JUnit 5) | 5.11.4 | Unit testing framework, used by the existing test suite. |
| Maven | 3.9.x | Build and dependency management. |

Maven plugins in use: Maven Compiler Plugin 3.13.0, Maven Surefire Plugin 3.5.2, and JavaFX Maven Plugin 0.0.8.

**Maven coordinates:** `com.campuswaste:campus-waste-collection:1.0-SNAPSHOT`

## Project Structure

```text
src/
  main/
    java/
      com/campuswaste/
        app/
          MainApp.java                      JavaFX application entry point
        data/
          PerceptronCsvLoader.java          CSV loading and validation
        model/
          WasteBin.java                     Bin identifier with three readings
          PerceptronTrainingSample.java     Three readings and a +1/-1 label
          PerceptronDataset.java            Feature names and training samples
        perceptron/
          PerceptronModel.java              Three weights, one bias, prediction
          PerceptronTrainer.java            Perceptron training
          PerceptronTrainingResult.java     Trained model and training outcome
          PerceptronEvaluator.java          Test-set evaluation
          PerceptronEvaluationResult.java   Confusion matrix and metrics
        optimization/                       Planned (no files yet)
        service/                            Planned (no files yet)
        ui/                                 Planned (no files yet)
    resources/
      css/
        dashboard.css                       Dashboard styling
      data/                                 Reserved for datasets (none supplied)
      fxml/
        dashboard.fxml                      Dashboard layout
  test/
    java/
      com/campuswaste/
        data/
          PerceptronCsvLoaderTest.java
        model/
          WasteBinTest.java
          PerceptronTrainingSampleTest.java
          PerceptronDatasetTest.java
        perceptron/
          PerceptronModelTest.java
          PerceptronTrainerTest.java
          PerceptronEvaluatorTest.java
          PerceptronEvaluationResultTest.java
```

Repository configuration: `.github/workflows/java-ci.yml`, `.github/dependabot.yml`, and `.gitignore`.

## Prerequisites

- **JDK 21** (the project is developed with Eclipse Temurin 21).
- One of the following:
  - **IntelliJ IDEA** with Maven support (Maven is bundled with IntelliJ IDEA), or
  - **Maven 3.9.x** installed separately and available on your `PATH`.

## Opening the Project in IntelliJ IDEA

1. Start IntelliJ IDEA and choose **File > Open**.
2. Select the project folder (the folder containing `pom.xml`), or select the `pom.xml` file itself.
3. If prompted, choose **Open as Project** and trust the project.
4. Wait for IntelliJ to import the Maven project and download dependencies.
5. Check that the project SDK is set to JDK 21 (**File > Project Structure > Project**).

## Build and Test

### IntelliJ IDEA (recommended)

1. Open the **Maven** tool window (**View > Tool Windows > Maven**).
2. Expand the project, then **Lifecycle**.
3. Double-click **clean**.
4. Double-click **verify**.

### Command line

Only use this where Maven is available on your `PATH`:

```bash
mvn -B clean verify
```

## Running the JavaFX Application

The project uses the JavaFX Maven Plugin, with `com.campuswaste.app.MainApp` as the main class. The dashboard is a skeleton: it does not yet load data, train the Perceptron, or plan routes.

### IntelliJ IDEA

1. Open the **Maven** tool window.
2. Expand the project, then **Plugins > javafx**.
3. Double-click **javafx:run**.

### Command line

Only use this where Maven is available on your `PATH`:

```bash
mvn javafx:run
```

## Testing Status

Eight JUnit 5 test classes cover the CSV loader, the domain models, the Perceptron model, the trainer, and the evaluator. Tests build their data in code or in temporary files and do not depend on real datasets. The JavaFX dashboard layout is intentionally not unit-tested.

**Recorded verification result:** the latest local run of `mvn -B clean verify` completed with `BUILD SUCCESS`: 264 tests run, 0 failures, 0 errors, 0 skipped. This is a record of that run, not a permanent count. It will change as the code and tests change.

## Continuous Integration

A GitHub Actions workflow is defined in `.github/workflows/java-ci.yml`. It:

- runs on pushes to `main` and on pull requests targeting `main`, when `pom.xml`, files under `src/`, or Maven wrapper files change;
- runs on Ubuntu;
- sets up Temurin Java 21;
- caches Maven dependencies;
- runs `mvn -B clean verify`;
- uses read-only `contents` permission.

Dependabot (`.github/dependabot.yml`) checks GitHub Actions dependencies weekly.

The workflow runs headless Maven verification only and does not launch the JavaFX window. The Java CI run for the Perceptron foundation pull request (#6) completed successfully when it was merged. This is a point-in-time result, not a guarantee for later commits.

## Engineering Practices

- Keep public APIs clear through descriptive names and meaningful tests.
- Add meaningful JUnit 5 unit tests for new non-UI business logic.
- Do not write placeholder or fake tests, and do not unit-test JavaFX layout appearance.
- Validate external input, such as CSV values, before using it.
- Never hardcode credentials, API keys, or tokens.
- Keep secrets out of Git. If credentials ever become necessary, use environment variables or local configuration kept outside version control.
- Keep UI code separate from business logic.
- Run `clean verify` before pushing changes.

## Planned Work

> Everything in this section is **planned and not implemented**. The existing CSV, model, Perceptron, and evaluation code provides foundations for some of these items but does not complete them.

- [ ] Integration of the project's real dataset files into the application
- [ ] Integration of the CSV loader, Perceptron, and evaluation with the JavaFX dashboard
- [ ] Simulated Annealing route optimization
- [ ] Route display in the dashboard
- [ ] Reporting
- [ ] Data persistence
- [ ] End-to-end application integration