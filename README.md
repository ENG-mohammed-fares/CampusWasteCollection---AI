# Campus Waste Collection AI

An intelligent university waste collection application intended to help collection teams determine which waste bins need collection and identify efficient collection routes.

> **Project status:** early stage. Only a JavaFX dashboard skeleton currently exists. The data, AI, and optimization features described under [Planned Work](#planned-work) are **not implemented yet**.

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

- A JavaFX dashboard skeleton (`MainApp`, `dashboard.fxml`, `dashboard.css`).
- A Maven build configured for Java 21.
- JUnit 5, Apache Commons CSV, and JavaFX configured as dependencies.
- GitHub Actions and Dependabot configuration files.
- The dashboard has been launched successfully on the owner's machine, and `mvn -B clean verify` completed with `BUILD SUCCESS` locally.

### What is not yet verified or implemented

CSV import, waste-bin data models, Perceptron training or prediction, Simulated Annealing, route optimization, reporting, and data persistence are **not implemented**. The package directories for these areas exist, but they currently contain no implementation files. No datasets are included in the repository.

## Technology Stack

| Technology | Version | Role in this project |
|---|---|---|
| Java | 21 | Implementation language and build target (`release` 21). |
| JavaFX Controls and FXML | 21.0.6 | Desktop user interface. The dashboard layout is defined in FXML and styled with CSS. |
| Apache Commons CSV | 1.13.0 | Configured for reading CSV data. No CSV loading code exists yet. |
| JUnit Jupiter (JUnit 5) | 5.11.4 | Unit testing framework. Configured, with no tests written yet. |
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
          MainApp.java        JavaFX application entry point
        data/                 Planned: data loading (no files yet)
        model/                Planned: domain model (no files yet)
        optimization/         Planned: route optimization (no files yet)
        perceptron/           Planned: collection-need prediction (no files yet)
        service/              Planned: application services (no files yet)
        ui/                   Planned: UI components (no files yet)
      org/example/
        Main.java             Additional source file in the org.example package
    resources/
      css/
        dashboard.css         Dashboard styling
      data/                   Reserved for datasets (currently empty)
      fxml/
        dashboard.fxml        Dashboard layout
  test/
    java/
      com/campuswaste/
        data/                 Test package (no tests yet)
        optimization/         Test package (no tests yet)
        perceptron/           Test package (no tests yet)
        service/              Test package (no tests yet)
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

The project uses the JavaFX Maven Plugin, with `com.campuswaste.app.MainApp` as the main class.

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

- JUnit 5 is configured in `pom.xml`.
- **No unit tests have been implemented or discovered yet.**
- `mvn -B clean verify` has completed successfully on the owner's machine. Because no test source files exist, no tests were executed, so this result does not show that any tests pass.

Tests will be added together with the first data, model, and algorithm implementations.

## Continuous Integration

A GitHub Actions workflow is defined in `.github/workflows/java-ci.yml`. It:

- runs on pushes to `main` and on pull requests targeting `main`, when `pom.xml`, files under `src/`, or Maven wrapper files change;
- runs on Ubuntu;
- sets up Temurin Java 21;
- caches Maven dependencies;
- runs `mvn -B clean verify`;
- uses read-only `contents` permission.

Dependabot (`.github/dependabot.yml`) checks GitHub Actions dependencies weekly.

The workflow is configured, but this README makes no claim about the results of any CI run. The local Maven build is not a GitHub Actions run.

## Engineering Practices

- Give public classes and meaningful public methods useful JavaDoc.
- Add meaningful JUnit 5 unit tests for new non-UI business logic.
- Do not write placeholder or fake tests, and do not unit-test JavaFX layout appearance.
- Validate external input, such as CSV values, before using it.
- Never hardcode credentials, API keys, or tokens.
- Keep secrets out of Git. If credentials ever become necessary, use environment variables or local configuration kept outside version control.
- Keep UI code separate from business logic.
- Run `clean verify` before pushing changes.

## Planned Work

> Everything in this section is **planned and not implemented**.

- [ ] CSV data loading and validation
- [ ] Waste-bin data model
- [ ] Perceptron-based collection-need prediction
- [ ] Simulated Annealing route optimization
- [ ] Integration of these services with the JavaFX dashboard