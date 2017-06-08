package de.dfki.lt.mdparser.features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.bwaldvogel.liblinear.FeatureNode;

public class FeatureVector {

  private List<Feature> featureList;
  private String label;


  public FeatureVector() {

    this.featureList = new ArrayList<Feature>(40);
  }


  public Feature getFeature(String featureName) {

    for (Feature oneFeature : this.featureList) {
      if (oneFeature.getName().equals(featureName)) {
        return oneFeature;
      }
    }
    return null;
  }


  public List<Feature> getFeatureList() {

    return this.featureList;
  }


  public void setFeatureList(List<Feature> featureList) {

    this.featureList = featureList;
  }


  public String getLabel() {

    return this.label;
  }


  public void setLabel(String label) {

    this.label = label;
  }


  public void addFeature(Feature feature, Alphabet alpha, boolean train) {

    this.featureList.add(feature);
    if (train && !alpha.getValueToIndexMap().keySet().contains((feature.getFeatureString()))) {
      alpha.addFeature(feature.getFeatureString());
    }
  }


  public void updateFeatureValue(String featureName, String featureValue) {

    for (int i = this.featureList.size() - 1; i >= 0; i--) {
      Feature oneFeature = this.featureList.get(i);
      if (oneFeature.getName().equals(featureName)) {
        System.out.println(oneFeature.getValue() + " vs " + featureValue);
        oneFeature.setValue(featureValue);
        i = -1;
      }
    }
  }


  public String getIntegerRepresentation(Alphabet alpha, boolean labels) {

    StringBuilder builder = new StringBuilder();
    List<Integer> indexList = new ArrayList<Integer>();
    builder.append(alpha.getLabelIndexMap().get(this.label));
    for (Feature oneFeature : this.featureList) {
      Integer featureIndex = null;
      if (labels) {
        featureIndex = oneFeature.getLabelerIndex();
      } else {
        featureIndex = oneFeature.getParserIndex();
      }
      if (featureIndex == null) {
        featureIndex = alpha.getFeatureIndex(oneFeature.getFeatureString());
        //System.out.println(featureIndex);
        //System.out.println("old: " + alpha.getFeatureIndex(oneFeature.getFeatureString()) + " new: " + featureIndex);
      }
      if (featureIndex != null) {
        indexList.add(featureIndex);
      }
    }
    Collections.sort(indexList);
    for (Integer oneFeatureIndex : indexList) {
      builder.append(" ");
      builder.append(oneFeatureIndex);
      builder.append(":");
      builder.append(1);
    }

    return builder.toString();
  }


  public FeatureNode[] getLiblinearRepresentation(boolean train, boolean labels, Alphabet alpha) {

    List<Integer> indexList = new ArrayList<Integer>(40);
    for (Feature oneFeature : this.featureList) {
      Integer featureIndex = null;
      if (labels) {
        featureIndex = oneFeature.getLabelerIndex();
      } else {
        featureIndex = oneFeature.getParserIndex();
      }
      if (featureIndex == null) {
        featureIndex = alpha.getFeatureIndex(oneFeature.getFeatureString());
      }
      if (featureIndex != null) {
        indexList.add(featureIndex);
      }
    }
    if (train) {
      Collections.sort(indexList);
    }
    FeatureNode[] featureNodes = new FeatureNode[indexList.size()];
    for (int i = 0; i < indexList.size(); i++) {
      Integer featureIndex = indexList.get(i);
      FeatureNode node = new FeatureNode(featureIndex, 1);
      featureNodes[i] = node;
    }
    return featureNodes;
  }


  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    sb.append(this.label);
    for (int i = 0; i < this.featureList.size(); i++) {
      sb.append(" ");
      sb.append(this.featureList.get(i).getFeatureString());
    }
    return sb.toString();
  }

}
