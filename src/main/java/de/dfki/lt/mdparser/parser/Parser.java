package de.dfki.lt.mdparser.parser;

import java.io.BufferedReader;
import java.io.FileReader;
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
import de.dfki.lt.mdparser.data.Data;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.CovingtonFeatureModel;
import de.dfki.lt.mdparser.features.FeatureExtractor;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.StackFeatureModel;

public class Parser {

  private static long time;

  private double[] weightsParser;
  private int numberOfClassesParser;
  private int numberOfClassesLabeler;
  private int predictedSecondBestLabelIndex;

  private HashMap<String, Model> splitModelMap;
  private HashMap<String, Model> splitModelMapL;
  private HashMap<String, String> splitMapL;

  private HashMap<String, double[]> splitWeightsMap;
  private HashMap<String, double[]> splitWeightsMapL;

  private HashMap<String, Alphabet> splitAlphabetsMap;


  public void setNumberOfClassesParser(int numberOfClassesParser) {

    this.numberOfClassesParser = numberOfClassesParser;
  }


  public int getNumberOfClassesParser() {

    return this.numberOfClassesParser;
  }


  public double[] getWeightsParser() {

    return this.weightsParser;
  }


  public void setWeightsParser(double[] featureWeights) {

    this.weightsParser = featureWeights;

  }


  public void setNumberOfClassesLabeler(int numberOfClassesLabeler) {

    this.numberOfClassesLabeler = numberOfClassesLabeler;
  }


  public int getNumberOfClassesLabeler() {

    return this.numberOfClassesLabeler;
  }


  public void setPredictedSecondBestLabelIndex(
      int predictedSecondBestLabelIndex) {

    this.predictedSecondBestLabelIndex = predictedSecondBestLabelIndex;
  }


  public int getPredictedSecondBestLabelIndex() {

    return this.predictedSecondBestLabelIndex;
  }


  public void setSplitModelMap(HashMap<String, Model> splitWeightsMap) {

    this.splitModelMap = splitWeightsMap;
  }


  public HashMap<String, Model> getSplitModelMap() {

    return this.splitModelMap;
  }


  public void setSplitModelMapL(HashMap<String, Model> splitWeightsMap) {

    this.splitModelMapL = splitWeightsMap;
  }


  public HashMap<String, Model> getSplitModelMapL() {

    return this.splitModelMapL;
  }


  public void setSplitMapL(HashMap<String, String> splitMapL) {

    this.splitMapL = splitMapL;
  }


  public HashMap<String, String> getSplitMapL() {

    return this.splitMapL;
  }


  public void setSplitWeightsMap(HashMap<String, double[]> splitWeightsMap) {

    this.splitWeightsMap = splitWeightsMap;
  }


  public HashMap<String, double[]> getSplitWeightsMap() {

    return this.splitWeightsMap;
  }


  public void setSplitWeightsMapL(HashMap<String, double[]> splitWeightsMapL) {

    this.splitWeightsMapL = splitWeightsMapL;
  }


  public HashMap<String, double[]> getSplitWeightsMapL() {

    return this.splitWeightsMapL;
  }


  public void setSplitAlphabetsMap(HashMap<String, Alphabet> splitAlphabetsMap) {

    this.splitAlphabetsMap = splitAlphabetsMap;
  }


  public HashMap<String, Alphabet> getSplitAlphabetsMap() {

    return this.splitAlphabetsMap;
  }


