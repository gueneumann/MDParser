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

public class Parser {

  private final FeatureModel featureModel;
  private final ParsingAlgorithm algorithm;
  private Map<String, Model> feature2ModelMap;
  private boolean noLabels;


  public Parser(String modelFileName)
      throws IOException {

    this.noLabels = false;

    Archivator arch = new Archivator(modelFileName);
    Alphabet alphabet = new Alphabet(arch.getInputStream(
        GlobalConfig.getModelBuildFolder().relativize(GlobalConfig.ALPHA_FILE).normalize()
            .toString()));

    long modelReadStart = System.currentTimeMillis();
    this.feature2ModelMap = readModels(arch);
    //readSplitAlphabets(arch);
    long modelReadEnd = System.currentTimeMillis();
    System.out.println("Time to read model (msec): " + (modelReadEnd - modelReadStart));
    //gds readSplitModelsL(arch);
    String algorithmId = GlobalConfig.getString(ConfigKeys.ALGORITHM);
    System.out.println(String.format("using algorithm \"%s\"", algorithmId));
    if (algorithmId.equals("covington")) {
      this.featureModel = new CovingtonFeatureModel(alphabet);
      this.algorithm = new CovingtonAlgorithm();
    } else if (algorithmId.equals("stack")) {
      this.featureModel = new StackFeatureModel(alphabet);
      this.algorithm = new StackAlgorithm();
    } else {
      throw new IOException("unknown algorithm " + algorithmId);
    }
  }


  public List<Sentence> parse(String conllFileName)
      throws IOException {

    List<Sentence> sentencesList = ConllUtils.readConllFile(conllFileName, false);
    System.out.println("No. of sentences: " + sentencesList.size());

    long processStart = System.currentTimeMillis();
    int parsingThreads = GlobalConfig.getInt(ConfigKeys.PARSING_THREADS);
    if (parsingThreads < 0) {
      parsingThreads = Runtime.getRuntime().availableProcessors();
    }
    parse(sentencesList);

    // System.out.println("All worker threads have completed.");
    long processEnd = System.currentTimeMillis();
    System.out.println("No. of threads: " + parsingThreads);
    System.out.println("Time to parse (msec): " + (processEnd - processStart));
    System.out.println(
        "Speed (sent/s): " + (sentencesList.size() * 1000.0) / (processEnd - processStart));
    System.out.println("Number of configurations: " + this.algorithm.getNumberOfConfigurations());
    System.out.println(
        "Average number of configurations per sentence: "
            + this.algorithm.getNumberOfConfigurations() / sentencesList.size());

    return sentencesList;
  }


  public List<Sentence> parse(List<Sentence> sentencesList) {

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
                .forEach(x -> this.algorithm.parse(
                    x, this.featureModel, this.noLabels, this.feature2ModelMap)))
            .get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    } else {
      sentencesList.stream()
          .forEach(x -> this.algorithm.parse(
              x, this.featureModel, this.noLabels, this.feature2ModelMap));
    }

    return sentencesList;
  }


  public Sentence parse(Sentence sentence) {

    this.algorithm.parse(sentence, this.featureModel, this.noLabels, this.feature2ModelMap);
    return sentence;
  }


  // returns a mapping of features to models
  public static Map<String, Model> readModels(Archivator arch) throws IOException {

    Map<String, Model> feature2ModelMap = new HashMap<>();
    Map<String, Model> modelId2ModelMap = new HashMap<>();
    Map<String, String> feature2ModelFileNameMap =
        readSplitFile(arch.getInputStream(
            GlobalConfig.getModelBuildFolder().relativize(GlobalConfig.SPLIT_FILE).normalize()
                .toString()));
    for (Map.Entry<String, String> oneFeature2ModelFileName : feature2ModelFileNameMap.entrySet()) {
      String modelId = Paths.get(oneFeature2ModelFileName.getValue()).getFileName().toString();
      Model model = modelId2ModelMap.get(modelId);
      if (null == model) {
        Path modelPath =
            GlobalConfig.getModelBuildFolder().relativize(GlobalConfig.SPLIT_MODELS_FOLDER)
                .normalize()
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
