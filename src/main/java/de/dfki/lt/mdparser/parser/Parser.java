package de.dfki.lt.mdparser.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
import de.dfki.lt.mdparser.data.Data;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.CovingtonFeatureModel;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.StackFeatureModel;

public final class Parser {

  private Parser() {

    // private constructor to enforce noninstantiability
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


  // returns a mapping of features to models
  private static Map<String, Model> readModels(Archivator arch) throws IOException {

    Map<String, Model> feature2ModelMap = new HashMap<>();
    Map<String, Model> modelId2ModelMap = new HashMap<>();
    Map<String, String> feature2ModelFileNameMap = readSplitFile(arch.getSplitFileInputStream());
    for (Map.Entry<String, String> oneFeature2ModelFileName : feature2ModelFileNameMap.entrySet()) {
      String modelId = oneFeature2ModelFileName.getValue().substring(6);
      Model model = modelId2ModelMap.get(modelId);
      if (null == model) {
        String modelFileName = "splitModels/" + modelId;
        InputStream is = arch.getInputStream(modelFileName);
        model = Model.load(new InputStreamReader(is));
      }
      feature2ModelMap.put(oneFeature2ModelFileName.getKey(), model);
      modelId2ModelMap.put(modelId, model);
    }
    return feature2ModelMap;
  }


  // GN: de.dfki.lt.mdparser.caller.MDPrunner.conllFileParsingAndEval(String, String, String)
  public static void parseCombined(
      String algorithmId, Data data, Archivator arch, Alphabet alphabetParser, boolean noLabels)
      throws IOException {

    long modelReadStart = System.currentTimeMillis();
    Map<String, Model> feature2ModelMap = readModels(arch);
    //readSplitAlphabets(arch);
    long modelReadEnd = System.currentTimeMillis();
    System.out.println("Time to read model (msec): " + (modelReadEnd - modelReadStart));
    //gds readSplitModelsL(arch);
    final FeatureModel featureModel;
    final ParsingAlgorithm algorithm;
    if (algorithmId.equals("covington")) {
      featureModel = new CovingtonFeatureModel(alphabetParser);
      algorithm = new CovingtonAlgorithm();
    } else if (algorithmId.equals("stack")) {
      featureModel = new StackFeatureModel(alphabetParser);
      algorithm = new StackAlgorithm();
    } else {
      System.err.println("unknown algorithm " + algorithmId);
      return;
    }

    long processStart = System.currentTimeMillis();
    List<Sentence> sentencesList = Arrays.asList(data.getSentences());
    int parsingThreads = GlobalConfig.getInt(ConfigKeys.PARSING_THREADS);
    if (parsingThreads > 1) {
      // we use our own thread pool to be able to better control parallelization
      ForkJoinPool forkJoinPool = new ForkJoinPool(parsingThreads);
      try {
        forkJoinPool.submit(
            () -> sentencesList.stream().parallel()
            .forEach(x -> algorithm.processCombined(x, featureModel, noLabels, feature2ModelMap))).get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    } else {
      sentencesList.stream().forEach(x -> algorithm.processCombined(x, featureModel, noLabels, feature2ModelMap));
    }

    // System.out.println("All worker threads have completed.");
    long processEnd = System.currentTimeMillis();
    System.out.println("No. of threads: " + parsingThreads);
    System.out.println("Time to parse (msec): " + (processEnd - processStart));
    System.out.println("Speed (sent/s): " + (data.getSentences().length * 1000.0) / (processEnd - processStart));
    System.out.println("Number of configurations: " + algorithm.getNumberOfConfigurations());
    System.out.println(
        "Average number of configurations per sentence: "
            + algorithm.getNumberOfConfigurations() / data.getSentences().length);
  }


  //TODO the non-parallel version
  public static void parseCombinedMem(
      String algorithmId, Data data, Archivator arch, Alphabet alphabetParser, boolean noLabels)
      throws IOException {

    long modelReadStart = System.currentTimeMillis();
    Map<String, Model> feature2ModelMap = readModels(arch);

    long modelReadEnd = System.currentTimeMillis();
    System.out.println("Time to read model (msec): " + (modelReadEnd - modelReadStart));
    Sentence[] sentences = data.getSentences();
    FeatureModel model = null;
    ParsingAlgorithm algorithm = null;
    if (algorithmId.equals("covington")) {
      model = new CovingtonFeatureModel(alphabetParser);
      algorithm = new CovingtonAlgorithm();
    } else if (algorithmId.equals("stack")) {
      model = new StackFeatureModel(alphabetParser);
      algorithm = new StackAlgorithm();
    } else {
      System.err.println("unkown algorithm " + algorithmId);
      return;
    }
    long processStart = System.currentTimeMillis();
    for (int n = 0; n < sentences.length; n++) {
      algorithm.processCombined(sentences[n], model, noLabels, feature2ModelMap);
    }
    long processEnd = System.currentTimeMillis();
    System.out.println("Time to parse: " + (processEnd - processStart));
    System.out.println("Speed (sent/s): " + (sentences.length * 1000.0) / (processEnd - processStart));
    System.out.println("Number of configurations: " + algorithm.getNumberOfConfigurations());
    System.out.println(
        "Average number of configurations per sentence: " + algorithm.getNumberOfConfigurations() / sentences.length);
  }
}
