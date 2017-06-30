package de.dfki.lt.mdparser;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import de.bwaldvogel.liblinear.Linear;
import de.dfki.lt.mdparser.caller.MDPrunner;
import de.dfki.lt.mdparser.config.ConfigKeys;
import de.dfki.lt.mdparser.config.GlobalConfig;
import de.dfki.lt.mdparser.eval.Eval;
import de.dfki.lt.mdparser.parser.Trainer;


public class TestMDParser {

  @Before
  public void setUp()
      throws IOException {

    Utils.deleteFolder(GlobalConfig.getModelBuildFolder());
    List<Path> filesToDelete = Utils.getAllFilesFromFolder(Paths.get("src/test/resources"), "*.zip");
    for (Path oneFileToDelete : filesToDelete) {
      Files.delete(oneFileToDelete);
    }
    Linear.resetRandom();
  }


  @Test
  public void testTrainEvalFilesCovington()
      throws IOException, InterruptedException {

    String trainingMode = "files";
    String algorithmId = "covington";
    String modelName = "de-2009-" + algorithmId + ".zip";

    double expectedParentAccuracy = 0.841186515716906;
    double expectedLabelAccuracy = 0.8006767440389602;

    GlobalConfig.getInstance().setProperty(ConfigKeys.TRAINING_MODE, trainingMode);
    GlobalConfig.getInstance().setProperty(ConfigKeys.ALGORITHM, algorithmId);
    // parallel training is not deterministic, so restrict number of threads to 1
    GlobalConfig.getInstance().setProperty(ConfigKeys.TRAINING_THREADS, 1);

    testTrainFiles(modelName, algorithmId);
    // for some reason the model archive is not immediately available in the file system, so we wait a moment
    TimeUnit.SECONDS.sleep(5);
    testEval(modelName, expectedParentAccuracy, expectedLabelAccuracy);
  }


  @Test
  public void testTrainEvalFilesStack()
      throws IOException, InterruptedException {

    String trainingMode = "files";
    String algorithmId = "stack";
    String modelName = "de-2009-" + algorithmId + ".zip";

    double expectedParentAccuracy = 0.8100056922395801;
    double expectedLabelAccuracy = 0.7698437796470812;

    GlobalConfig.getInstance().setProperty(ConfigKeys.TRAINING_MODE, trainingMode);
    GlobalConfig.getInstance().setProperty(ConfigKeys.ALGORITHM, algorithmId);
    // parallel training is not deterministic, so restrict number of threads to 1
    GlobalConfig.getInstance().setProperty(ConfigKeys.TRAINING_THREADS, 1);

    testTrainFiles(modelName, algorithmId);
    // for some reason the model archive is not immediately available in the file system, so we wait a moment
    TimeUnit.SECONDS.sleep(5);
    testEval(modelName, expectedParentAccuracy, expectedLabelAccuracy);
  }


  private void testTrainFiles(String modelName, String algorithmId)
      throws IOException {

    Trainer.train("src/test/resources/corpora/de-train-2009.conll", modelName);

    assertThat(GlobalConfig.getPath(ConfigKeys.MODEL_OUTPUT_FOLDER).resolve(modelName)).exists();

    compareFolders(
        GlobalConfig.SPLIT_ALPHA_FOLDER,
        Paths.get("src/test/resources/expected/file-" + algorithmId + "/split_alphas"));
    compareFolders(
        GlobalConfig.FEATURE_VECTORS_FOLDER,
        Paths.get("src/test/resources/expected/file-" + algorithmId + "/1_initial_feature_vectors"));
    compareFolders(
        GlobalConfig.SPLIT_INITIAL_FOLDER,
        Paths.get("src/test/resources/expected/file-" + algorithmId + "/2_initial_splits"));
    compareFolders(
        GlobalConfig.SPLIT_ADJUST_FOLDER,
        Paths.get("src/test/resources/expected/file-" + algorithmId + "/3_adjusted_splits"));
    compareFolders(
        GlobalConfig.SPLIT_COMPACT_FOLDER,
        Paths.get("src/test/resources/expected/file-" + algorithmId + "/4_compacted_splits"));
    compareFolders(
        GlobalConfig.SPLIT_MODELS_FOLDER,
        Paths.get("src/test/resources/expected/file-" + algorithmId + "/split_models"));
    assertThat(GlobalConfig.ALPHA_FILE).usingCharset(StandardCharsets.UTF_8)
        .hasSameContentAs(Paths.get("src/test/resources/expected/file-" + algorithmId + "/alpha.txt"),
            StandardCharsets.UTF_8);
    assertThat(GlobalConfig.SPLIT_FILE).usingCharset(StandardCharsets.UTF_8)
        .hasSameContentAs(Paths.get("src/test/resources/expected/file-" + algorithmId + "/split.txt"),
            StandardCharsets.UTF_8);
  }


