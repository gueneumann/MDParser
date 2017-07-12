package de.dfki.lt.mdparser.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Problem;
import de.dfki.lt.mdparser.config.ConfigKeys;
import de.dfki.lt.mdparser.config.GlobalConfig;
import de.dfki.lt.mdparser.features.Alphabet;

public final class Trainer {

  private static final Logger logger = LoggerFactory.getLogger(Trainer.class);


  private Trainer() {

    // private constructor to enforce noninstantiability
  }


  public static void train(String conllFileName, String modelFileName)
      throws IOException {

    String trainingMode = GlobalConfig.getString(ConfigKeys.TRAINING_MODE);
    if (trainingMode.equals("files")) {
      TrainerFiles.trainWithSplittingFromDisk(conllFileName, modelFileName);
    } else if (trainingMode.equals("memory")) {
      TrainerMemory.trainWithSplittingFromMemory(conllFileName, modelFileName);
    } else {
      logger.error(String.format("unknown training mode \"%s\"", trainingMode));
    }
  }


  static void deleteModelBuildeFolder()
      throws IOException {

    if (GlobalConfig.getModelBuildFolder().toString().trim().length() == 0) {
      deleteFolder(GlobalConfig.SPLIT_ALPHA_FOLDER);
      deleteFolder(GlobalConfig.FEATURE_VECTORS_FOLDER);
      deleteFolder(GlobalConfig.SPLIT_INITIAL_FOLDER);
      deleteFolder(GlobalConfig.SPLIT_ADJUST_FOLDER);
      deleteFolder(GlobalConfig.SPLIT_COMPACT_FOLDER);
      deleteFolder(GlobalConfig.SPLIT_MODELS_FOLDER);
      deleteFolder(GlobalConfig.FEATURE_VECTORS_FOLDER);
      try {
        Files.delete(GlobalConfig.ALPHA_FILE);
      } catch (NoSuchFileException e) {
        // nothing to do, file already deleted
      }
      try {
        Files.delete(GlobalConfig.SPLIT_FILE);
      } catch (NoSuchFileException e) {
        // nothing to do, file already deleted
      }
    } else {
      deleteFolder(GlobalConfig.getModelBuildFolder());
    }
  }


  private static void deleteFolder(Path path)
      throws IOException {

    try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException {

          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }


        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            throws IOException {

          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (NoSuchFileException e) {
      // nothing to do, file already deleted
    }
  }


  static void recreateOneAlphabetAndAdjustModels(
      Path alphabetPath, Path splitAlphaDir, Path splitModelsDir)
      throws IOException {

    Alphabet alpha = uniteAlphabets(splitAlphaDir);
    restoreModels(splitModelsDir, alpha, splitAlphaDir);
    alpha.writeToFile(alphabetPath);
  }


  private static Alphabet uniteAlphabets(Path splitAlphaDir) throws IOException {

    Alphabet alphasUnited = null;
    List<Path> alphaFiles = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(splitAlphaDir)) {
      stream.forEach(alphaFiles::add);
    }
    alphaFiles.sort(Comparator.comparing(Path::toString));
    for (Path oneAlphaFile : alphaFiles) {
      Alphabet curAlpha = new Alphabet(Files.newInputStream(oneAlphaFile));
      if (alphasUnited == null) {
        // init union with first alphabet to unite
        alphasUnited = curAlpha;
        continue;
      }
      int numberOfFeatures = curAlpha.getNumberOfFeatures();
      // feature indices start at 1, so we iterate until num + 1
      for (int k = 1; k <= numberOfFeatures; k++) {
        alphasUnited.addFeature(curAlpha.getFeature(k));
        //System.out.println(features[k]);
      }
    }
    return alphasUnited;
  }


