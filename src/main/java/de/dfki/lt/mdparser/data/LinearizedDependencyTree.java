package de.dfki.lt.mdparser.data;

import java.util.ArrayList;
import java.util.List;

import de.dfki.lt.mdparser.algorithm.Dependency;
import de.dfki.lt.mdparser.algorithm.DependencyStructure;

public class LinearizedDependencyTree {

  private List<String> linearizedSentence = new ArrayList<String>();
  private DependencyStructure depStruct = null;
  private boolean withPosFeature = true;
  private boolean withLabelAsPosfeature = true;


  public List<String> getLinearizedSentence() {

    return this.linearizedSentence;
  }


  public DependencyStructure getDepStruct() {

    return this.depStruct;
  }

  /**
   * When this class is instantiated, it takes as input a dependency structure computed by MDParser
   * and too flags, and returns a linearized dependency tree.
   * The linearized dependency tree will be used as input to our Deep AMR parser which is implemented using OpenNMT.
   * If the flag withPosFeature is false, then only the node and edge labels are used, i.e., the words and grammatical labels
   * of a dependency tree.
   * If the withPosFeature is true, it will add to each node (word) the POS tag, separated by the special char "￨"
   * (which is NOT the pipe char).
   * If the flag withLabelAsPosfeature is true, it will add to each edge label (grammatical label)
   * as POS tag otherwise the dummy label "LABEL" is used. We do this for two purposes: 1.) to be syntactical consistent with
   * word POS labels, and  2) in order to test whether the content of the label is useful.
   * @param depStruct
   * @param withPosFeature
   * @param withLabelAsPosfeature
   */

  public LinearizedDependencyTree(DependencyStructure depStruct, boolean withPosFeature, boolean withLabelAsPosfeature) {

    this.depStruct = depStruct;
    this.withPosFeature = withPosFeature;
    this.withLabelAsPosfeature = withLabelAsPosfeature;
    this.linearizedDependencyStructure();
  }


  // Node string representation for linearized form of dependency tree part

  private String makeLinearizedNodeString(Dependency dependency) {

    if (this.withPosFeature) {
    return dependency.getDependentWord()
        + "￨"
        + dependency.getDependentPos()
    ;
    } else {
      return dependency.getDependentWord();
    }
  }


  private String makeLinearizedEdgeString(String paraString, String labelString) {

    String edgeString = paraString + labelString;
    if (this.withPosFeature) {
      if (this.withLabelAsPosfeature) {
        edgeString = edgeString + "￨" + labelString;
      } else {
        edgeString = edgeString + "￨" + "LABEL";
      }
    }
    return edgeString;
  }


  private List<Dependency> getDepRelsWithHeadId(int headId) {

    List<Dependency> modifiers = new ArrayList<Dependency>();
    for (int i = 1; i < getDepStruct().getDependenciesArray().length; i++) {
      if (getDepStruct().getDependenciesArray()[i].getHead() == headId) {
        modifiers.add(getDepStruct().getDependenciesArray()[i]);
      }
    }
    return modifiers;
  }


  private void descendFromNode(Dependency dependency, String openNode, String closeNode) {

    this.getLinearizedSentence().add(openNode);
    String word = this.makeLinearizedNodeString(dependency);

    this.getLinearizedSentence().add(word);
    // Modifiers are processed from left to right
    List<Dependency> modifiers = getDepRelsWithHeadId(dependency.getDependent());
    for (int i = 0; i < modifiers.size(); i++) {
      descendFromNode(modifiers.get(i),
          makeLinearizedEdgeString("(_", modifiers.get(i).getLabel()),
          makeLinearizedEdgeString(")_", modifiers.get(i).getLabel())
          );
    }
    this.getLinearizedSentence().add(closeNode);
  }


  public void linearizedDependencyStructure() {

    Dependency root = this.getDepStruct().getDependenciesArray()[this.getDepStruct().getRootPosition()];
    String rootLabel = root.getLabel();
    descendFromNode(root,
        makeLinearizedEdgeString("(_", rootLabel),
        makeLinearizedEdgeString(")_", rootLabel)
        );
  }

  // Methods for creating the node label in the string representation


  public String toLinearizedDependencyString() {

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < this.getLinearizedSentence().size() - 1; i++) {
      result.append(this.getLinearizedSentence().get(i) + " ");
    }
    result.append(this.getLinearizedSentence().get(this.getLinearizedSentence().size() - 1));
    return result.toString();
  }
}
