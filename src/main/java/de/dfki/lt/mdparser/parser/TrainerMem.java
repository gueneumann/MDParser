package de.dfki.lt.mdparser.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import de.dfki.lt.mdparser.data.Data;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.CovingtonFeatureModel;
import de.dfki.lt.mdparser.features.Feature;
import de.dfki.lt.mdparser.features.FeatureExtractor;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.FeatureVector;
import de.dfki.lt.mdparser.features.StackFeatureModel;
import de.dfki.lt.mdparser.model.ModelEditor;

public class TrainerMem {

  private double bias = -1;
  private Problem prob = null;
  // solver, penalty C, epsilon Eps
  private Parameter param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.3);
  private int numberOfFeatures;
  private int maxLabelParser;
  private int maxLabelLabeler;


  public int getNumberOfFeatures() {

    return this.numberOfFeatures;
  }


  public void setNumberOfFeatures(int numberOfFeatures) {

    this.numberOfFeatures = numberOfFeatures;
  }


  public int getMaxLabelParser() {

    return this.maxLabelParser;
  }


  public void setMaxLabelParser(int maxLabelParser) {

    this.maxLabelParser = maxLabelParser;
  }


  public int getMaxLabelLabeler() {

    return this.maxLabelLabeler;
  }


  public void setMaxLabelLabeler(int maxLabelLabeler) {

    this.maxLabelLabeler = maxLabelLabeler;
  }


  public Parameter getParam() {

    return this.param;
  }


  public void setParam(Parameter param) {

    this.param = param;
  }


  public Problem getProblem() {

    return this.prob;
  }


  public void myReadProblem(Alphabet alpha, boolean labels, List<FeatureVector> fvList) {

    int max_index = Integer.MIN_VALUE;
    List<Integer> yList = new ArrayList<Integer>();
    List<FeatureNode[]> xList = new ArrayList<FeatureNode[]>();
    for (int i = 0; i < fvList.size(); i++) {
      FeatureVector fv = fvList.get(i);
      FeatureNode[] fnArray = fv.getLiblinearRepresentation(true, labels, alpha);
      Integer y = Integer.valueOf(alpha.getLabelIndexMap().get(fv.getLabel()));
      //TODO Integer x = 0;
      yList.add(y);
      xList.add(fnArray);
      max_index = Math.max(max_index, fnArray[fnArray.length - 1].index);
    }
    this.prob = constructProblem(yList, xList, max_index);
  }


  private HashMap<String, int[][]> compactiseTrainingDataFiles(Alphabet alphaParser,
      HashMap<String, List<FeatureVector>> mergedMap) {

    int maxIndex = alphaParser.getMaxIndex();
    alphaParser.createIndexToValueArray();
    HashMap<String, int[][]> compactMap = new HashMap<String, int[][]>();
    Iterator<String> iter = mergedMap.keySet().iterator();
    while (iter.hasNext()) {
      String curFeature = iter.next();
      List<FeatureVector> curTrainingData = mergedMap.get(curFeature);
      int[][] compactArray = new int[4][];
      int[] newToOld = new int[maxIndex + 1];
      int[] oldToNew = new int[maxIndex + 1];
      int[] newToOldL = new int[alphaParser.getMaxLabelIndex() + 1];
      int[] oldToNewL = new int[alphaParser.getMaxLabelIndex() + 1];
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
      for (int i = 0; i < curTrainingData.size(); i++) {
        FeatureVector fv = curTrainingData.get(i);
        FeatureVector newFv = new FeatureVector(true);
        String label = fv.getLabel();
        //System.out.println(label);
        //TODO Integer labelOld = alphaParser.getLabelIndexMap().get(label);
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
        newFv.setLabel(label);
        List<Feature> fList = fv.getfList();
        List<Feature> newFList = new ArrayList<Feature>();
        for (int k = 0; k < fList.size(); k++) {
          Feature f = fList.get(k);
          Integer oldIndex = alphaParser.getFeatureIndex(f.getFeatureString());
          if (!alreadyProcessed.contains(oldIndex)) {
            alreadyProcessed.add(oldIndex);
            oldToNew[oldIndex] = curMaxIndex;
            newToOld[curMaxIndex] = oldIndex;
            curMaxIndex++;
          }
          Feature newF = f.clone();
          newF.setIndexParser(oldToNew[oldIndex]);
          newFList.add(newF);
        }
        newFv.setfList(newFList);
        compactisedTrainingData.add(newFv);
      }
      mergedMap.put(curFeature, compactisedTrainingData);
    }
    return compactMap;


  }


  private void guaranteeOrder(HashMap<String, List<FeatureVector>> splitMap, Alphabet alpha) {

    Iterator<String> iter = splitMap.keySet().iterator();
    while (iter.hasNext()) {
      String key = iter.next();
      List<FeatureVector> curList = splitMap.get(key);
      for (int i = 0; i < alpha.getMaxLabelIndex(); i++) {
        curList.add(curList.get(i));
      }
      boolean[] b = new boolean[alpha.getMaxLabelIndex()];
      int curIndex = 1;
      for (int i = alpha.getMaxLabelIndex(); i < curList.size() && curIndex < alpha.getMaxLabelIndex(); i++) {
        FeatureVector fv = curList.get(i);
        String label = fv.getLabel();
        int labelIndex = alpha.getLabelIndexMap().get(label);
        if (!b[labelIndex - 1]) {
          curList.set(labelIndex - 1, fv);
          curIndex++;
          b[labelIndex - 1] = true;
        }
      }
    }
  }


  // XXX GN: this is used for training
  public void createAndTrainWithSplittingFromMemory(String algorithm,
      String inputFile, String splitModelsDir,
      String alphabetFileParser, String alphabetFileLabeler,
      String splitFile) throws IOException {

    boolean noLabels = false;
    HashMap<String, List<FeatureVector>> splitMap = new HashMap<String, List<FeatureVector>>();
    long st = System.currentTimeMillis();

    System.out.println("Start training with createAndTrainWithSplittingFromMemory!");

    // GN: internalize CONLL data in 2-Dim sentences
    System.out.println("Internalize training data from: " + inputFile);

    Data d = new Data(inputFile, true);
    Alphabet alphaParser = new Alphabet();
    FeatureExtractor fe = new FeatureExtractor();
    Sentence[] sentences = d.getSentences();
    FeatureModel fm = null;
    ParsingAlgorithm pa = null;
    if (algorithm.equals("covington")) {
      fm = new CovingtonFeatureModel(alphaParser, fe);
      pa = new CovingtonAlgorithm();
    } else if (algorithm.equals("stack")) {
      fm = new StackFeatureModel(alphaParser, fe);
      pa = new StackAlgorithm();
    } else {
      System.err.println("unkown algorithm " + algorithm);
      return;
    }
    int totalConfigurations = 0;
    File splitA = new File("splitA");
    splitA.mkdir();
    System.out.println("Create feature vectors for data: " + sentences.length);
    // For each training example x_i = n-th sentence do:
    for (int n = 0; n < sentences.length; n++) {
      Sentence sent = sentences[n];
      // GN: initialize static features (i.e., concrete values for feature+value instance)
      // check static features for current word j and left words i
      // NOTE: the static and dynamic feature-values are added to the alphabet class as a side-effect
      fm.initializeStaticFeaturesCombined(sent, true);

      // GN: call parsing control strategy
      // GN: call the parser on each training example to "re-play" the parser configurations
      //     and to compute the operations in form of a list of feature vectors for each state.
      //     this means that all feature functions are applied on the parsed sentence by applying
      //     the feature model in the training mode.
      //     the result is then a list of parser states in form of feature vectors whose values are based
      //     one the specific training example
      List<FeatureVector> parserList = pa.processCombined(sent, fm, noLabels);
      totalConfigurations += parserList.size();

      // GN: for each feature vector (which represents ONE parser configuration) do
      //     group them into
      for (int i = 0; i < parserList.size(); i++) {

        // the feature vector of the ith configuration of the ith token of the "parsed" sentence:
        // which is a list starting with a label and followed by a list of feature values

        FeatureVector fv = parserList.get(i);
        //System.out.println("Sentence " + n + " ... configuration fv-parse " + fv.toString());
        // Sentence 1778 ... fv-parse j#O pj=NNS pjp1=IN pjp2=DT pjp3=NN wfj=results cpj=NNS wfjp1=for m0=IN_DT_NN
        // pjp4=TO pi=null pip1=" wfi=null cpi=null m1=null_" m2=null_null pip2=IN dist=6 wfhi=null phi=null depi=none
        // depldj=null depldi=none deprdi=O m3=null_NNS m4=null_O_none m5=none_null m6=null_"_IN m7=results_null_DT
        // m8=6_NNS_for

        // reference to the first feature-value which is the POS of current token j
        Feature splitFeature = fv.getfList().get(0);

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
        listForThisSplitVal.add(fv);
      }
    }
    System.out.println("Total configurations: " + totalConfigurations);
    // NOTE: the static and dynamic feature-values are added to the alphabet class as a side-effect
    // via the selected model (CovingtonFeatureModel()) and are now saved in a file.
    // HIERIX
    alphaParser.printToFile(alphabetFileParser);
    setMaxLabelParser(alphaParser.getMaxLabelIndex());

    //merging split maps parser
    this.numberOfFeatures = alphaParser.getMaxIndex();
    HashMap<String, String> newSplitMap = new HashMap<String, String>();
    Iterator<String> keyIter = splitMap.keySet().iterator();
    int curCount = 0;
    int t = 10000;
    int n = 1;
    List<FeatureVector> curList = new ArrayList<FeatureVector>();
    HashMap<String, List<FeatureVector>> mergedMap = new HashMap<String, List<FeatureVector>>();
    //find all that are > t
    Set<String> toRemove = new HashSet<String>();
    while (keyIter.hasNext()) {
      String key = keyIter.next();
      List<FeatureVector> listForThisSplitVal = splitMap.get(key);
      int count = splitMap.get(key).size();
      if (count > t) {
        newSplitMap.put(key, String.valueOf(n));
        mergedMap.put(String.valueOf(n), listForThisSplitVal);
        n++;
        toRemove.add(key);
      }
    }
    keyIter = splitMap.keySet().iterator();
    while (keyIter.hasNext()) {
      String key = keyIter.next();
      if (!toRemove.contains(key)) {
        List<FeatureVector> listForThisSplitVal = splitMap.get(key);
        curList.addAll(listForThisSplitVal);
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
    mergedMap.put(String.valueOf(n), curList);
    //}
    /*
    else {
      List<FeatureVector> previousList = mergedMap.get(String.valueOf(n-1));
      previousList.addAll(curList);
      mergedMap.put(String.valueOf(n), previousList);
    }
    */
    guaranteeOrder(mergedMap, alphaParser);
    long end = System.currentTimeMillis();
    System.out.println("Training data creating time: " + (end - st) / 1000 + " seconds.");

    //smaller indexes for each model
    HashMap<String, int[][]> compactMap = compactiseTrainingDataFiles(alphaParser, mergedMap);

    //train parser
    FileOutputStream out = new FileOutputStream(splitFile);
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter bw = new BufferedWriter(or);
    Iterator<String> iter = splitMap.keySet().iterator();
    boolean[] b = new boolean[100];
    System.out.println("Do training with liblinear ... ");
    st = System.currentTimeMillis();
    while (iter.hasNext()) {
      String curFeature = iter.next();
      //System.out.println(curFeature);
      String nValForCurFeature = newSplitMap.get(curFeature);
      n = Integer.valueOf(nValForCurFeature);
      bw.append(curFeature + " " + "split/" + nValForCurFeature + ".txt " + nValForCurFeature + ".txt\n");
      if (!b[n]) {
        curList = mergedMap.get(nValForCurFeature);
        //System.out.println(nValForCurFeature+" "+mergedMap.get(nValForCurFeature).size());
        myReadProblem(alphaParser, false, curList);
        //myReadProblem(compactMap.get(nValForCurFeature),alphaParser, false, curList);
        //System.out.println(curList.get(0));System.out.println(curList.get(1));
        //System.out.println(curList.get(2));System.out.println(curList.get(3));
        Linear.disableDebugOutput();
        // DO THE TRAINING
        Model model = Linear.train(this.prob, this.param);
        //System.out.println(
        //  "mmodel "+nValForCurFeature+".txt: " +Double.valueOf(MemoryUtil.deepMemoryUsageOf(model))/1024/1024+" MB");
        //use weights but with old indexes
        //saveModel(model,compactMap.get(nValForCurFeature), new File(splitModelsDir+"/"+nValForCurFeature+".txt"));
        saveAlphabet(alphaParser, model, compactMap.get(nValForCurFeature),
            new File("splitA/" + nValForCurFeature + ".txt"));
        // old
        model.save(new File(splitModelsDir + "/" + nValForCurFeature + ".txt"));
        // edit immediately
        ModelEditor me = new ModelEditor(new File(splitModelsDir + "/" + nValForCurFeature + ".txt"),
            "splitA/" + nValForCurFeature + ".txt", true);
        me.editAlphabetAndModel("splitA/" + nValForCurFeature + ".txt",
            splitModelsDir + "/" + nValForCurFeature + ".txt");
        b[n] = true;
      }
    }
    bw.close();
    // recreate alphabet and models
    recreateOneAlphabetAndAdjustModels(alphabetFileParser, "splitA", splitModelsDir);
    long end2 = System.currentTimeMillis();
    System.out.println("... done : " + (end2 - st) / 1000 + " seconds.");

  }


  private void recreateOneAlphabetAndAdjustModels(String alphabetFile, String splitAlphaDir, String splitModelsDir)
      throws IOException {

    Alphabet alpha = unionAlphabets(alphabetFile, splitAlphaDir);
    restoreModels(splitModelsDir, alpha, splitAlphaDir);
    alpha.printToFile(alphabetFile);
  }


  private void restoreModels(String splitModelsDir, Alphabet alpha, String splitA) throws IOException {

    File[] models = new File(splitModelsDir).listFiles();
    alpha.createIndexToValueArray();
    for (int i = 0; i < models.length; i++) {
      System.out.println("Model file: " + models[i]);
      Model model = Linear.loadModel(models[i]);
      double[] wArray = model.getFeatureWeights();
      String alphaName = splitA + "/" + models[i].getName();
      Alphabet a = new Alphabet(new FileInputStream(new File(alphaName)));
      int numberOfClasses = model.getNrClass();
      FileOutputStream out = new FileOutputStream(models[i]);
      OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
      BufferedWriter bw = new BufferedWriter(or);
      bw.append("solver_type MCSVM_CS\n");
      bw.append("nr_class " + model.getNrClass() + "\nlabel ");

      for (int k = 0; k < model.getLabels().length; k++) {
        bw.append(model.getLabels()[k] + " ");
      }
      bw.append("\n");
      String[] features = alpha.getIndexToValueArray();
      //System.out.println(features);
      boolean notFound = true;
      Set<String> thisAlphabetFeatures = a.getValueToIndexMap().keySet();
      int lastIndex = -1;
      for (int k = features.length - 1; k > 1 && notFound; k--) {
        if (thisAlphabetFeatures.contains(features[k])) {
          lastIndex = k;
          notFound = false;
        }
      }
      bw.append("nr_feature " + (lastIndex - 1) + "\n");
      bw.append("bias " + model.getBias() + "\nw\n");
      HashMap<String, Integer> valToIndexMap = a.getValueToIndexMap();
      for (int k = 1; k < lastIndex; k++) {
        String feature = features[k];
        Integer oldIndex = valToIndexMap.get(feature);
        if (oldIndex == null) {
          for (int m = 0; m < numberOfClasses; m++) {
            bw.append("0 ");
          }
        } else {
          for (int l = 0; l < numberOfClasses; l++) {
            double curWeight = wArray[(oldIndex - 1) * numberOfClasses + l];
            bw.append(String.valueOf(curWeight) + " ");
          }
        }
        bw.append("\n");

      }
      bw.close();
    }
  }


  private Alphabet unionAlphabets(String alphabetFile, String splitAlphaDir) throws IOException {

    Alphabet alpha = new Alphabet();
    File[] alphabets = new File(splitAlphaDir).listFiles();
    HashMap<String, Integer> map = alpha.getValueToIndexMap();
    for (int i = 0; i < alphabets.length; i++) {
      Alphabet curAlpha = new Alphabet(new FileInputStream(alphabets[i]));
      if (alpha.getLabelIndexMap().keySet().isEmpty()) {
        alpha.setLabelIndexMap(curAlpha.getLabelIndexMap());
        /*
        String[] labels = curAlpha.getIndexLabelArray();
        for (int k=0; k < labels.length;k++) {
          alpha.addLabel(labels[k]);
        }
        */
        alpha.setMaxLabelIndex(curAlpha.getLabelIndexMap().size() + 1);
      }
      String[] features = curAlpha.getIndexToValueArray();
      //System.out.println(curAlpha.getValueToIndexMap());
      for (int k = 1; k < features.length; k++) {
        Integer index = map.get(features[k]);
        //System.out,.println(features)
        if (index == null) {
          alpha.addFeature(features[k]);
          //System.out.println(features[k]);
        }
      }
    }
    return alpha;
  }


  void saveAlphabet(Alphabet alphaParser, Model model, int[][] compactArray, File file) throws IOException {

    int[] newToOld = compactArray[0];
    FileOutputStream out = new FileOutputStream(file);
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter bw = new BufferedWriter(or);
    String[] indexLabelArray = alphaParser.getIndexLabelArray();
    for (int i = 1; i < alphaParser.getMaxLabelIndex(); i++) {
      bw.append(i + " " + indexLabelArray[i] + "\n");
    }
    // GN: seems to cause problem in training with algorithm=stack
    bw.append("\n");
    String[] indexToValue = alphaParser.getIndexToValueArray();
    boolean notFinished = true;
    for (int i = 1; notFinished && i < newToOld.length; i++) {
      int newIndex = i;
      int oldIndex = newToOld[i];
      if (oldIndex == 0) {
        notFinished = false;
      } else {
        String stringValue = indexToValue[oldIndex];
        bw.append(newIndex + " " + stringValue + "\n");
      }
    }
    bw.close();
  }


  private Problem constructProblem(List<Integer> vy, List<FeatureNode[]> vx, int max_index) {

    Problem resultProb = new Problem();
    resultProb.bias = this.bias;
    resultProb.l = vy.size();
    resultProb.n = max_index;
    if (this.bias >= 0) {
      resultProb.n++;
    }
    resultProb.x = new FeatureNode[resultProb.l][];
    for (int i = 0; i < resultProb.l; i++) {
      resultProb.x[i] = vx.get(i);

      if (this.bias >= 0) {
        assert resultProb.x[i][resultProb.x[i].length - 1] == null;
        resultProb.x[i][resultProb.x[i].length - 1] = new FeatureNode(max_index + 1, this.bias);
      } else {
        assert resultProb.x[i][resultProb.x[i].length - 1] != null;
      }
    }

    //GN, May, 2016
    // prob.y = new int[prob.l];
    resultProb.y = new double[resultProb.l];
    for (int i = 0; i < resultProb.l; i++) {
      resultProb.y[i] = vy.get(i);
    }

    return resultProb;
  }


}
