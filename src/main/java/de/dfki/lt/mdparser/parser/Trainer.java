package de.dfki.lt.mdparser.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Problem;
import de.dfki.lt.mdparser.algorithm.CovingtonAlgorithm;
import de.dfki.lt.mdparser.algorithm.ParsingAlgorithm;
import de.dfki.lt.mdparser.algorithm.StackAlgorithm;
import de.dfki.lt.mdparser.config.ConfigKeys;
import de.dfki.lt.mdparser.config.GlobalConfig;
import de.dfki.lt.mdparser.data.Data;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.CovingtonFeatureModel;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.FeatureVector;
import de.dfki.lt.mdparser.features.StackFeatureModel;

public class Trainer {

  private double bias = -1;


  // XXX GN: this is used for training
  public void createAndTrainWithSplittingFromDisk(String algorithmId, String inputFile)
      throws IOException {

    boolean noLabels = false;
    System.out.println("Start training with createAndTrainWithSplittingFromDisk!");

    // GN: internalize CONLL data in 2-Dim sentences; max 0-12 conll columns are considered
    System.out.println("Internalize training data from: " + inputFile);
    Data data = new Data(inputFile, true);

    // GN: alpha is used for the mapping of integer to feature name
    // it is incrementally built during training for all features that are added
    // to the model
    Alphabet alpha = new Alphabet();
    // GN: the feature templates functions
    Sentence[] sentences = data.getSentences();
    FeatureModel featureModel = null;
    ParsingAlgorithm algorithm = null;
    if (algorithmId.equals("covington")) {
      featureModel = new CovingtonFeatureModel(alpha);
      algorithm = new CovingtonAlgorithm();
    } else if (algorithmId.equals("stack")) {
      featureModel = new StackFeatureModel(alpha);
      algorithm = new StackAlgorithm();
    } else {
      System.err.println("unknown algorithm " + algorithmId);
      return;
    }
    //print training data
    Map<Integer, PrintWriter> outputMap = new HashMap<>();
    Map<Integer, String> posMap = new HashMap<>();

    // GN: for each training sentence example do:
    // NOTE: this is a sequential step
    System.out.println("Create feature vectors for data: " + sentences.length);
    System.out.println("Create files in " + GlobalConfig.FEATURE_VECTORS_FOLDER);
    Files.createDirectories(GlobalConfig.FEATURE_VECTORS_FOLDER);
    for (int n = 0; n < sentences.length; n++) {
      Sentence sent = sentences[n];
      // featureModel.initializeStaticFeaturesCombined(sent, true);

      // GN: call the parser on each training example to "re-play" the parser configurations
      //     and to compute the operations in form of a list of feature vectors for each state.
      //     this means that all feature functions are applied on the parsed sentence by applying
      //     the feature model in the training mode.
      //     the result is then a list of parser states in form of feature vectors whose values are based
      //     one the specific training example
      List<FeatureVector> featureVectorList = algorithm.processCombined(sent, featureModel, noLabels);

      //System.out.println(parserList.toString());

      // GN: for each feature vector (which represents a parser state) do
      for (int i = 0; i < featureVectorList.size(); i++) {
        // GN: for each state/feature vector store them in internal format in a file
        // buffer whose name depends on the label-value of the feature vector
        // store in the label-value/buffer in opMap hash.
        // NOTE: token level, so duplicates
        FeatureVector featureVector = featureVectorList.get(i);
        String operation = featureVector.getLabel();
        // GN: Lookup up label-index and use it to create/extend buffer
        Integer index = alpha.getLabelIndex(operation);
        // GN: create label-index many different split0 files, so that each files contains just the feature vectors
        // of each edge feature vector and its label instance
        // The label-index buffers are kept in a hash array
        PrintWriter curWriter = outputMap.get(index);
        if (curWriter == null) {
          curWriter =
              new PrintWriter(
                  Files.newBufferedWriter(
                      GlobalConfig.FEATURE_VECTORS_FOLDER.resolve(String.format("%03d.txt", index)),
                      StandardCharsets.UTF_8));
          outputMap.put(index, curWriter);
        }
        String sentenceIntegerString = featureVector.getIntegerRepresentation(alpha, false);
        //System.out.println(sentenceIntegerString+"\n");
        curWriter.println(sentenceIntegerString);
      }
    }

    // GN: and finally close all the writers
    for (PrintWriter oneWriter : outputMap.values()) {
      oneWriter.close();
    }

    //XXX HIERIX -MDP-howItWorks.txt
    // GN: the next code basically creates the split training files
    // using a distributed approach based on the available processors
    // stores and adjust the split files in folder split/
    // and finally calls the trainer on each file in parallel
    alpha.writeToFile(GlobalConfig.ALPHA_FILE);
    int numberOfFeatures = alpha.getNumberOfFeatures();
    // feature indices start at 1, so we iterate until num + 1
    for (int v = 1; v <= numberOfFeatures; v++) {
      String val = alpha.getFeature(v);
      if (val.split("=")[0].equals("pj")) {
        posMap.put(v, val);
      }
    }

    System.out.println("Create splitting training files!");

    // GN: for each label-specific feature vector integer encoded file do

    // split files
    Map<String, PrintWriter> splitMap = new HashMap<>();
    List<Path> filesToSplit = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(GlobalConfig.FEATURE_VECTORS_FOLDER)) {
      stream.forEach(x -> filesToSplit.add(x));
    }
    SplitWorker splitWorker = new SplitWorker(posMap, splitMap);
    int trainingThreads = GlobalConfig.getInt(ConfigKeys.TRAINING_THREADS);
    if (trainingThreads < 0) {
      trainingThreads = Runtime.getRuntime().availableProcessors();
    }
    if (trainingThreads > 1) {
      // we use our own thread pool to be able to better control parallelization
      ForkJoinPool forkJoinPool = new ForkJoinPool(trainingThreads);
      System.out.println("Parallel processing on " + trainingThreads + " processors !");
      try {
        forkJoinPool.submit(
            () -> filesToSplit.stream().parallel().forEach(x -> splitWorker.processFile(x))).get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    } else {
      filesToSplit.stream().forEach(x -> splitWorker.processFile(x));
    }
    for (PrintWriter oneWriter : splitMap.values()) {
      oneWriter.close();
    }

    // GN: First version of split files are generated in splitF/
    // adjust them and store them in split and merge into files of acceptable size
    System.out.println(
        "Adjust splitting files in " + GlobalConfig.SPLIT_INITIAL_FOLDER
        + " and store them in " + GlobalConfig.SPLIT_ADJUST_FOLDER);
    Files.createDirectories(GlobalConfig.SPLIT_ADJUST_FOLDER);
    Map<String, String> newSplitMap = new LinkedHashMap<>();
    int curSize = 0;
    int splitThreshold = 3000;
    PrintWriter curWriter = null;
    String curFile = null;
    String lastFile = null;
    String curSplitVal = null;
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(GlobalConfig.SPLIT_INITIAL_FOLDER)) {
      for (Path oneTrainingFile : stream) {
        curSplitVal = oneTrainingFile.getFileName().toString();

        if (curWriter == null) {
          curFile = curSplitVal;
          curWriter = new PrintWriter(
              Files.newBufferedWriter(GlobalConfig.SPLIT_ADJUST_FOLDER.resolve(curFile), StandardCharsets.UTF_8));
        }

        try (BufferedReader in = Files.newBufferedReader(oneTrainingFile, StandardCharsets.UTF_8)) {
          String line;
          while ((line = in.readLine()) != null) {
            curWriter.println(line);
            curSize++;
          }
        }

        if (curSize > splitThreshold) {
          curWriter.close();
          curWriter = null;
          curSize = 0;
          lastFile = curFile;
        }
        newSplitMap.put(curSplitVal, curFile);
      }
    }
    //if the last file is too small add to the second to last

