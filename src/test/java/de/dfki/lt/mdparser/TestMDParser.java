package de.dfki.lt.mdparser;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.bwaldvogel.liblinear.Linear;
import de.dfki.lt.mdparser.caller.MDPrunner;
import de.dfki.lt.mdparser.caller.MDPtrainer;
import de.dfki.lt.mdparser.config.ConfigKeys;
import de.dfki.lt.mdparser.config.GlobalConfig;
import de.dfki.lt.mdparser.eval.Eval;


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
  public void testTrainEvalFiles() throws IOException {

    // parallel training is not deterministic, so restrict number of threads to 1
    GlobalConfig.getInstance().setProperty(ConfigKeys.TRAINING_THREADS, 1);

    String modelName = "de-2009.zip";

    testTrainFiles(modelName);
    testEvalFiles(modelName);
  }


  private void testTrainFiles(String modelName)
      throws IOException {

    MDPtrainer.train("src/test/resources/corpora/de-train-2009.conll",
        GlobalConfig.getPath(ConfigKeys.MODEL_OUTPUT_FOLDER).resolve(modelName).toString());

    assertThat(GlobalConfig.getPath(ConfigKeys.MODEL_OUTPUT_FOLDER).resolve(modelName)).exists();

    compareFolders(
        GlobalConfig.SPLIT_ALPHA_FOLDER, Paths.get("src/test/resources/expected/file/split_alphas"));
    compareFolders(
        GlobalConfig.FEATURE_VECTORS_FOLDER, Paths.get("src/test/resources/expected/file/1_initial_feature_vectors"));
    compareFolders(
        GlobalConfig.SPLIT_INITIAL_FOLDER, Paths.get("src/test/resources/expected/file/2_initial_splits"));
    compareFolders(
        GlobalConfig.SPLIT_ADJUST_FOLDER, Paths.get("src/test/resources/expected/file/3_adjusted_splits"));
    compareFolders(
        GlobalConfig.SPLIT_COMPACT_FOLDER, Paths.get("src/test/resources/expected/file/4_compacted_splits"));
    compareFolders(
        GlobalConfig.SPLIT_MODELS_FOLDER, Paths.get("src/test/resources/expected/file/split_models"));
    assertThat(GlobalConfig.ALPHA_FILE).usingCharset(StandardCharsets.UTF_8)
        .hasSameContentAs(Paths.get("src/test/resources/expected/file/alpha.txt"), StandardCharsets.UTF_8);
    assertThat(GlobalConfig.SPLIT_FILE).usingCharset(StandardCharsets.UTF_8)
        .hasSameContentAs(Paths.get("src/test/resources/expected/file/split.txt"), StandardCharsets.UTF_8);
  }


  private void testEvalFiles(String modelName)
      throws IOException {

    Eval evaluator = MDPrunner.conllFileParsingAndEval(
        "src/test/resources/corpora/de-test-2009.conll",
        "src/test/resources/corpora/de-result-2009.conll",
        GlobalConfig.getPath(ConfigKeys.MODEL_OUTPUT_FOLDER).resolve(modelName).toString());
    assertThat(evaluator.getParentsAccuracy()).isEqualTo(0.841186515716906);
    assertThat(evaluator.getLabelsAccuracy()).isEqualTo(0.8006767440389602);
  }


  @Test
  public void testTrainEvalMemory()
      throws IOException {

    String modelName = "de-2009.zip";

    testTrainMemory(modelName);
    testEvalMemory(modelName);
  }


  private void testTrainMemory(String modelName)
      throws IOException {

    MDPtrainer.trainMem("src/test/resources/corpora/de-train-2009.conll",
        GlobalConfig.getPath(ConfigKeys.MODEL_OUTPUT_FOLDER).resolve(modelName).toString());

    assertThat(GlobalConfig.getPath(ConfigKeys.MODEL_OUTPUT_FOLDER).resolve(modelName)).exists();

    compareFolders(
        GlobalConfig.SPLIT_ALPHA_FOLDER, Paths.get("src/test/resources/expected/memory/split_alphas"));
    compareFolders(
        GlobalConfig.SPLIT_MODELS_FOLDER, Paths.get("src/test/resources/expected/memory/split_models"));
    assertThat(GlobalConfig.ALPHA_FILE).usingCharset(StandardCharsets.UTF_8)
        .hasSameContentAs(Paths.get("src/test/resources/expected/memory/alpha.txt"), StandardCharsets.UTF_8);
    assertThat(GlobalConfig.SPLIT_FILE).usingCharset(StandardCharsets.UTF_8)
        .hasSameContentAs(Paths.get("src/test/resources/expected/memory/split.txt"), StandardCharsets.UTF_8);
  }


  private void testEvalMemory(String modelName)
      throws IOException {

    Eval evaluator = MDPrunner.conllFileParsingAndEval(
        "src/test/resources/corpora/de-test-2009.conll",
        "src/test/resources/corpora/de-result-2009.conll",
        GlobalConfig.getPath(ConfigKeys.MODEL_OUTPUT_FOLDER).resolve(modelName).toString());
    assertThat(evaluator.getParentsAccuracy()).isEqualTo(0.8452343305293782);
    assertThat(evaluator.getLabelsAccuracy()).isEqualTo(0.8051672885965467);
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