  public HashMap<String, String> readSplitFile(String splitFile) throws IOException {

    HashMap<String, String> resultSplitMap = new HashMap<String, String>();
    BufferedReader fp = new BufferedReader(new FileReader(splitFile));
    String line;
    while ((line = fp.readLine()) != null) {
      String[] lineArray = line.split(" ");
      //System.out.println(line);
      resultSplitMap.put(lineArray[0], lineArray[1]);
    }
    fp.close();
    return resultSplitMap;
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
  public void parseCombined(String algorithm, Data d, Archivator arch, Alphabet alphabetParser, boolean noLabels)
      throws IOException {

    long st = System.currentTimeMillis();
    Map<String, Model> feature2ModelMap = readModels(arch);
    //readSplitAlphabets(arch);
    long end = System.currentTimeMillis();
    System.out.println("Time to read model (msec): " + (end - st));
    //gds readSplitModelsL(arch);
    FeatureExtractor fe = new FeatureExtractor();
    final FeatureModel fm;
    final ParsingAlgorithm pa;
    if (algorithm.equals("covington")) {
      fm = new CovingtonFeatureModel(alphabetParser, fe);
      pa = new CovingtonAlgorithm();
      pa.setNumberOfConfigurations(0);

    } else if (algorithm.equals("stack")) {
      fm = new StackFeatureModel(alphabetParser, fe);
      pa = new StackAlgorithm();
      pa.setNumberOfConfigurations(0);
    } else {
      System.err.println("unknown algorithm " + algorithm);
      return;
    }

    pa.initLabelFreqMap();
    pa.setParser(this);
    long start = System.currentTimeMillis();
    int numberOfProcessors = Runtime.getRuntime().availableProcessors();
    // TODO make this configurable
    int threadCount = numberOfProcessors;
    List<Sentence> sentencesList = Arrays.asList(d.getSentences());
    //sentencesList.stream().forEach(x -> pa.processCombined(x, fm, noLabels, this.splitMap));
    // we use our own thread pool to be able to better control parallelization
    ForkJoinPool forkJoinPool = new ForkJoinPool(threadCount);
    try {
      forkJoinPool.submit(
          () -> sentencesList.stream().parallel().forEach(x -> pa.processCombined(x, fm, noLabels, feature2ModelMap))
      ).get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }

    // System.out.println("All worker threads have completed.");
    long end2 = System.currentTimeMillis();
    time += end2 - start;
    System.out.println("No. of threads: " + threadCount);
    System.out.println("Time to parse (msec): " + Double.valueOf(time));
    System.out.println("Speed (sent/s): " + (d.getSentences().length * 1000) / Double.valueOf(time));
    System.out.println("Number of configurations: " + pa.getNumberOfConfigurations());
    System.out.println(
        "Average number of configurations per sentence: " + pa.getNumberOfConfigurations() / d.getSentences().length);
  }


  //TODO the non-parallel version
  public void parseCombinedMem(String algorithm, Data d, Archivator arch, Alphabet alphabetParser, boolean noLabels)
      throws IOException {

    long st = System.currentTimeMillis();
    Map<String, Model> feature2ModelMap = readModels(arch);

    long end = System.currentTimeMillis();
    System.out.println("Time to read model (msec): " + (end - st));
    FeatureExtractor fe = new FeatureExtractor();
    Sentence[] sentences = d.getSentences();
    FeatureModel fm = null;
    ParsingAlgorithm pa = null;
    if (algorithm.equals("covington")) {
      fm = new CovingtonFeatureModel(alphabetParser, fe);
      pa = new CovingtonAlgorithm();
      pa.setNumberOfConfigurations(0);
    } else if (algorithm.equals("stack")) {
      fm = new StackFeatureModel(alphabetParser, fe);
      pa = new StackAlgorithm();
      pa.setNumberOfConfigurations(0);
    } else {
      System.err.println("unkown algorithm " + algorithm);
      return;
    }
    pa.setParser(this);
    long start = System.currentTimeMillis();
    for (int n = 0; n < sentences.length; n++) {
      pa.processCombined(sentences[n], fm, noLabels, feature2ModelMap);
    }
    long end2 = System.currentTimeMillis();
    time += end2 - start;
    System.out.println("Time to parse: " + Double.valueOf(time));
    System.out.println("Speed (sent/s): " + (sentences.length * 1000) / Double.valueOf(time));
    System.out.println("Number of configurations: " + pa.getNumberOfConfigurations());
    System.out.println(
        "Average number of configurations per sentence: " + pa.getNumberOfConfigurations() / sentences.length);
  }
}
