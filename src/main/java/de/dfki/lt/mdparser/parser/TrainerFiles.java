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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import de.dfki.lt.mdparser.algorithm.CovingtonAlgorithm;
import de.dfki.lt.mdparser.algorithm.ParsingAlgorithm;
import de.dfki.lt.mdparser.algorithm.StackAlgorithm;
import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.config.ConfigKeys;
import de.dfki.lt.mdparser.config.GlobalConfig;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.CovingtonFeatureModel;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.FeatureVector;
import de.dfki.lt.mdparser.features.StackFeatureModel;

public final class TrainerFiles {

  private TrainerFiles() {

    // private constructor to enforce noninstantiability
  }


  // XXX GN: this is used for training
  public static void trainWithSplittingFromDisk(String conllFileName, String modelFileName)
      throws IOException {

    System.out.println("Start training with createAndTrainWithSplittingFromDisk!");
    Trainer.deleteModelBuildeFolder();

    boolean noLabels = false;
    double bias = -1;

    long trainingStartTime = System.currentTimeMillis();

    // GN: internalize CONLL data in 2-Dim sentences; max 0-12 conll columns are considered
    System.out.println("Internalize training data from: " + conllFileName);
    List<Sentence> sentences = ConllUtils.readConllFile(conllFileName, true);

    // GN: alpha is used for the mapping of integer to feature name
    // it is incrementally built during training for all features that are added
    // to the model
    Alphabet alpha = new Alphabet();
    // GN: the feature templates functions
    FeatureModel featureModel = null;
    ParsingAlgorithm algorithm = null;
    String algorithmId = GlobalConfig.getString(ConfigKeys.ALGORITHM);
    System.out.println(String.format("using algorithm \"%s\"", algorithmId));
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
    System.out.println("Create feature vectors for data: " + sentences.size());
    System.out.println("Create files in " + GlobalConfig.FEATURE_VECTORS_FOLDER);
    Files.createDirectories(GlobalConfig.FEATURE_VECTORS_FOLDER);
    for (Sentence sent : sentences) {
      // featureModel.initializeStaticFeaturesCombined(sent, true);

      // GN: call the parser on each training example to "re-play" the parser configurations
      //     and to compute the operations in form of a list of feature vectors for each state.
      //     this means that all feature functions are applied on the parsed sentence by applying
      //     the feature model in the training mode.
      //     the result is then a list of parser states in form of feature vectors whose values are based
      //     one the specific training example
      List<FeatureVector> featureVectorList = algorithm.train(sent, featureModel, noLabels);

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
        // normalize split file path
        String normalizedPath =
            GlobalConfig.getModelBuildFolder().relativize(GlobalConfig.SPLIT_ADJUST_FOLDER).normalize()
                .resolve(newFile).toString().replaceAll("\\" + File.separator, "/");
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
    TrainWorker trainWorker = new TrainWorker(alpha, bias);
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
    Trainer.recreateOneAlphabetAndAdjustModels(
        GlobalConfig.ALPHA_FILE, GlobalConfig.SPLIT_ALPHA_FOLDER, GlobalConfig.SPLIT_MODELS_FOLDER);

    long trainingEndTime = System.currentTimeMillis();
    System.out.println("Complete Training time: " + ((trainingEndTime - trainingStartTime)) + " milliseconds.");

    Archivator arch = new Archivator(modelFileName);
    arch.pack();
  }
}
