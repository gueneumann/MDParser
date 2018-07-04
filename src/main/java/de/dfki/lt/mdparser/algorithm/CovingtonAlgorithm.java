package de.dfki.lt.mdparser.algorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.CovingtonFeatureModel;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.FeatureVector;

public class CovingtonAlgorithm extends ParsingAlgorithm {


  public CovingtonAlgorithm() {

    super();
  }


  @Override
  public List<FeatureVector> train(Sentence sent, FeatureModel featureModel, boolean noLabels) {

    List<FeatureVector> featureVectorList = new ArrayList<FeatureVector>();

    //System.err.println("Do training with:\n");;
    //System.err.println(sent.toString());

    // sent is the conll structure with correct labeled dependency tree
    String[][] sentArray = sent.getSentArray();
    //double rootProbabilities[] = new double[sentArray.length];
    DependencyStructure curDepStruct = new DependencyStructure(sentArray.length);
    CovingtonFeatureModel covFeatureModel = (CovingtonFeatureModel)featureModel;
    covFeatureModel.initializeStaticFeaturesCombined(sent, false);
    // XXX GN: for all possible pairs (i,j)
    // determine all permissible states and for these its feature vector

    for (int j = 1; j < sentArray.length + 1; j++) {
      for (int i = j - 1; i >= 0; i--) {
        // Compute all possible edges from j downto i

        //System.out.println("Span: " + j + ", " + i);

        CovingtonParserState state = new CovingtonParserState(j, i, sent, curDepStruct);
        if (state.isPermissible()) {
          super.incNumberOfConfigurations();
          FeatureVector featureVector = covFeatureModel.applyCombined(state, true, noLabels);
          // GN: determine the name of the operation/class using the information from
          // relevant tokens of the sentence
          // and create the class/label name from it (an instance of left-arc, right-arc,
          // shift, terminate)
          String label = findOutCorrectLabelCombined(j, i, sentArray);
          //System.out.println(i+" "+j+" "+label+" ... "+featureVector.toString());
          String labelTrans = "";
          if (label.contains("#")) {
            labelTrans = label.split("#")[0];
          }

          if (labelTrans.equals("j")) {
            String depRel = sentArray[j - 1][7];
            sentArray[j - 1][9] = depRel;
            curDepStruct.addDependency(new Dependency(j, i, depRel));
          } else if (labelTrans.equals("i")) {
            String depRel = sentArray[i - 1][7];
            sentArray[i - 1][9] = depRel;
            curDepStruct.addDependency(new Dependency(i, j, depRel));
          } else if (label.equals("terminate")) {
            i = -1;
          }
          covFeatureModel.getParserAlphabet().addLabel(label);
          featureVector.setLabel(label);
          featureVectorList.add(featureVector);
        }
      }
    }
    //postprocess(sentArray, sent,curDepStruct);

//    System.out.println(sent.toString());
//    int cntP = 1;
//    for (FeatureVector x : featureVectorList){
//      System.out.println(cntP +": " + x.toString());
//      cntP++;
//    }
//    System.out.println("----------------");


    return featureVectorList;
  }


  public static String findOutCorrectLabelCombined(int j, int i, String[][] sentArray) {

    // 1 - left arc
    // 2 - right arc
    // 3 - shift
    String label = "";
    if (Integer.valueOf(sentArray[j - 1][6]) == i) {
      label = "j";
      label += "#" + sentArray[j - 1][7];
      return label;
    } else if (i == 0) {
      label = "shift";
      return label;
    } else if (Integer.valueOf(sentArray[i - 1][6]) == j) {
      label = "i";
      label += "#" + sentArray[i - 1][7];
      return label;
    } else {
      label = "shift";
    }
    if (label.equals("shift")) {
      boolean terminate = true;
      if (Integer.valueOf(sentArray[j - 1][6]) < i) {
        terminate = false;
      }
      for (int k = i; k > 1; k--) {
        if (Integer.valueOf(sentArray[k - 1][6]) == j) {
          terminate = false;
        }
      }
      if (terminate) {
        label = "terminate";
      }
    }
    return label;
  }


