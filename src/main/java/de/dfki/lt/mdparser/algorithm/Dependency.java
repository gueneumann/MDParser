package de.dfki.lt.mdparser.algorithm;

import java.util.Stack;

public class Dependency {

  private int dependent;
  private int head;

  private int startIndex;

  private String label;

  private double probability;
  private String dependentString;
  private String headString;


  public Dependency(int dependent, int head) {
    this.dependent = dependent;
    this.head = head;
  }


  public Dependency(int dependent, int head, String label) {
    this.dependent = dependent;
    this.head = head;
    this.label = label;
  }


  public int getStartIndex() {

    return this.startIndex;
  }


  public void setStartIndex(int si) {

    this.startIndex = si;
  }


  public int getDependent() {

    return this.dependent;
  }


  public int getHead() {

    return this.head;
  }


  public String getLabel() {

    return this.label;
  }


  public void setLabel(String label) {

    this.label = label;
  }


  public int getDistance() {

    if (this.dependent > this.head) {
      return this.dependent - this.head;
    } else {
      return this.head - this.dependent;
    }
  }


  public void setProbability(double prob) {

    this.probability = prob;
  }


  public double getProbability() {

    return this.probability;
  }


  public void setDependentString(String dependentString) {

    this.dependentString = dependentString;
  }


  public String getDependentString() {

    return this.dependentString;
  }


  public String getDependentWord() {

    int splitPoint = this.dependentString.lastIndexOf(":");
    return this.dependentString.substring(0, splitPoint);
  }


  public String getDependentPos() {

    int splitPoint = this.dependentString.lastIndexOf(":");
    return this.dependentString.substring(splitPoint + 1, this.dependentString.length());
  }


  public String getHeadWord() {

    int splitPoint = this.headString.lastIndexOf(":");
    return this.headString.substring(0, splitPoint);
  }


  public String getHeadPos() {

    int splitPoint = this.headString.lastIndexOf(":");
    return this.headString.substring(splitPoint + 1, this.headString.length());
  }


  public void setHeadString(String headString) {

    this.headString = headString;
  }


  public String getHeadString() {

    return this.headString;
  }


  public boolean isProjective(int[] heads) {

    if (this.getDistance() == 1) {
      return true;
    } else {
      int smaller = this.head;
      int bigger = this.dependent;
      if (this.head > this.dependent) {
        smaller = this.dependent;
        bigger = this.head;
      }
      for (int i = bigger - 1; i > 0; i--) {
        if (i != smaller) {
          if (i > smaller && heads[i] < smaller) {
            return false;
          } else if (i < smaller && heads[i] > smaller) {
            return false;
          }
        }
      }

    }
    return true;
  }


  public boolean isSingleHead(int[] heads) {

    if (heads[this.dependent] != -1) {
      return false;
    }
    return true;
  }


  public boolean isNotReflexive() {

    return this.dependent != this.head;
  }


  public boolean notIntroducingCycle(int[] heads) {

    int curHead = this.getHead();
    boolean[] possibleEnds = new boolean[heads.length + 1];
    possibleEnds[this.dependent] = true;
    Stack<Integer> toCheck = new Stack<Integer>();
    toCheck.add(curHead);
    while (!toCheck.isEmpty()) {
      curHead = toCheck.pop();
      int curHeadHead = heads[curHead];
      if (curHeadHead < 1) {
        return true;
      }
      if (possibleEnds[curHeadHead]) {
        return false;
      } else {
        toCheck.add(curHeadHead);
        possibleEnds[curHead] = true;

      }
    }
    return true;
  }


  public boolean isNotImproperRoot() {

    if (this.dependent == 0) {
      return false;
    }
    return true;
  }


  public boolean isPermissible(DependencyStructure depStruct, boolean projective) {

    //System.out.println("single head: " + this.isSingleHead(depStruct) + " not reflexive: " + this.isNotReflexive()
    //    + " not introducing cycle: " + notIntroducingCycle(depStruct) + " is not improper root "
    //    + this.isNotImproperRoot() + " is projective " + this.isProjective(depStruct));
    int[] heads = depStruct.getHeads();
    if (projective) {
      return this.isSingleHead(heads)
          && this.isNotReflexive()
          && this.notIntroducingCycle(heads)
          && this.isNotImproperRoot()
          && this.isProjective(heads);
    }
    return this.isSingleHead(heads) && this.isNotReflexive() && this.notIntroducingCycle(heads)
        && this.isNotImproperRoot();
  }


  @Override
  public String toString() {

    return "(" + this.getDependent() + "," + this.label + "," + this.getHead() + ")";
  }
}
