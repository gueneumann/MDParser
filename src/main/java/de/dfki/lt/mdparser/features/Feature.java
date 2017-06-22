package de.dfki.lt.mdparser.features;

public class Feature {

  private String name;
  private String value;
  private String featureString;
  private Integer parserIndex;
  private Integer labelerIndex;
  private int nameIndex;


  public Feature(String name, String value) {

    this.name = name;
    this.value = value;
    this.featureString = name + "=" + value;
  }


  public Feature(int nameIndex, int parserIndex) {

    this.parserIndex = parserIndex;
    this.nameIndex = nameIndex;
  }


  //TODO
  // Added because of TrainierMem
  @Override
  public Feature clone() {

    Feature featureClone = new Feature(this.name, this.value);
    featureClone.featureString = this.featureString;
    featureClone.parserIndex = this.parserIndex;
    featureClone.labelerIndex = this.labelerIndex;
    featureClone.nameIndex = this.nameIndex;
    return featureClone;
  }


  public String getName() {

    return this.name;
  }


  public String getValue() {

    return this.value;
  }


  public void setValue(String value) {

    this.value = value;
    this.featureString = this.name + "=" + this.value;
  }


  public String getFeatureString() {

    return this.featureString;
  }


  public Integer getParserIndex() {

    return this.parserIndex;
  }


  public void setParserIndex(Integer fIndex) {

    this.parserIndex = fIndex;
  }


  public Integer getLabelerIndex() {

    return this.labelerIndex;
  }


  @Override
  public String toString() {

    return String.valueOf(this.featureString + "(" + this.parserIndex + ")");
  }
}
