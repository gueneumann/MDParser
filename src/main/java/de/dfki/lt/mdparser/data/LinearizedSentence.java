package de.dfki.lt.mdparser.data;

import java.util.ArrayList;
import java.util.List;

import de.dfki.lt.mdparser.algorithm.Dependency;
import de.dfki.lt.mdparser.algorithm.DependencyStructure;

public class LinearizedSentence {

  private Sentence sentence = null;
  private List<String> linearizedSentence = new ArrayList<String>();
  private DependencyStructure depStruct = null;


  public LinearizedSentence(DependencyStructure depStruct) {

    this.depStruct = depStruct;
  }


  public LinearizedSentence(Sentence sentence) {

    this.sentence = sentence;
    this.depStruct = this.fillDependencyStructure(this.sentence);
  }


  /**
   * Transform the CONLL 2D-array format returned by the parser to a DependencyStructure which is a
   * set of Dependencies (plus index-structure for efficient access of the Dependency elements.
   *
   * A Dependency is a directed labeled dependency link with string representation (modifierID edge
   * headID) plus additional information for labels and props.
   *
   * CONLL to Dependency representation: For each token array of form "ID FORM LEMMA CPOSTAG POSTAG
   * FEATS HEAD DEPREL PHEAD PDEPREL PRED ARG1 ARG2" which correspond to array counts from 0 to 12
   * create a dependency link (modID:form:POS label headID:hForm:hPOS) and add it to the dependency
   * structure.
   *
   * The DependencyStructure comes with a set of useful operations for testing the shape of the
   * underlying dependency graph
   *
   * @param sent
   * @return
   */
  private DependencyStructure fillDependencyStructure(Sentence sent) {
  
    List<Dependency> parsedDependencies = new ArrayList<Dependency>();
  
    String[][] sentArray = sent.getSentArray();
    // For each token of sentence
    for (int i = 0; i < sentArray.length; i++) {
      // Get conll information for token
      String[] lineArray = sentArray[i];
      Dependency dep = new Dependency(
          Integer.valueOf(lineArray[0]), // modID
          Integer.valueOf(lineArray[6]), // label
          lineArray[7]); // headID
  
      dep.setDependentString(lineArray[1] + ":" + lineArray[3]); // modID label :== form:pos
      if (Integer.valueOf(lineArray[6]) != 0) {
        // Retrieve head token from sentence array and create
        // headID label
        int head = Integer.valueOf(lineArray[6]) - 1;
        dep.setHeadString(sentArray[head][1] + ":" + sentArray[head][3]); // headID label :== headIDform:headIDpos
      } else {
        // or a dummy string for the root element
        dep.setHeadString("null:null");
      }
      parsedDependencies.add(dep);
    }
    DependencyStructure depStr = new DependencyStructure(parsedDependencies.size());
    for (int i = 0; i < parsedDependencies.size(); i++) {
      depStr.addDependency(parsedDependencies.get(i));
    }
    depStr.constructDependenciesArray();
    return depStr;
  }


  public List<String> getLinearizedSentence() {

    return this.linearizedSentence;
  }


  public DependencyStructure getDepStruct() {

    return this.depStruct;
  }


  /**
   * Linearize a dependency tree/dag into a list of tokens. Do left-to-right depth first traversal
   * from root. Start from root node -> assumed to be unique
   *
   * @param ds
   * @return
   */
  
  public void linearizedDependencyStructure() {
  
    Dependency root = this.getDepStruct().getDependenciesArray()[this.getDepStruct().getRootPosition()];
    descendFromNode(root, "(_RT", ")_RT");
  }


  /**
   * Traverse a dependency tree top-down, depth first left to right. For each dependency relation
   * (mod label head) create a sequence <"(_label" mod sublist ")_label"> where sublist is empty or
   * the linearized modifier dependency nodes of the modifier.
   */
  
  
  private void descendFromNode(Dependency dependency, String openNode, String closeNode) {
  
    this.getLinearizedSentence().add(openNode);
    String word = this.makeNodeString(dependency);
    this.getLinearizedSentence().add(word);
    // Modifiers are processed from left to right
    List<Dependency> modifiers = getDepRelsWithHeadId(dependency.getDependent());
    for (int i = 0; i < modifiers.size(); i++) {
      descendFromNode(modifiers.get(i),
          "(_" + modifiers.get(i).getLabel(),
          ")_" + modifiers.get(i).getLabel());
    }
    this.getLinearizedSentence().add(closeNode);
  }


  // NOTE:
  // I am currently defining two very similar versions
  // - one which is very close to a standard dependency tree representation
  // - one which I am using in my GAMR system - these are marked by suffix *AMR
  
  
  // Method for creating the node label in the string representation
  
  private String makeNodeString(Dependency dependency) {
  
    return dependency.getDependent()
        + ":" + dependency.getDependentWord()
        + ":" + dependency.getDependentPos();
  }


  public void linearizedDependencyStructureAMR() {
  
    Dependency root = this.getDepStruct().getDependenciesArray()[this.getDepStruct().getRootPosition()];
    descendFromNodeAMR(root, "", "(", ")");
  }


  // Mainly the same but created a AMR-syntactic tree representation
  private void descendFromNodeAMR(Dependency dependency, String label, String openNode, String closeNode) {
  
    if (!label.isEmpty()) {
      this.getLinearizedSentence().add(":" + label);
    }
    this.getLinearizedSentence().add(openNode);
    String word = this.makeNodeStringAMR(dependency);
    this.getLinearizedSentence().add(word);
    // Modifiers are processed from left to right
    List<Dependency> modifiers = getDepRelsWithHeadId(dependency.getDependent());
    for (int i = 0; i < modifiers.size(); i++) {
      descendFromNodeAMR(modifiers.get(i),
          modifiers.get(i).getLabel(),
          "(",
          ")");
    }
    this.getLinearizedSentence().add(closeNode);
  }


  // NOTE:
  // I am currently defining two very similar versions
  // - one which is very close to a standard dependency tree representation
  // - one which I am using in my GAMR system - these are marked by suffix *AMR
  
  
  // Method for creating the node label in the string representation
  
  private String makeNodeStringAMR(Dependency dependency) {
  
    return dependency.getDependentWord();
  }


  /*
   * get-Modifiers(headId) -> list of depRels of all direct modifiers (level one)
   */
  private List<Dependency> getDepRelsWithHeadId(int headId) {

    List<Dependency> modifiers = new ArrayList<Dependency>();
    for (int i = 1; i < getDepStruct().getDependenciesArray().length; i++) {
      if (getDepStruct().getDependenciesArray()[i].getHead() == headId) {
        modifiers.add(getDepStruct().getDependenciesArray()[i]);
      }
    }
    return modifiers;
  }

  // NOTE:
  // I am currently defining two very similar versions
  // - one which is very close to a standard dependency tree representation
  // - one which I am using in my GAMR system - these are marked by suffix *AMR


  // Method for creating the node label in the string representation

  public String toLinearizedDependencyString() {

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < this.getLinearizedSentence().size() - 1; i++) {
      result.append(this.getLinearizedSentence().get(i) + " ");
    }
    result.append(this.getLinearizedSentence().get(this.getLinearizedSentence().size() - 1));
    return result.toString();
  }
}
