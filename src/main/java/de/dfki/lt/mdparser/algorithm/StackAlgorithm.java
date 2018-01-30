package de.dfki.lt.mdparser.algorithm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.FeatureVector;

public class StackAlgorithm extends ParsingAlgorithm {

  public StackAlgorithm() {

    super();
  }


  @Override
  // GN: called by Trainer
  public List<FeatureVector> train(
      Sentence sentence, FeatureModel featureModel, boolean noLabels) {

    List<FeatureVector> featureVectorList = new ArrayList<FeatureVector>();
    String[][] sentArray = sentence.getSentArray();
    int maxToken = 0;
    Stack<Integer> buffer = initBuffer(sentArray.length);
    Stack<Integer> stack = new Stack<Integer>();
    stack.add(0);
    DependencyStructure curDepStruct = new DependencyStructure(sentArray.length);
    DependencyStructure goldDepStruct = initGoldDepStruct(sentArray);
    StackParserState curState = new StackParserState(stack, buffer, sentence, curDepStruct);
    while (!curState.isTerminal()) {
      FeatureVector featureVector = featureModel.applyCombined(curState, true, noLabels);
      /*
      System.out.print(
          curState.getStackToken(0) + " " + curState.getBufferToken(0) + " " + maxi + " " + stack
              + " " + buffer + " ");
      */
      String label =
          findOutCorrectLabel2Combined(curState.getStack(), curState.getBufferToken(0), sentArray,
              goldDepStruct);
      /*
      System.out.println(
          label + " " + curDepStruct.getDependencies() + " "
              + curDepStruct.getDependencies().size());
      */
      featureVector.setLabel(label);
      String labelTrans = "";
      if (label.contains("#")) {
        labelTrans = label.split("#")[0];
      }
      if (labelTrans.equals("j")) {
        sentArray[curState.getStackToken(0) - 1][8] = String.valueOf(curState.getBufferToken(0));
        String depRel = sentArray[curState.getStackToken(0) - 1][7];
        sentArray[curState.getStackToken(0) - 1][9] = depRel;
        curDepStruct.addDependency(
            new Dependency(curState.getStackToken(0), curState.getBufferToken(0), depRel));

        stack.pop();
      } else if (labelTrans.equals("i")) {
        sentArray[curState.getBufferToken(0) - 1][8] = String.valueOf(curState.getStackToken(0));
        String depRel = sentArray[curState.getBufferToken(0) - 1][7];
        sentArray[curState.getBufferToken(0) - 1][9] = depRel;
        curDepStruct.addDependency(
            new Dependency(curState.getBufferToken(0), curState.getStackToken(0), depRel));
        stack.push(buffer.remove(0));
      } else if (label.equals("reduce")) {
        stack.pop();
      } else {
        if (!buffer.isEmpty()) {
          stack.push(buffer.remove(0));
        }
      }
      if (buffer.isEmpty()) {
        curState.setTerminal(true);
      }
      featureModel.getParserAlphabet().addLabel(label);
      featureVectorList.add(featureVector);
      maxToken = Math.max(curState.getBufferToken(0), maxToken);
    }
    return featureVectorList;
  }


  private static DependencyStructure initGoldDepStruct(String[][] sentArray) {

    DependencyStructure goldDepStruct = new DependencyStructure(sentArray.length);
    for (int i = 0; i < sentArray.length; i++) {
      goldDepStruct.addDependency(
          new Dependency(
              Integer.valueOf(sentArray[i][0]),
              Integer.valueOf(sentArray[i][6]),
              sentArray[i][7]));
    }
    return goldDepStruct;
  }


  private static String findOutCorrectLabel2Combined(
      Stack<Integer> stack, int i, String[][] sentArray, DependencyStructure goldDepStruct) {

    int j = stack.get(stack.size() - 1);
    String label = "";
    if (j != 0 && Integer.valueOf(sentArray[j - 1][6]) == i) {
      label = "j";
      label += "#" + sentArray[j - 1][7];
      return label;
    } else if (Integer.valueOf(sentArray[i - 1][6]) == j) {
      label = "i";
      label += "#" + sentArray[i - 1][7];
      return label;
    } else if (j != 0 && (sentArray[j - 1][6] != null)) {
      Integer parent = Integer.valueOf(sentArray[i - 1][6]);
      Set<Integer> dependents = goldDepStruct.getDependents().get(i);
      if (dependents != null) {
        Iterator<Integer> depIter = dependents.iterator();
        while (depIter.hasNext()) {
          Integer curDep = depIter.next();
          if (stack.contains(curDep)) {
            return "reduce";
          }
        }
      }
      if (stack.contains(parent)) {
        return "reduce";
      } else {
        return "shift";
      }
    } else {
      return "shift";
    }
  }


