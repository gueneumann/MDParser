package de.dfki.lt.mdparser.algorithm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.bwaldvogel.liblinear.Model;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.FeatureVector;

public abstract class ParsingAlgorithm {

  private int numberOfConfigurations;
  private Map<String, Integer> labelFreqMap;


  public ParsingAlgorithm() {

    this.numberOfConfigurations = 0;
    this.labelFreqMap = new HashMap<>();
  }


  public int getNumberOfConfigurations() {

    return this.numberOfConfigurations;
  }


  public synchronized void incNumberOfConfigurations() {

    this.numberOfConfigurations++;
  }


  //TRAIN-GDS-COMBINED
  public abstract List<FeatureVector> processCombined(
      Sentence sent, FeatureModel featureModel, boolean noLabels);


  //TEST-GDS-COMBINED
  public abstract void processCombined(
      Sentence sent, FeatureModel featureModel, boolean noLabels, Map<String, Model> feature2ModelMap);


  public synchronized String findMostFrequentLabel(String pos) {

    Iterator<String> iter = this.labelFreqMap.keySet().iterator();
    Integer curMax = Integer.MIN_VALUE;
    String curMaxLabel = "";
    while (iter.hasNext()) {
      String key = iter.next();
      String curPos = key.split("###")[0];
      if (curPos.equals(pos)) {
        String curLabel = key.split("###")[1];
        Integer val = this.labelFreqMap.get(key);
        if (val > curMax) {
          curMaxLabel = curLabel;
          curMax = val;
        }
      }
    }
    return curMaxLabel;
  }


  public synchronized void increaseLabelCount(String label) {

    Integer curKeyFreq = this.labelFreqMap.get(label);
    if (curKeyFreq == null) {
      this.labelFreqMap.put(label, 1);
    } else {
      this.labelFreqMap.put(label, 1 + curKeyFreq);
    }
  }
}
