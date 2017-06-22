package de.dfki.lt.mdparser.features;

import de.dfki.lt.mdparser.data.Sentence;

public final class FeatureExtractor {

  private FeatureExtractor() {

    // private constructor to enforce noninstantiability
  }


  // return POS for token index in sent
  // -> is it basically index because array and deptree are counted from 0/1 ?!
  public static Feature createFeatureForPos(int tokenIndex, String featureName, Sentence sent) {

    // 3 -> 4th column in CONLL format -> coarse-grained POS
    int featureIndex = 3;
    return createFeature(tokenIndex, featureName, sent, featureIndex);
  }


  // same as createFeatureForPos() -> used as static feature
  public static Feature createFeatureForCPos(int tokenIndex, String featureName, Sentence sent) {

    // 3 -> 4th column in CONLL format -> coarse-grained POS
    int featureIndex = 3;
    return createFeature(tokenIndex, featureName, sent, featureIndex);
  }


  // return WF for token index in sent
  public static Feature createFeatureForWF(int tokenIndex, String featureName, Sentence sent) {

    // 1 -> 2nd column in CONLL format -> word-form
    int featureIndex = 1;
    return createFeature(tokenIndex, featureName, sent, featureIndex);
  }


  private static Feature createFeature(
      int tokenIndex, String featureName, Sentence sent, int featureIndex) {

    String value = "";
    if (tokenIndex <= 0) {
      value = "null";
    } else if (tokenIndex > sent.getSentArray().length) {
      value = "null";
    } else {
      value = sent.getSentArray()[tokenIndex - 1][featureIndex];
    }
    Feature f = new Feature(featureName, value);
    return f;
  }


  // returns the dependency relation of head of token index,
  // which is parent dependency relation PDEPREL at cell s[9]
  public static Feature createFeatureForDepRel(
      int tokenIndex, String featureName, Sentence sent, boolean train) {

    //String depRel = curDepStruct.getLabels()[index];
    String depRel;
    if (tokenIndex < 0) {
      depRel = "null";
    } else if (tokenIndex == 0) {
      depRel = "none";
    } else {
      // 9 -> 10th column used for "storing" oracle/predicted label
      // -> at least, seems so; because also these columns are overwritten with "_" when training is read in Data()
      if (train) {
        //System.out.println(sent.toString());
        if (sent.getSentArray()[tokenIndex - 1][9] == null || sent.getSentArray()[tokenIndex - 1][9].equals("_")) {
          depRel = "none";
        } else {
          depRel = sent.getSentArray()[tokenIndex - 1][9];
        }
      } else {
        if (sent.getSentArray()[tokenIndex - 1][9] == null || sent.getSentArray()[tokenIndex - 1][9].equals("_")) {
          depRel = "none";
        } else {
          depRel = sent.getSentArray()[tokenIndex - 1][9];
        }
      }
    }

    Feature feature = new Feature(featureName, depRel);
    // System.out.println(f.toString());
    // computes something like: deprdi=null(null), depldj=SB(null), depi=ROOT(null)
    return feature;
  }


  public static Feature createFeatureForDistance(int j, int i) {

    int dist = j - i;
    int distClass;
    if (j < 0 || i < 0) {
      distClass = 0;
    } else if (dist == 1) {
      distClass = 1;
    } else if (dist == 2) {
      distClass = 2;
    } else if (dist == 3) {
      distClass = 3;
    } else if (dist < 6) {
      distClass = 4;
    } else if (dist < 10) {
      distClass = 5;
    } else {
      distClass = 6;
    }
    Feature feature = new Feature("dist", String.valueOf(distClass));
    return feature;
  }


  public static Feature mergeFeatures(int i, String[] mergeFeatureNames, Feature f1, Feature f2) {

    String name = mergeFeatureNames[i];
    String value = f1.getValue() + "_" + f2.getValue();
    Feature doubleFeature = new Feature(name, value);
    return doubleFeature;
  }


  public static Feature mergeFeatures(int i, String[] mergeFeatureNames, Feature f1, Feature f2, Feature f3) {

    String name = mergeFeatureNames[i];
    String value = f1.getValue() + "_" + f2.getValue() + "_" + f3.getValue();
    Feature tripleFeature = new Feature(name, value);
    return tripleFeature;
  }
}
