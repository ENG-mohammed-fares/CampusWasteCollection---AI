$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

$javaHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.10002-hotspot"
if (Test-Path (Join-Path $javaHome "bin\java.exe")) {
    $env:JAVA_HOME = $javaHome
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
}

$mvnCmd = "D:\Intellij\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd"
if (-not (Test-Path $mvnCmd)) {
    throw "Maven executable was not found at: $mvnCmd"
}

$dataDir = Join-Path $projectRoot "src\main\resources\data"
$trainingFiles = @(Get-ChildItem -LiteralPath $dataDir -Filter "waste_bin_perceptron_train*.csv" -File)
$testFiles = @(Get-ChildItem -LiteralPath $dataDir -Filter "waste_bin_perceptron_test*.csv" -File)

if ($trainingFiles.Count -ne 1) {
    throw "Expected exactly one training CSV in $dataDir. Found: $($trainingFiles.Count)"
}
if ($testFiles.Count -ne 1) {
    throw "Expected exactly one test CSV in $dataDir. Found: $($testFiles.Count)"
}

Write-Host "========== BUILD AND UNIT TESTS ==========" -ForegroundColor Cyan
& $mvnCmd -B clean verify
if ($LASTEXITCODE -ne 0) {
    throw "Maven verification failed. Training was not started."
}

$classpathFile = Join-Path $projectRoot "target\perceptron-classpath.txt"
& $mvnCmd -q "-Dmdep.outputFile=$classpathFile" "-Dmdep.includeScope=runtime" dependency:build-classpath
if ($LASTEXITCODE -ne 0) {
    throw "Could not build the runtime dependency classpath."
}

$dependencyClasspath = (Get-Content -LiteralPath $classpathFile -Raw).Trim()
$runtimeClasspath = "target\classes;$dependencyClasspath"
$runnerPath = Join-Path $projectRoot "target\PerceptronCli.java"

$runnerSource = @'
import com.campuswaste.data.PerceptronCsvLoader;
import com.campuswaste.model.PerceptronDataset;
import com.campuswaste.perceptron.PerceptronEvaluationResult;
import com.campuswaste.perceptron.PerceptronEvaluator;
import com.campuswaste.perceptron.PerceptronTrainer;
import com.campuswaste.perceptron.PerceptronTrainingResult;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

public class PerceptronCli {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Provide training and test CSV paths.");
        }

        Path trainingPath = Path.of(args[0]).toAbsolutePath().normalize();
        Path testPath = Path.of(args[1]).toAbsolutePath().normalize();
        if (trainingPath.equals(testPath)) {
            throw new IllegalArgumentException("Training and test files must be different.");
        }

        PerceptronCsvLoader loader = new PerceptronCsvLoader();
        PerceptronDataset training = loader.load(trainingPath);
        PerceptronDataset testing = loader.load(testPath);
        if (!training.featureColumns().equals(testing.featureColumns())) {
            throw new IllegalArgumentException("Training and test feature columns do not match.");
        }

        System.out.println("TRAINING_SAMPLES=" + training.samples().size());
        System.out.println("TEST_SAMPLES=" + testing.samples().size());
        System.out.println("FEATURES=" + training.featureColumns());

        PerceptronTrainingResult trained = new PerceptronTrainer(
                PerceptronTrainer.DEFAULT_LEARNING_RATE,
                PerceptronTrainer.DEFAULT_MAX_EPOCHS,
                PerceptronTrainer.PRODUCTION_POSITIVE_CLASS_WEIGHT).train(training);
        System.out.println("MODEL_STATUS=TRAINED");
        System.out.println("CONVERGED=" + trained.converged());
        System.out.println("EPOCHS=" + trained.epochsCompleted());
        System.out.println("WEIGHTS=" + Arrays.toString(trained.model().weights()));
        System.out.println("BIAS=" + trained.model().bias());

        PerceptronEvaluationResult evaluation = new PerceptronEvaluator().evaluate(
                trained.model(), training, testing);
        System.out.printf(Locale.ROOT, "ACCURACY=%.2f%%%n", evaluation.accuracy() * 100.0);
        System.out.printf(Locale.ROOT, "PRECISION=%.2f%%%n", evaluation.precision() * 100.0);
        System.out.printf(Locale.ROOT, "RECALL=%.2f%%%n", evaluation.recall() * 100.0);
        System.out.printf(Locale.ROOT, "F1=%.4f%n", evaluation.f1Score());
        System.out.println("TRUE_POSITIVES=" + evaluation.truePositives());
        System.out.println("TRUE_NEGATIVES=" + evaluation.trueNegatives());
        System.out.println("FALSE_POSITIVES=" + evaluation.falsePositives());
        System.out.println("FALSE_NEGATIVES=" + evaluation.falseNegatives());
        System.out.println("EVALUATION_STATUS=COMPLETED");
    }
}
'@

[System.IO.File]::WriteAllText($runnerPath, $runnerSource, [System.Text.UTF8Encoding]::new($false))
& javac -encoding UTF-8 -cp $runtimeClasspath -d target\classes $runnerPath
if ($LASTEXITCODE -ne 0) {
    throw "The command-line training runner could not be compiled."
}

Write-Host "========== TRAINING AND TEST EVALUATION ==========" -ForegroundColor Cyan
$trainingPath = $trainingFiles[0].FullName
$testPath = $testFiles[0].FullName
& java -cp $runtimeClasspath PerceptronCli "$trainingPath" "$testPath"
if ($LASTEXITCODE -ne 0) {
    throw "Perceptron training or evaluation failed."
}

Write-Host "PERCEPTRON TRAINING AND EVALUATION COMPLETED" -ForegroundColor Green
