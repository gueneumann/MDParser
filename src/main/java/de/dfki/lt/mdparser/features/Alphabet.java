package de.dfki.lt.mdparser.features;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Bidirectional mapping of features to their indices and of labels to their indices.
 */
public class Alphabet {

  private Map<String, Integer> label2IndexMap;
  private String[] index2LabelArray;
  // flag to indicate if index2LabelArray is in sync with label2IndexMap
  private boolean labelArrayDirty;

  private Map<String, Integer> feature2IndexMap;
  private String[] index2FeatureArray;
  // flag to indicate if index2FeatureArray is in sync with feature2IndexMap
  private boolean featureArrayDirty;


  public Alphabet() {

    this.label2IndexMap = new HashMap<>(100);
    this.feature2IndexMap = new HashMap<>(1000000);
    this.featureArrayDirty = true;
    this.labelArrayDirty = true;
  }


  public Alphabet(Path alphabetPath)
      throws IOException {

    this(Files.newInputStream(alphabetPath));
  }


  public Alphabet(InputStream inputStream)
      throws IOException {

    this();

    try (BufferedReader in = new BufferedReader(
        new InputStreamReader(new BufferedInputStream(inputStream), StandardCharsets.UTF_8))) {
      int c = 0;
      String line;
      while ((line = in.readLine()) != null) {
        if (line.trim().length() == 0) {
          c++;
        } else {
          String[] lineArray;
          lineArray = line.split(" ");
          if (c == 0) {
            this.label2IndexMap.put(lineArray[1], Integer.valueOf(lineArray[0]));
          } else if (c == 1) {
            this.feature2IndexMap.put(lineArray[1], Integer.valueOf(lineArray[0]));
          }
        }
      }
    }
  }


  private synchronized String[] getIndex2LabelArray() {

    if (this.labelArrayDirty) {
      // update array
      this.index2LabelArray = new String[this.label2IndexMap.size() + 1];
      for (Map.Entry<String, Integer> oneLabelIndexPair : this.label2IndexMap.entrySet()) {
        String curLabel = oneLabelIndexPair.getKey();
        Integer curIndex = oneLabelIndexPair.getValue();
        this.index2LabelArray[curIndex] = curLabel;
      }
      this.labelArrayDirty = false;
    }

    return this.index2LabelArray;
  }


  private synchronized String[] getIndex2FeatureArray() {

    if (this.featureArrayDirty) {
      // update array
      this.index2FeatureArray = new String[this.feature2IndexMap.size() + 1];
      for (Map.Entry<String, Integer> oneFeatureIndexPair : this.feature2IndexMap.entrySet()) {
        String curFeature = oneFeatureIndexPair.getKey();
        Integer curIndex = oneFeatureIndexPair.getValue();
        this.index2FeatureArray[curIndex] = curFeature;
      }
      this.featureArrayDirty = false;
    }

    return this.index2FeatureArray;
  }


  public int getNumberOfLabels() {

    return this.label2IndexMap.size();
  }


  public int getNumberOfFeatures() {

    return this.feature2IndexMap.size();
  }


  public Integer getLabelIndex(String label) {

    return this.label2IndexMap.get(label);
  }


  public String getLabel(int index) {

    return this.getIndex2LabelArray()[index];
  }


  public Integer getFeatureIndex(String feature) {

    return this.feature2IndexMap.get(feature);
  }


  public String getFeature(int index) {

    return this.getIndex2FeatureArray()[index];
  }


  public synchronized void addLabel(String label) {

    Integer index = this.label2IndexMap.get(label);
    if (index == null) {
      this.label2IndexMap.put(label, this.label2IndexMap.size() + 1);
      this.labelArrayDirty = true;
    }
  }


  public synchronized void addFeature(String feature) {

    Integer index = this.feature2IndexMap.get(feature);
    if (index == null) {
      this.feature2IndexMap.put(feature, this.feature2IndexMap.size() + 1);
      this.featureArrayDirty = true;
    }
  }


  public void removeUnusedFeatures(Set<Integer> unusedFeatures) {

    int indexCount = 1;
    Map<String, Integer> updatedFeature2IndexMap =
        new HashMap<>(this.feature2IndexMap.size() - unusedFeatures.size());
    String[] index2Feature = this.getIndex2FeatureArray();
    for (int i = 1; i < index2Feature.length; i++) {
      if (!unusedFeatures.contains(i)) {
        updatedFeature2IndexMap.put(index2Feature[i], indexCount);
        indexCount++;
      }
    }
    this.feature2IndexMap = updatedFeature2IndexMap;
    this.featureArrayDirty = true;
  }


  public void writeToFile(Path alphabetPath)
      throws IOException {

    if (alphabetPath.getParent() != null) {
      Files.createDirectories(alphabetPath.getParent());
    }
    try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(
        alphabetPath, StandardCharsets.UTF_8))) {
      String[] index2Label = this.getIndex2LabelArray();
      for (int i = 1; i < index2Label.length; i++) {
        out.println(String.format("%d %s", i, index2Label[i]));
      }
      out.println();
      String[] index2Feature = this.getIndex2FeatureArray();
      for (int i = 1; i < index2Feature.length; i++) {
        String feature = index2Feature[i];
        if (feature != null) {
          out.println(i + " " + feature);
        }
      }
    }
  }


  public void writeToFile(Path alphabetPath, int[][] compactArray)
      throws IOException {

    Files.createDirectories(alphabetPath.getParent());
    try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(
        alphabetPath, StandardCharsets.UTF_8))) {
      String[] index2label = this.getIndex2LabelArray();
      for (int i = 1; i < index2label.length; i++) {
        out.println(String.format("%d %s", i, index2label[i]));
      }
      out.println();
      int[] newToOld = compactArray[0];
      boolean notFinished = true;
      String[] index2Feature = this.getIndex2FeatureArray();
      for (int i = 1; notFinished && i < newToOld.length; i++) {
        int newIndex = i;
        int oldIndex = newToOld[i];
        if (oldIndex == 0) {
          notFinished = false;
        } else {
          String feature = index2Feature[oldIndex];
          out.println(newIndex + " " + feature);
        }
      }
    }
  }
}