  @Test
  public void testTrainEvalMemoryCovington()
      throws IOException, InterruptedException {

    String trainingMode = "memory";
    String algorithmId = "covington";
    String modelName = "de-2009-" + algorithmId + ".zip";

    double expectedParentAccuracy = 0.8452343305293782;
    double expectedLabelAccuracy = 0.8051672885965467;

    GlobalConfig.getInstance().setProperty(ConfigKeys.TRAINING_MODE, trainingMode);
    GlobalConfig.getInstance().setProperty(ConfigKeys.ALGORITHM, algorithmId);

    testTrainMemory(modelName, algorithmId);
    // for some reason the model archive is not immediately available in the file system, so we wait a moment
    TimeUnit.SECONDS.sleep(5);
    testEval(modelName, expectedParentAccuracy, expectedLabelAccuracy);
  }


  @Test
  public void testTrainEvalMemoryStack()
      throws IOException, InterruptedException {

    String trainingMode = "memory";
    String algorithmId = "stack";
    String modelName = "de-2009-" + algorithmId + ".zip";

    double expectedParentAccuracy = 0.8104800455379166;
    double expectedLabelAccuracy = 0.7700967680728606;

    GlobalConfig.getInstance().setProperty(ConfigKeys.TRAINING_MODE, trainingMode);
    GlobalConfig.getInstance().setProperty(ConfigKeys.ALGORITHM, algorithmId);

    testTrainMemory(modelName, algorithmId);
    // for some reason the model archive is not immediately available in the file system, so we wait a moment
    TimeUnit.SECONDS.sleep(5);
    testEval(modelName, expectedParentAccuracy, expectedLabelAccuracy);
  }


  private void testTrainMemory(String modelName, String algorithmId)
      throws IOException {

    Trainer.train("src/test/resources/corpora/de-train-2009.conll", modelName);

    assertThat(GlobalConfig.getPath(ConfigKeys.MODEL_OUTPUT_FOLDER).resolve(modelName)).exists();

    compareFolders(
        GlobalConfig.SPLIT_ALPHA_FOLDER,
        Paths.get("src/test/resources/expected/memory-" + algorithmId + "/split_alphas"));
    compareFolders(
        GlobalConfig.SPLIT_MODELS_FOLDER,
        Paths.get("src/test/resources/expected/memory-" + algorithmId + "/split_models"));
    assertThat(GlobalConfig.ALPHA_FILE).usingCharset(StandardCharsets.UTF_8)
        .hasSameContentAs(Paths.get("src/test/resources/expected/memory-" + algorithmId + "/alpha.txt"),
            StandardCharsets.UTF_8);
    assertThat(GlobalConfig.SPLIT_FILE).usingCharset(StandardCharsets.UTF_8)
        .hasSameContentAs(Paths.get("src/test/resources/expected/memory-" + algorithmId + "/split.txt"),
            StandardCharsets.UTF_8);
  }


  private void testEval(String modelName, double expectedParentAccuracy, double expectedLabelAccuracy)
      throws IOException {

    Eval evaluator = MDPrunner.parseAndEvalConllFile(
        "src/test/resources/corpora/de-test-2009.conll",
        "src/test/resources/corpora/de-result-2009.conll",
        modelName);
    assertThat(evaluator.getParentsAccuracy()).isEqualTo(expectedParentAccuracy);
    assertThat(evaluator.getLabelsAccuracy()).isEqualTo(expectedLabelAccuracy);
  }


  private static void compareFolders(Path testPath, Path expectedPath)
      throws IOException {

    List<Path> filesCreatedByTest = Utils.getAllFilesFromFolder(testPath, "*");
    List<Path> expectedFiles = Utils.getAllFilesFromFolder(expectedPath, "*");

    assertThat(filesCreatedByTest).hasSameSizeAs(expectedFiles);
    filesCreatedByTest.sort(null);
    expectedFiles.sort(null);
    for (int i = 0; i < filesCreatedByTest.size(); i++) {
      assertThat(filesCreatedByTest.get(i).getFileName()).isEqualTo(expectedFiles.get(i).getFileName());
      assertThat(filesCreatedByTest.get(i)).usingCharset(StandardCharsets.UTF_8)
          .hasSameContentAs(expectedFiles.get(i), StandardCharsets.UTF_8);
    }
  }
}