  @Override
  public void parse(
      Sentence sent, FeatureModel featureModel, boolean noLabels,
      Map<String, Model> feature2ModelMap) {

    String[][] sentArray = sent.getSentArray();
    Stack<Integer> buffer = initBuffer(sentArray.length);
    Stack<Integer> stack = new Stack<Integer>();
    stack.add(0);
    DependencyStructure curDepStruct = new DependencyStructure(sentArray.length);
    StackParserState curState = new StackParserState(stack, buffer, sent, curDepStruct);
    while (!curState.isTerminal()) {
      super.incNumberOfConfigurations();
      FeatureVector featureVector = featureModel.applyCombined(curState, true, noLabels);
      /*
      System.out.print(sent.getRootPosition() + " " + curState.getStackToken(0) + " "
          + curState.getBufferToken(0)
          + " " + stack + " " + buffer + " ");
      */
      Model curModel = feature2ModelMap.get(featureVector.getFeature("pj").getFeatureString());
      if (curModel == null) {
        curModel = feature2ModelMap.values().iterator().next();
      }
      int labelInt = (int)Linear.predict(
          curModel,
          featureVector.getLiblinearRepresentation(false, false, featureModel.getParserAlphabet()));
      String label = featureModel.getParserAlphabet().getLabel(labelInt);
      /*
      System.out.println(label + " " + curDepStruct.getDependencies() + " "
          + curDepStruct.getDependencies().size());
      */
      String labelTrans = "";
      String labelDepRel = "";
      if (label.contains("#")) {
        labelTrans = label.split("#")[0];
        labelDepRel = label.split("#")[1];
      }
      featureVector.setLabel(label);
      if (labelTrans.equals("j") && 0 != curState.getStackToken(0)) {
        sentArray[curState.getStackToken(0) - 1][6] = String.valueOf(curState.getBufferToken(0));
        String depRel = labelDepRel;
        sentArray[curState.getStackToken(0) - 1][7] = depRel;
        sentArray[curState.getStackToken(0) - 1][9] = depRel;
        curDepStruct.addDependency(
            new Dependency(curState.getStackToken(0), curState.getBufferToken(0), depRel));
        stack.pop();
      } else if (labelTrans.equals("i")) {
        int head = curState.getStackToken(0);
        sentArray[curState.getBufferToken(0) - 1][6] = String.valueOf(head);
        String depRel = labelDepRel;
        sentArray[curState.getBufferToken(0) - 1][7] = depRel;
        sentArray[curState.getBufferToken(0) - 1][9] = depRel;
        curDepStruct.addDependency(
            new Dependency(curState.getBufferToken(0), curState.getStackToken(0), depRel));
        stack.push(buffer.remove(0));
        if (head == 0) {
          sent.setRootPosition(curState.getBufferToken(0));
        }
      } else if (label.equals("reduce")) {
        if (sentArray[curState.getStackToken(0) - 1][6] == null
            || sentArray[curState.getStackToken(0) - 1][6].equals("_")) {
          label = "shift";
          stack.push(buffer.remove(0));
        } else if (stack.peek() != 0) {
          stack.pop();
        }
      } else {
        if (!buffer.isEmpty()) {
          stack.push(buffer.remove(0));
        }
      }
      if (buffer.isEmpty()) {
        curState.setTerminal(true);
      }
    }
    postprocess(sentArray, sent);
  }


  private static void postprocess(String[][] sentArray, Sentence sent) {

    for (int j = 0; j < sentArray.length; j++) {
      if (sentArray[j][6] == null || sentArray[j][6].equals("_")) {
        int root = sent.getRootPosition();
        if (root == -1) {
          root = 1;
        }
        sentArray[j][6] = String.valueOf(root);
      }
    }
  }


  private static Stack<Integer> initBuffer(int length) {

    Stack<Integer> buffer = new Stack<Integer>();
    for (int j = 1; j < length + 1; j++) {
      buffer.push(j);
    }
    return buffer;
  }
}