  private static void restoreModels(Path splitModelsDir, Alphabet alpha, Path splitAlphaDir) throws IOException {

    List<Path> splitModelFiles = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(splitModelsDir)) {
      stream.forEach(splitModelFiles::add);
    }
    splitModelFiles.sort(Comparator.comparing(Path::toString));
    for (Path oneModelFile : splitModelFiles) {
      System.out.println("Model file: " + oneModelFile);
      Model model = Linear.loadModel(oneModelFile.toFile());
      double[] wArray = model.getFeatureWeights();
      Path alphaPath = splitAlphaDir.resolve(oneModelFile.getFileName());
      Alphabet a = new Alphabet(Files.newInputStream(alphaPath));
      int numberOfClasses = model.getNrClass();

      try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(oneModelFile, StandardCharsets.UTF_8))) {
        out.println("solver_type MCSVM_CS");
        out.print(String.format("nr_class %d%nlabel ", model.getNrClass()));

        for (int k = 0; k < model.getLabels().length; k++) {
          out.print(model.getLabels()[k] + " ");
        }
        out.println();
        int numberOfFeatures = alpha.getNumberOfFeatures();
        boolean notFound = true;
        int lastIndex = -1;
        for (int k = numberOfFeatures; k > 1 && notFound; k--) {
          if (a.getFeatureIndex(alpha.getFeature(k)) != null) {
            lastIndex = k;
            notFound = false;
          }
        }
        out.println("nr_feature " + (lastIndex - 1));
        out.println("bias " + model.getBias());
        out.println("w");
        for (int k = 1; k < lastIndex; k++) {
          String feature = alpha.getFeature(k);
          Integer oldIndex = a.getFeatureIndex(feature);
          if (oldIndex == null) {
            for (int m = 0; m < numberOfClasses; m++) {
              out.print("0 ");
            }
          } else {
            for (int l = 0; l < numberOfClasses; l++) {
              double curWeight = wArray[(oldIndex - 1) * numberOfClasses + l];
              out.print(curWeight + " ");
            }
          }
          out.println();
        }
      }
    }
  }


  static Problem constructProblem(
      List<Integer> yList, List<FeatureNode[]> xList, int max_index, double bias) {

    Problem prob = new Problem();
    prob.bias = bias;
    prob.l = yList.size();
    prob.n = max_index;
    if (bias >= 0) {
      prob.n++;
    }
    prob.x = new FeatureNode[prob.l][];
    for (int i = 0; i < prob.l; i++) {
      prob.x[i] = xList.get(i);

      if (bias >= 0) {
        assert prob.x[i][prob.x[i].length - 1] == null;
        prob.x[i][prob.x[i].length - 1] = new FeatureNode(max_index + 1, bias);
      } else {
        assert prob.x[i][prob.x[i].length - 1] != null;
      }
    }

    prob.y = new double[prob.l];
    for (int i = 0; i < prob.l; i++) {
      prob.y[i] = yList.get(i);
    }

    return prob;
  }


  static Set<Integer> getUnusedFeatures(Path modelPath) {

    Set<Integer> unusedFeatures = new HashSet<>();

    try (BufferedReader in = Files.newBufferedReader(modelPath, StandardCharsets.UTF_8)) {
      // skip first 6 lines
      for (int k = 0; k < 6; k++) {
        in.readLine();
      }

      int wIndex = 1;
      String line;
      while ((line = in.readLine()) != null) {
        String[] lineArray = line.split("\\s+");
        boolean zeroLine = true;
        for (int k = 0; k < lineArray.length && zeroLine; k++) {
          if (Math.abs(Double.valueOf(lineArray[k])) > 0.1) {
            zeroLine = false;
          }
        }
        if (zeroLine) {
          unusedFeatures.add(wIndex);
        }
        wIndex++;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return unusedFeatures;
  }


  static void removeUnusedFeaturesFromModel(
      Path modelPath, Set<Integer> unusedFeatures, int numberOfFeatures)
      throws IOException {

    BufferedReader in = Files.newBufferedReader(modelPath, StandardCharsets.UTF_8);
    String solverType = in.readLine();
    String nrClass = in.readLine();
    int numberOfClasses = Integer.valueOf(nrClass.split(" ")[1]);
    String label = in.readLine();
    // don't use the 'number of features' value from the model
    //String nrFeature = in.readLine();
    in.readLine();
    String bias = in.readLine();
    // read "w":
    in.readLine();
    double[] weights;
    if (numberOfClasses != 2) {
      weights = new double[numberOfFeatures * numberOfClasses];
    } else {
      weights = new double[numberOfFeatures];
    }
    int k = 0;
    int l = 1;
    String line;
    while ((line = in.readLine()) != null) {
      if (!unusedFeatures.contains(l)) {
        String[] weightsArray = line.split(" ");
        if (numberOfClasses != 2) {
          for (int c = 0; c < numberOfClasses; c++) {
            weights[k * numberOfClasses + c] = Double.valueOf(weightsArray[c]);
          }
          k++;
        } else {
          weights[k] = Double.valueOf(weightsArray[0]);
          k++;
        }
      }
      l++;
    }
    in.close();

    // re-write model
    PrintWriter out = new PrintWriter(Files.newBufferedWriter(modelPath, StandardCharsets.UTF_8));
    out.println(solverType);
    if (numberOfClasses != 2) {
      out.println("nr_class " + numberOfClasses);
    } else {
      out.println("nr_class 1");
    }
    out.println(label);
    out.println("nr_feature " + numberOfFeatures);
    out.println(bias);
    out.println("w");
    if (numberOfClasses != 2) {
      for (int i = 0; i < weights.length / numberOfClasses; i++) {
        for (int c = 0; c < numberOfClasses; c++) {
          if (weights[i * numberOfClasses + c] == 0) {
            out.print("0");
          } else {
            out.print(String.valueOf(weights[i * numberOfClasses + c]));
          }
          out.print(" ");
        }
        out.println();
      }
    } else {
      for (int i = 0; i < weights.length; i++) {
        if (weights[i] == 0) {
          out.print("0");
        } else {
          out.print(String.valueOf(weights[i]));
        }
        out.println(" ");
      }
    }
    out.close();
  }
}
