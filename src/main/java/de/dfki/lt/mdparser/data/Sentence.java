package de.dfki.lt.mdparser.data;

import de.dfki.lt.mdparser.features.Feature;

public class Sentence {

  private String[][] sentArray;
  private Integer rootPosition;

  private Feature[][][] staticFeatures;


  // setters and getters
  public void setSentArray(String[][] sentArray) {

    this.sentArray = sentArray;
  }


  public String[][] getSentArray() {

    return this.sentArray;
  }


  public void setRootPosition(int rootPosition) {

    this.rootPosition = rootPosition;
  }


  public Integer getRootPosition() {

    return this.rootPosition;
  }


  public void setStaticFeatures(Feature[][][] staticFeatures) {

    this.staticFeatures = staticFeatures;
  }


  public Feature[][][] getStaticFeatures() {

    return this.staticFeatures;
  }


  // Init class
  public Sentence(Integer sentSize, int infoSize) {
    this.sentArray = new String[sentSize][infoSize];
    this.rootPosition = -1;
  }


  public Sentence(String[][] sentArray) {
    this.sentArray = sentArray;
    this.rootPosition = -1;
  }


  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < this.sentArray.length; i++) {
      for (int j = 0; j < this.sentArray[0].length - 1; j++) {
        if (this.sentArray[i][j] == null) {
          this.sentArray[i][j] = "_";
        }
        sb.append(this.sentArray[i][j]);
        sb.append("\t");
      }
      sb.append(this.sentArray[i][this.sentArray[0].length - 1]);
      sb.append("\n");
    }
    return sb.toString();
  }

}
