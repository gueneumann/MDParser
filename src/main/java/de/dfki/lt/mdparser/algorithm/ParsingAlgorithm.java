package de.dfki.lt.mdparser.algorithm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.FeatureVector;
import de.dfki.lt.mdparser.parser.Parser;

public abstract class ParsingAlgorithm {

  private Parser parser;
  private int numberOfConfigurations;
  private HashMap<String, Integer> labelFreqMap;


  public void setParser(Parser parser) {

    this.parser = parser;
  }


  public Parser getParser() {

    return this.parser;
  }


  //TRAIN-GDS-COMBINED
  public abstract List<FeatureVector> processCombined(Sentence sentence, FeatureModel fm, boolean noLabels);


  //TEST-GDS-COMBINED
  public abstract void processCombined(Sentence sent, FeatureModel fm, boolean noLabels,
      HashMap<String, String> splitMap);


  public abstract String findOutCorrectLabel(int j, int i, String[][] sentArray);


  public int getNumberOfConfigurations() {

    return this.numberOfConfigurations;
  }


  public void plus() {

    this.numberOfConfigurations++;
  }


  public void setNumberOfConfigurations(int numberOfConfigurations) {

    this.numberOfConfigurations = numberOfConfigurations;
  }


  public void initLabelFreqMap() {

    this.labelFreqMap = new HashMap<String, Integer>();
  }


  public void setLabelFreqMap(HashMap<String, Integer> labelFreqMap) {

    this.labelFreqMap = labelFreqMap;
  }


  public HashMap<String, Integer> getLabelFreqMap() {

    return this.labelFreqMap;
  }


  public synchronized String findTheMostFreqLabel(String pos) {

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


  public synchronized void increaseCount(String key) {

    Integer curKeyFreq = this.labelFreqMap.get(key);
    if (curKeyFreq == null) {
      this.labelFreqMap.put(key, 1);
    } else {
      this.labelFreqMap.put(key, 1 + curKeyFreq);
    }
  }

}
