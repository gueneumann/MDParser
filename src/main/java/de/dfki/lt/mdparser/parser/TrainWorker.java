package de.dfki.lt.mdparser.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import de.dfki.lt.mdparser.config.GlobalConfig;
import de.dfki.lt.mdparser.features.Alphabet;

public class TrainWorker {

  private Alphabet alpha;
  private double bias;
  private Parameter param;


  public TrainWorker(Alphabet alpha, double bias)
      throws IOException {

    this.alpha = alpha;
    this.bias = bias;
    this.param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.3);

    Files.createDirectories(GlobalConfig.SPLIT_COMPACT_FOLDER);
    Files.createDirectories(GlobalConfig.SPLIT_MODELS_FOLDER);
  }


  public void processFile(Path file) {

    try {
      long threadId = Thread.currentThread().getId();
      System.out.println("Hello from TrainWorker in thread " + threadId);

      System.out.println("... Steps: compactifize/sort training file " + file);
      System.out.println("... and store in " + GlobalConfig.SPLIT_COMPACT_FOLDER
          + "; read problem and call trainer, and finally save models and alphabet, "
          + "and edit them. ");
      int[][] compactArray = compactiseTrainingDataFile(file, this.alpha.getNumberOfFeatures());
      //System.out.println("new to old size: "+compactArray[0].length);

      String alphaFileName = GlobalConfig.SPLIT_ALPHA_FOLDER.resolve(file.getFileName()).toString();
      this.alpha.writeToFile(Paths.get(alphaFileName), compactArray);

      // GN: call the trainer
      Problem prob = readProblem(
          GlobalConfig.SPLIT_COMPACT_FOLDER.resolve(file.getFileName()), this.bias);
      Linear.disableDebugOutput();
      Model model = Linear.train(prob, this.param);

      // GN: and save the training files
      Path modelPath = GlobalConfig.SPLIT_MODELS_FOLDER.resolve(file.getFileName());
      System.out.println("Save: " + modelPath);
      model.save(modelPath.toFile());

      Set<Integer> unusedFeatures = Trainer.getUnusedFeatures(modelPath);

      Alphabet compactAlpha = new Alphabet(Paths.get(alphaFileName));
      compactAlpha.removeUnusedFeatures(unusedFeatures);
      compactAlpha.writeToFile(Paths.get(alphaFileName));

      Trainer.removeUnusedFeaturesFromModel(
          modelPath, unusedFeatures, compactAlpha.getNumberOfFeatures());
    } catch (IOException | InvalidInputDataException e) {
      e.printStackTrace();
    }
  }


  private static int[][] compactiseTrainingDataFile(
      Path curentTrainingFile, int numberOfFeatures) throws IOException {

    int[][] compactArray = new int[2][];
    // feature indices start at 1, so we have to add 1 to the size
    int[] oldToNew = new int[numberOfFeatures + 1];
    int[] newToOld = new int[numberOfFeatures + 1];
    compactArray[0] = newToOld;
    compactArray[1] = oldToNew;

    try (PrintWriter out = new PrintWriter(
        Files.newBufferedWriter(
            GlobalConfig.SPLIT_COMPACT_FOLDER.resolve(curentTrainingFile.getFileName()),
            StandardCharsets.UTF_8));
        BufferedReader in = Files.newBufferedReader(curentTrainingFile, StandardCharsets.UTF_8)) {
      String line;
      int maxIndex = 1;
      Set<Integer> encountered = new HashSet<>(numberOfFeatures + 1);
      while ((line = in.readLine()) != null) {
        String[] lineArray = line.split(" ");
        out.print(lineArray[0]);
        List<Integer> featureList = new ArrayList<Integer>();
        for (int i = 1; i < lineArray.length; i++) {
          Integer curFeature = Integer.valueOf(lineArray[i].split(":")[0]);
          Integer newValue = oldToNew[curFeature];
          if (!encountered.contains(curFeature)) {
            newValue = maxIndex;
            oldToNew[curFeature] = newValue;
            newToOld[newValue] = curFeature;
            encountered.add(curFeature);
            maxIndex++;
          }
          if (!featureList.contains(newValue)) {
            featureList.add(newValue);
          }
        }
        Collections.sort(featureList);
        for (int i = 0; i < featureList.size(); i++) {
          out.print(" " + featureList.get(i) + ":1");
        }
        out.println();
      }
    }

    return compactArray;
  }


  private static Problem readProblem(Path path, double bias)
      throws InvalidInputDataException {

    List<Integer> yList = new ArrayList<Integer>();
    List<FeatureNode[]> xList = new ArrayList<FeatureNode[]>();
    int maxIndex = 0;

    try (BufferedReader in = Files.newBufferedReader(
        path, StandardCharsets.UTF_8)) {
      String line;
      int lineNr = 0;
      while ((line = in.readLine()) != null) {
        lineNr++;
        StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
        String token = st.nextToken();

        try {
          yList.add(Integer.parseInt(token));
        } catch (NumberFormatException e) {
          throw new InvalidInputDataException(
              "invalid label: " + token, path.toString(), lineNr, e);
        }

        int m = st.countTokens() / 2;
        FeatureNode[] x;
        if (bias >= 0) {
          x = new FeatureNode[m + 1];
        } else {
          x = new FeatureNode[m];
        }
        int indexBefore = 0;
        for (int j = 0; j < m; j++) {
          token = st.nextToken();
          int index;

          try {
            index = Integer.parseInt(token);
          } catch (NumberFormatException e) {
            throw new InvalidInputDataException(
                "invalid index: " + token, path.toString(), lineNr, e);
          }

          // assert that indices are valid and sorted
          if (index < 0) {
            throw new InvalidInputDataException("invalid index: " + index, path.toString(), lineNr);
          }
          if (index <= indexBefore) {
            throw new InvalidInputDataException(
                "indices must be sorted in ascending order", path.toString(), lineNr);
          }
          indexBefore = index;

          token = st.nextToken();
          try {
            double value = Double.parseDouble(token);
            x[j] = new FeatureNode(index, value);
          } catch (NumberFormatException e) {
            throw new InvalidInputDataException("invalid value: " + token, path.toString(), lineNr);
          }
        }
        if (m > 0) {
          maxIndex = Math.max(maxIndex, x[m - 1].index);
        }
        xList.add(x);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return Trainer.constructProblem(yList, xList, maxIndex, bias);
  }
}