  // GN: Used in nereid.parser.MDPApi
  @Override
  public void parse(
      Sentence sent, FeatureModel featureModel, boolean noLabels,
      Map<String, Model> feature2ModelMap) {
// For tracing
//    List<FeatureVector> featureVectorList = new ArrayList<FeatureVector>();

    String[][] sentArray = sent.getSentArray();
    sent.setRootPosition(-1);
    DependencyStructure curDepStruct = new DependencyStructure(sentArray.length);
    CovingtonFeatureModel covFeatureModel = (CovingtonFeatureModel)featureModel;
    covFeatureModel.initializeStaticFeaturesCombined(sent, false);
    for (int j = 1; j < sentArray.length + 1; j++) {
      for (int i = j - 1; i >= 0; i--) {
        CovingtonParserState state = new CovingtonParserState(j, i, sent, curDepStruct);
        if (state.isPermissible()) {
          super.incNumberOfConfigurations();
          FeatureVector featureVector = covFeatureModel.applyCombined(state, false, noLabels);
          //System.out.println(featureVector);

          Model curModel = feature2ModelMap.get(featureVector.getFeature("pj").getFeatureString());
          if (curModel == null) {
            // TODO don't use a random model here!!! -> non-deterministic
            curModel = feature2ModelMap.values().iterator().next();
          }
          //System.out.println(mName+" "+curAlphabet);
          //System.out.println(+" "+curModel+" "+fm2+" "+fm2.getAlphabetParser());
          int labelInt = (int)Linear.predict(
              curModel, featureVector.getLiblinearRepresentation(
                  false, false, covFeatureModel.getParserAlphabet()));

          String label = covFeatureModel.getParserAlphabet().getLabel(labelInt);

//          featureVector.setLabel(label);
//          featureVectorList.add(featureVector);

          String labelTrans = "";
          String labelDepRel = "";
          if (label.contains("#")) {
            labelTrans = label.split("#")[0];
            labelDepRel = label.split("#")[1];
          }
          //System.out.println(j+" "+i+" "+ps.isL1Permissible()+" "+ps.isL2Permissible());
          if (labelTrans.equals("j") && state.isL1Permissible()) {

            sentArray[j - 1][6] = String.valueOf(i);
            String depRel = labelDepRel;
            sentArray[j - 1][7] = depRel;
            sentArray[j - 1][9] = depRel;
            curDepStruct.addDependency(new Dependency(j, i, labelDepRel));
            if (i == 0) {
              sent.setRootPosition(j);
            }
          } else if (labelTrans.equals("i") && state.isL2Permissible()) {

            sentArray[i - 1][6] = String.valueOf(j);
            String depRel = labelDepRel;
            sentArray[i - 1][7] = depRel;
            sentArray[i - 1][9] = depRel;
            curDepStruct.addDependency(new Dependency(i, j, labelDepRel));
          } else if (label.equals("terminate")) {
            i = -1;
          }
        }
      }
    }

    // For tracing
//  System.out.println(sent.toString());
//  int cntP = 1;
//  for (FeatureVector x : featureVectorList){
//    System.out.println(cntP +": " + x.toString());
//    cntP++;
//  }
//  System.out.println("----------------");

    postprocess(sentArray, sent, curDepStruct);
  }


  private static void postprocess(
      String[][] sentArray, Sentence sent, DependencyStructure depStruct) {

    Set<Integer> headless = new HashSet<Integer>();
    for (int j = 0; j < sentArray.length; j++) {
      if (sentArray[j][6] == null || sentArray[j][6].equals("_") || sentArray[j][6].equals("-1")) {
        headless.add(j);
      }
    }
    //System.out.println(sent.getRootPosition()+" "+headless);
    if (!headless.isEmpty()) {
      Integer rootPosition = sent.getRootPosition();
      if (rootPosition == null || rootPosition == -1) {
        boolean foundWithHead = false;
        int ind = 1;
        while (!foundWithHead) {
          if (!headless.contains(ind + 1)) {
            foundWithHead = true;
          } else {
            ind++;
          }
        }
        //System.out.println("ind: "+ind);
        Stack<Integer> st = new Stack<Integer>();
        st.add(ind);
        boolean foundRoot = false;
        while (!st.isEmpty() && !foundRoot) {
          int curDep = st.pop();
          int curHead = depStruct.getHeads()[curDep];
          //System.out.println(curDep+" "+curHead);
          if (curHead >= 0) {
            st.add(curHead);
          } else {
            foundRoot = true;
            rootPosition = curDep;
          }
        }
      }
      Iterator<Integer> iter = headless.iterator();
      while (iter.hasNext()) {
        int curJ = iter.next();

        if (rootPosition != null && curJ + 1 != rootPosition) {
          sentArray[curJ][6] = String.valueOf(rootPosition);
          sentArray[curJ][7] = "NMOD";
          //sentArray[curJ][7] = "nmod__adj";
          //String mostFreqLabel = super.findTheMostFreqLabel(sentArray[curJ][3]);
          //sentArray[curJ][7] = mostFreqLabel;
        } else {
          sentArray[curJ][6] = "0";
          sentArray[curJ][7] = "ROOT";
          rootPosition = curJ + 1;
        }
      }
    }
  }
}