    System.out.println("Eventually adjust last split/ file. ");
    if (curWriter != null) {
      curWriter.close();
      if (lastFile != null) {

        StringBuilder builder = new StringBuilder();

        try (BufferedReader in = Files.newBufferedReader(
            GlobalConfig.SPLIT_ADJUST_FOLDER.resolve(lastFile), StandardCharsets.UTF_8)) {
          String line;
          while ((line = in.readLine()) != null) {
            builder.append(String.format("%s%n", line));
          }
        }

        try (BufferedReader in = Files.newBufferedReader(
            GlobalConfig.SPLIT_ADJUST_FOLDER.resolve(curFile), StandardCharsets.UTF_8)) {
          String line;
          while ((line = in.readLine()) != null) {
            builder.append(String.format("%s%n", line));
          }
        }

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(
            GlobalConfig.SPLIT_ADJUST_FOLDER.resolve(lastFile), StandardCharsets.UTF_8))) {
          out.print(builder.toString());
        }
        // delete the curFile that was just appended to lastFile
        Files.delete(GlobalConfig.SPLIT_ADJUST_FOLDER.resolve(curFile));

        newSplitMap.put(curSplitVal, lastFile);
        Set<String> toSubstitute = new HashSet<String>();
        for (Map.Entry<String, String> oneEntry : newSplitMap.entrySet()) {
          if (oneEntry.getValue().equals(curFile)) {
            toSubstitute.add(oneEntry.getKey());
          }
        }
        if (!toSubstitute.isEmpty()) {
          for (String oneSubstitute : toSubstitute) {
            newSplitMap.put(oneSubstitute, lastFile);
          }
        }
      } else {
        newSplitMap.put(curFile, curFile);
      }
    }
    //System.out.println(newSplitMap);
    //printSplitMap

    if (GlobalConfig.SPLIT_FILE.getParent() != null) {
      Files.createDirectories(GlobalConfig.SPLIT_FILE.getParent());
    }
    try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(
        GlobalConfig.SPLIT_FILE, StandardCharsets.UTF_8))) {
      for (Map.Entry<String, String> oneSplitValFilePair : newSplitMap.entrySet()) {
        String splitVal = oneSplitValFilePair.getKey();
        String newFile = oneSplitValFilePair.getValue();
        Integer index = Integer.valueOf(splitVal.split("\\.")[0]);
        String featureString = alpha.getFeature(index);
        // normalize separators
        String normalizedPath =
            GlobalConfig.SPLIT_ADJUST_FOLDER.resolve(newFile).toString().replaceAll("\\" + File.separator, "/");
        out.println(String.format("%s %s %s", featureString, normalizedPath, splitVal));
      }
    }

    System.out.println(
        "Compute the weights and the final model files in " + GlobalConfig.SPLIT_MODELS_FOLDER
        + " and alphabet files in " + GlobalConfig.SPLIT_ALPHA_FOLDER);

    // compact files
    List<Path> filesToCompact = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(GlobalConfig.SPLIT_ADJUST_FOLDER)) {
      stream.forEach(x -> filesToCompact.add(x));
    }
    TrainWorker trainWorker = new TrainWorker(alpha, this.bias);
    if (trainingThreads > 1) {
      // we use our own thread pool to be able to better control parallelization
      ForkJoinPool compactingForkJoinPool = new ForkJoinPool(trainingThreads);
      try {
        compactingForkJoinPool.submit(
            () -> filesToCompact.stream().parallel().forEach(x -> trainWorker.processFile(x))).get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    } else {
      filesToCompact.stream().forEach(x -> trainWorker.processFile(x));
    }

    System.out.println("Make single alphabet file " + GlobalConfig.ALPHA_FILE + " from splitA files");
    recreateOneAlphabetAndAdjustModels(
        GlobalConfig.ALPHA_FILE, GlobalConfig.SPLIT_ALPHA_FOLDER, GlobalConfig.SPLIT_MODELS_FOLDER);
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
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(splitAlphaDir)) {
      for (Path oneAlphaFile : stream) {
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
    }
    return alphasUnited;
  }


  private static void restoreModels(Path splitModelsDir, Alphabet alpha, Path splitAlphaDir) throws IOException {

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(splitModelsDir)) {
      for (Path oneModelFile : stream) {
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
  }


  public static int[][] compactiseTrainingDataFile(
      Path curentTrainingFile, int numberOfFeatures) throws IOException {

    int[][] compactArray = new int[2][];
    // feature indices start at 1, so we have to add 1 to the size
    int[] oldToNew = new int[numberOfFeatures + 1];
    int[] newToOld = new int[numberOfFeatures + 1];
    compactArray[0] = newToOld;
    compactArray[1] = oldToNew;

    try (PrintWriter out = new PrintWriter(
        Files.newBufferedWriter(
            GlobalConfig.SPLIT_COMPACT_FOLDER.resolve(curentTrainingFile.getFileName()), StandardCharsets.UTF_8));
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


  static Problem readProblem(Path path, double bias)
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
          throw new InvalidInputDataException("invalid label: " + token, path.toString(), lineNr, e);
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
            throw new InvalidInputDataException("invalid index: " + token, path.toString(), lineNr, e);
          }

          // assert that indices are valid and sorted
          if (index < 0) {
            throw new InvalidInputDataException("invalid index: " + index, path.toString(), lineNr);
          }
          if (index <= indexBefore) {
            throw new InvalidInputDataException("indices must be sorted in ascending order", path.toString(), lineNr);
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

    return constructProblem(yList, xList, maxIndex, bias);
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
