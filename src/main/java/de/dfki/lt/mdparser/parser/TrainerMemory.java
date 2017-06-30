package de.dfki.lt.mdparser.parser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import de.dfki.lt.mdparser.algorithm.CovingtonAlgorithm;
import de.dfki.lt.mdparser.algorithm.ParsingAlgorithm;
import de.dfki.lt.mdparser.algorithm.StackAlgorithm;
import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.config.ConfigKeys;
import de.dfki.lt.mdparser.config.GlobalConfig;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.CovingtonFeatureModel;
import de.dfki.lt.mdparser.features.Feature;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.FeatureVector;
import de.dfki.lt.mdparser.features.StackFeatureModel;

public final class TrainerMemory {

  private TrainerMemory() {

    // private constructor to enforce noninstantiability
  }


  // XXX GN: this is used for training
  public static void trainWithSplittingFromMemory(String conllFileName, String modelFileName)
      throws IOException {

    System.out.println("Start training with createAndTrainWithSplittingFromMemory!");
    Trainer.deleteModelBuildeFolder();

    boolean noLabels = false;
    double bias = -1;

    Map<String, List<FeatureVector>> splitMap = new HashMap<String, List<FeatureVector>>();
    long startTime = System.currentTimeMillis();
    long trainingStartTime = System.currentTimeMillis();

    // GN: internalize CONLL data in 2-Dim sentences
    System.out.println("Internalize training data from: " + conllFileName);

    List<Sentence> sentences = ConllUtils.readConllFile(conllFileName, true);
    Alphabet alpha = new Alphabet();
    FeatureModel model = null;
    ParsingAlgorithm algorithm = null;
    String algorithmId = GlobalConfig.getString(ConfigKeys.ALGORITHM);
    System.out.println(String.format("using algorithm \"%s\"", algorithmId));
    if (algorithmId.equals("covington")) {
      model = new CovingtonFeatureModel(alpha);
      algorithm = new CovingtonAlgorithm();
    } else if (algorithmId.equals("stack")) {
      model = new StackFeatureModel(alpha);
      algorithm = new StackAlgorithm();
    } else {
      System.err.println("unkown algorithm " + algorithmId);
      return;
    }
    int totalConfigurations = 0;
    System.out.println("Create feature vectors for data: " + sentences.size());
    // For each training example x_i = n-th sentence do:
    for (Sentence sent : sentences) {
      // GN: initialize static features (i.e., concrete values for feature+value instance)
      // check static features for current word j and left words i
      // NOTE: the static and dynamic feature-values are added to the alphabet class as a side-effect
      model.initializeStaticFeaturesCombined(sent, true);

      // GN: call parsing control strategy
      // GN: call the parser on each training example to "re-play" the parser configurations
      //     and to compute the operations in form of a list of feature vectors for each state.
      //     this means that all feature functions are applied on the parsed sentence by applying
      //     the feature model in the training mode.
      //     the result is then a list of parser states in form of feature vectors whose values are based
      //     one the specific training example
      List<FeatureVector> featureVectorList = algorithm.train(sent, model, noLabels);
      totalConfigurations += featureVectorList.size();

      // GN: for each feature vector (which represents ONE parser configuration) do
      //     group them into
      for (int i = 0; i < featureVectorList.size(); i++) {

        // the feature vector of the ith configuration of the ith token of the "parsed" sentence:
        // which is a list starting with a label and followed by a list of feature values

        FeatureVector featureVector = featureVectorList.get(i);
        //System.out.println("Sentence " + n + " ... configuration fv-parse " + fv.toString());
        // Sentence 1778 ... fv-parse j#O pj=NNS pjp1=IN pjp2=DT pjp3=NN wfj=results cpj=NNS wfjp1=for m0=IN_DT_NN
        // pjp4=TO pi=null pip1=" wfi=null cpi=null m1=null_" m2=null_null pip2=IN dist=6 wfhi=null phi=null depi=none
        // depldj=null depldi=none deprdi=O m3=null_NNS m4=null_O_none m5=none_null m6=null_"_IN m7=results_null_DT
        // m8=6_NNS_for

        // reference to the first feature-value which is the POS of current token j
        Feature splitFeature = featureVector.getFeatureList().get(0);

        // Basically the POS-feature value for the jth element, e.g., pj=ART
        // It is used for creating splitVal different hashes, which serve as parallel split training files
        String splitVal = splitFeature.getFeatureString();

        // NOTE: difference to function createAndTrainWithSplittingFromDisk():
        //       there the label is used, not the pos-class

        // System.out.println("Sentence " + n + " ... Split value " + splitVal);

        List<FeatureVector> listForThisSplitVal = splitMap.get(splitVal);
        if (listForThisSplitVal == null) {
          listForThisSplitVal = new ArrayList<FeatureVector>();
          splitMap.put(splitVal, listForThisSplitVal);
        }
        listForThisSplitVal.add(featureVector);
      }
    }
    System.out.println("Total configurations: " + totalConfigurations);
    // NOTE: the static and dynamic feature-values are added to the alphabet class as a side-effect
    // via the selected model (CovingtonFeatureModel()) and are now saved in a file.
    // HIERIX
    alpha.writeToFile(GlobalConfig.ALPHA_FILE);

    // merging split maps parser
    Map<String, String> newSplitMap = new LinkedHashMap<String, String>();
    int curCount = 0;
    int t = 10000;
    int n = 1;
    List<FeatureVector> curFeatureVectorList = new ArrayList<FeatureVector>();
    Map<String, List<FeatureVector>> mergedMap = new HashMap<String, List<FeatureVector>>();
    // find all that are > t
    Set<String> toRemove = new HashSet<String>();
    for (Map.Entry<String, List<FeatureVector>> oneEntry : splitMap.entrySet()) {
      String key = oneEntry.getKey();
      List<FeatureVector> listForThisSplitVal = oneEntry.getValue();
      int count = listForThisSplitVal.size();
      if (count > t) {
        newSplitMap.put(key, String.valueOf(n));
        mergedMap.put(String.valueOf(n), listForThisSplitVal);
        n++;
        toRemove.add(key);
      }
    }

    for (Map.Entry<String, List<FeatureVector>> oneEntry : splitMap.entrySet()) {
      String key = oneEntry.getKey();
      if (!toRemove.contains(key)) {
        List<FeatureVector> listForThisSplitVal = oneEntry.getValue();
        curFeatureVectorList.addAll(listForThisSplitVal);
        newSplitMap.put(key, String.valueOf(n));
        int count = splitMap.get(key).size();
        curCount += count;
        if (curCount > t) {
          //mergedMap.put(String.valueOf(n),curList);
          //n++;
          //curCount = 0;
          //curList = new ArrayList<FeatureVector>();
        }
      }
    }
    //if (!curList.isEmpty() && curList.size() > (t/2)) {
    mergedMap.put(String.valueOf(n), curFeatureVectorList);
    //}
    /*
    else {
      List<FeatureVector> previousList = mergedMap.get(String.valueOf(n-1));
      previousList.addAll(curList);
      mergedMap.put(String.valueOf(n), previousList);
    }
    */
    guaranteeOrder(mergedMap, alpha);
    long endTime = System.currentTimeMillis();
    System.out.println("Training data creating time: " + (endTime - startTime) / 1000 + " seconds.");

    //smaller indexes for each model
    Map<String, int[][]> compactMap = compactiseTrainingDataFiles(alpha, mergedMap);

    //train parser
    if (GlobalConfig.SPLIT_FILE.getParent() != null) {
      Files.createDirectories(GlobalConfig.SPLIT_FILE.getParent());
    }
    Files.createDirectories(GlobalConfig.SPLIT_MODELS_FOLDER);
    try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(
        GlobalConfig.SPLIT_FILE, StandardCharsets.UTF_8))) {
      // solver, penalty C, epsilon Eps
      Parameter param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.3);
      boolean[] b = new boolean[100];
      System.out.println("Do training with liblinear ... ");
      startTime = System.currentTimeMillis();
      for (String curFeature : splitMap.keySet()) {
        //System.out.println(curFeature);
        String nValForCurFeature = newSplitMap.get(curFeature);
        n = Integer.valueOf(nValForCurFeature);
        // normalize split file path
        String normalizedPath =
            GlobalConfig.getModelBuildFolder().relativize(GlobalConfig.SPLIT_ADJUST_FOLDER).normalize()
                .resolve(nValForCurFeature + ".txt").toString().replaceAll("\\" + File.separator, "/");
        out.println(String.format("%s %s %s", curFeature, normalizedPath, nValForCurFeature + ".txt"));
        if (!b[n]) {
          curFeatureVectorList = mergedMap.get(nValForCurFeature);
          //System.out.println(nValForCurFeature + " " + mergedMap.get(nValForCurFeature).size());
          Problem prob = readProblem(alpha, false, curFeatureVectorList, bias);
          //myReadProblem(compactMap.get(nValForCurFeature), alpha, false, curList);
          //System.out.println(curList.get(0));
          //System.out.println(curList.get(1));
          //System.out.println(curList.get(2));
          //System.out.println(curList.get(3));
          Linear.disableDebugOutput();
          // DO THE TRAINING
          Model libModel = Linear.train(prob, param);
          //System.out.println(
          //  "mmodel " + nValForCurFeature + ".txt: "
          //  + Double.valueOf(MemoryUtil.deepMemoryUsageOf(model))/1024/1024 + " MB");
          //use weights but with old indexes
          //saveModel(model,compactMap.get(nValForCurFeature), new File(splitModelsDir+"/"+nValForCurFeature+".txt"));
          Path curAlphaPath = GlobalConfig.SPLIT_ALPHA_FOLDER.resolve(nValForCurFeature + ".txt");
          Path curModelPath = GlobalConfig.SPLIT_MODELS_FOLDER.resolve(nValForCurFeature + ".txt");
          alpha.writeToFile(curAlphaPath, compactMap.get(nValForCurFeature));
          // old
          libModel.save(curModelPath.toFile());
          // edit immediately

          Set<Integer> unusedFeatures = Trainer.getUnusedFeatures(curModelPath);

          Alphabet compactAlpha = new Alphabet(curAlphaPath);
          compactAlpha.removeUnusedFeatures(unusedFeatures);
          compactAlpha.writeToFile(curAlphaPath);

          Trainer.removeUnusedFeaturesFromModel(curModelPath, unusedFeatures, compactAlpha.getNumberOfFeatures());
          b[n] = true;
        }
      }
    }

    // recreate alphabet and models
    Trainer.recreateOneAlphabetAndAdjustModels(
        GlobalConfig.ALPHA_FILE, GlobalConfig.SPLIT_ALPHA_FOLDER, GlobalConfig.SPLIT_MODELS_FOLDER);
    long end2 = System.currentTimeMillis();
    System.out.println("... done : " + (end2 - startTime) / 1000 + " seconds.");

    long trainingEndTime = System.currentTimeMillis();
    System.out.println("Complete Training time: " + ((trainingEndTime - trainingStartTime)) + " milliseconds.");

    Archivator arch = new Archivator(modelFileName);
    arch.pack();
  }


  private static Map<String, int[][]> compactiseTrainingDataFiles(
      Alphabet alpha, Map<String, List<FeatureVector>> mergedMap) {

    int numberOfFeatures = alpha.getNumberOfFeatures();
    Map<String, int[][]> compactMap = new HashMap<String, int[][]>();
    for (Map.Entry<String, List<FeatureVector>> oneEntry : mergedMap.entrySet()) {
      String curFeature = oneEntry.getKey();
      List<FeatureVector> curTrainingData = oneEntry.getValue();
      int[][] compactArray = new int[4][];
      int[] newToOld = new int[numberOfFeatures + 1];
      int[] oldToNew = new int[numberOfFeatures + 1];
      int[] newToOldL = new int[alpha.getNumberOfLabels() + 1];
      int[] oldToNewL = new int[alpha.getNumberOfLabels() + 1];
      compactArray[0] = newToOld;
      compactArray[1] = oldToNew;
      compactArray[2] = newToOldL;
      compactArray[3] = oldToNewL;
      compactMap.put(curFeature, compactArray);
      int curMaxIndex = 1;
      //TODO int curLabelMaxIndex = 1;
      Set<Integer> alreadyProcessed = new HashSet<Integer>();
      //TODO Set<Integer> alreadyProcessedLabels = new HashSet<Integer>();
      List<FeatureVector> compactisedTrainingData = new ArrayList<FeatureVector>(curTrainingData.size());
      for (FeatureVector oneFeatureVector : curTrainingData) {
        FeatureVector newFeatureVector = new FeatureVector();
        String label = oneFeatureVector.getLabel();
        //System.out.println(label);
        //TODO Integer labelOld = alpha.getLabelIndexMap().get(label);
        /*
        Integer labelNew = -1;
        if (!alreadyProcessedLabels.contains(labelOld)) {
          labelNew = curLabelMaxIndex;
          oldToNewL[labelOld] = labelNew;
          newToOldL[labelNew] = labelOld;
          alreadyProcessedLabels.add(labelOld);
          curLabelMaxIndex++;
        }
        */
        newFeatureVector.setLabel(label);
        List<Feature> featureList = oneFeatureVector.getFeatureList();
        List<Feature> newFeatureList = new ArrayList<Feature>();
        for (Feature oneFeature : featureList) {
          Integer oldIndex = alpha.getFeatureIndex(oneFeature.getFeatureString());
          if (!alreadyProcessed.contains(oldIndex)) {
            alreadyProcessed.add(oldIndex);
            oldToNew[oldIndex] = curMaxIndex;
            newToOld[curMaxIndex] = oldIndex;
            curMaxIndex++;
          }
          Feature newFeature = oneFeature.clone();
          newFeature.setParserIndex(oldToNew[oldIndex]);
          newFeatureList.add(newFeature);
        }
        newFeatureVector.setFeatureList(newFeatureList);
        compactisedTrainingData.add(newFeatureVector);
      }
      mergedMap.put(curFeature, compactisedTrainingData);
    }
    return compactMap;
  }


  private static Problem readProblem(
      Alphabet alpha, boolean labels, List<FeatureVector> featureVectorList, double bias) {

    int maxIndex = Integer.MIN_VALUE;
    List<Integer> yList = new ArrayList<Integer>();
    List<FeatureNode[]> xList = new ArrayList<FeatureNode[]>();
    for (int i = 0; i < featureVectorList.size(); i++) {
      FeatureVector featureVector = featureVectorList.get(i);
      FeatureNode[] featureNodeArray = featureVector.getLiblinearRepresentation(true, labels, alpha);
      int y = alpha.getLabelIndex(featureVector.getLabel());
      //TODO Integer x = 0;
      yList.add(y);
      xList.add(featureNodeArray);
      maxIndex = Math.max(maxIndex, featureNodeArray[featureNodeArray.length - 1].index);
    }
    return Trainer.constructProblem(yList, xList, maxIndex, bias);
  }


  private static void guaranteeOrder(Map<String, List<FeatureVector>> splitMap, Alphabet alpha) {

    int numberOfLabels = alpha.getNumberOfLabels();
    for (List<FeatureVector> curList : splitMap.values()) {
      for (int i = 0; i < numberOfLabels + 1; i++) {
        curList.add(curList.get(i));
      }
      boolean[] b = new boolean[numberOfLabels + 1];
      int curIndex = 1;
      for (int i = numberOfLabels + 1; i < curList.size() && curIndex < numberOfLabels + 1; i++) {
        FeatureVector featureVector = curList.get(i);
        String label = featureVector.getLabel();
        int labelIndex = alpha.getLabelIndex(label);
        if (!b[labelIndex - 1]) {
          curList.set(labelIndex - 1, featureVector);
          curIndex++;
          b[labelIndex - 1] = true;
        }
      }
    }
  }
}
