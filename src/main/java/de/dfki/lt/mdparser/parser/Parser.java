package de.dfki.lt.mdparser.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import de.bwaldvogel.liblinear.Model;
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
import de.dfki.lt.mdparser.features.StackFeatureModel;

public final class Parser {

  private Parser() {

    // private constructor to enforce noninstantiability
  }


  public static List<Sentence> parse(String conllFileName, String modelFileName)
      throws IOException {

    List<Sentence> sentencesList = ConllUtils.readConllFile(conllFileName, false);
    System.out.println("No. of sentences: " + sentencesList.size());

    Archivator arch = new Archivator(modelFileName);
    Alphabet alphabet = new Alphabet(arch.getInputStream(
        GlobalConfig.getModelBuildFolder().relativize(GlobalConfig.ALPHA_FILE).normalize().toString()));

    boolean noLabels = false;

    long modelReadStart = System.currentTimeMillis();
    Map<String, Model> feature2ModelMap = readModels(arch);
    //readSplitAlphabets(arch);
    long modelReadEnd = System.currentTimeMillis();
    System.out.println("Time to read model (msec): " + (modelReadEnd - modelReadStart));
    //gds readSplitModelsL(arch);
    final FeatureModel featureModel;
    final ParsingAlgorithm algorithm;
    String algorithmId = GlobalConfig.getString(ConfigKeys.ALGORITHM);
    System.out.println(String.format("using algorithm \"%s\"", algorithmId));
    if (algorithmId.equals("covington")) {
      featureModel = new CovingtonFeatureModel(alphabet);
      algorithm = new CovingtonAlgorithm();
    } else if (algorithmId.equals("stack")) {
      featureModel = new StackFeatureModel(alphabet);
      algorithm = new StackAlgorithm();
    } else {
      System.err.println("unknown algorithm " + algorithmId);
      return null;
    }

    long processStart = System.currentTimeMillis();
    int parsingThreads = GlobalConfig.getInt(ConfigKeys.PARSING_THREADS);
    if (parsingThreads < 0) {
      parsingThreads = Runtime.getRuntime().availableProcessors();
    }
    if (parsingThreads > 1) {
      // we use our own thread pool to be able to better control parallelization
      ForkJoinPool forkJoinPool = new ForkJoinPool(parsingThreads);
      try {
        forkJoinPool.submit(
            () -> sentencesList.stream().parallel()
            .forEach(x -> algorithm.parse(x, featureModel, noLabels, feature2ModelMap))).get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    } else {
      sentencesList.stream().forEach(x -> algorithm.parse(x, featureModel, noLabels, feature2ModelMap));
    }

    // System.out.println("All worker threads have completed.");
    long processEnd = System.currentTimeMillis();
    System.out.println("No. of threads: " + parsingThreads);
    System.out.println("Time to parse (msec): " + (processEnd - processStart));
    System.out.println("Speed (sent/s): " + (sentencesList.size() * 1000.0) / (processEnd - processStart));
    System.out.println("Number of configurations: " + algorithm.getNumberOfConfigurations());
    System.out.println(
        "Average number of configurations per sentence: "
            + algorithm.getNumberOfConfigurations() / sentencesList.size());

    return sentencesList;
  }


  // returns a mapping of features to models
  public static Map<String, Model> readModels(Archivator arch) throws IOException {

    Map<String, Model> feature2ModelMap = new HashMap<>();
    Map<String, Model> modelId2ModelMap = new HashMap<>();
    Map<String, String> feature2ModelFileNameMap =
        readSplitFile(arch.getInputStream(
            GlobalConfig.getModelBuildFolder().relativize(GlobalConfig.SPLIT_FILE).normalize().toString()));
    for (Map.Entry<String, String> oneFeature2ModelFileName : feature2ModelFileNameMap.entrySet()) {
      String modelId = Paths.get(oneFeature2ModelFileName.getValue()).getFileName().toString();
      Model model = modelId2ModelMap.get(modelId);
      if (null == model) {
        Path modelPath =
            GlobalConfig.getModelBuildFolder().relativize(GlobalConfig.SPLIT_MODELS_FOLDER).normalize()
                .resolve(modelId);
        try (InputStream is = arch.getInputStream(modelPath.toString())) {
          model = Model.load(new InputStreamReader(is));
        }
      }
      feature2ModelMap.put(oneFeature2ModelFileName.getKey(), model);
      modelId2ModelMap.put(modelId, model);
    }
    return feature2ModelMap;
  }


  // returns a mapping of features to model file names
  private static Map<String, String> readSplitFile(InputStream splitFileInputStream) {

    Map<String, String> splitMap = new HashMap<>();
    try (BufferedReader in = new BufferedReader(
        new InputStreamReader(splitFileInputStream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = in.readLine()) != null) {
        String[] lineArray = line.split(" ");
        splitMap.put(lineArray[0], lineArray[1]);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return splitMap;
  }
}
