package de.dfki.lt.mdparser.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dfki.lt.mdparser.algorithm.Dependency;
import de.dfki.lt.mdparser.algorithm.DependencyStructure;

public class LinearizedSentence {

  private Sentence sentence = null;
  private List<String> flatSentence = new ArrayList<String>();
  private List<String> linearizedSentence = new ArrayList<String>();
  private DependencyStructure depStruct = null;
  private String typeTokenIdstring = "#";


  // getters and setters
  public List<String> getFlatSentence() {

    return this.flatSentence;
  }


  public List<String> getLinearizedSentence() {

    return this.linearizedSentence;
  }


  public DependencyStructure getDepStruct() {

    return this.depStruct;
  }


  //Instantiation


  public LinearizedSentence(DependencyStructure depStruct, boolean typeTokenId) {

    this.depStruct = depStruct;
    if (typeTokenId) {
      this.createTokenTypeIndex();
    }
    this.createFlatWordPOSsequence();
    this.linearizedDependencyStructure();
  }


  public LinearizedSentence(Sentence sentence) {

    this.sentence = sentence;
    this.depStruct = this.fillDependencyStructure(this.sentence);
  }


  // Methods

  // Internal node representation for the parts of an aligned (sentence,dependency tree)

  //Node string representation for sentence part - dependency word only

  private String makeSentenceTokenNode(Dependency dependency) {

    return dependency.getDependentWord();
  }

  //Node string representation for sentence element - dependency word and POS as word|POS
  private String makeSentenceTokenPosNode(Dependency dependency) {
    return dependency.getDependentWord()
     // please note that this is NOT the standard pipe character
        + "￨"
        + dependency.getDependentPos();
  }


  // Node string representation for linearized form of dependency tree part

  // Output modifierWord only, label is part of the paranthesis

  private String makeLinearizedParseNodeString(Dependency dependency) {

    return dependency.getDependentWord()
    //        + ":"
    //        + dependency.getHeadWord()

    ;
  }

  /** Method for providing token-type id
   * if word occurs at different position, it will also be represented as different nodes
   * so "the man the woman" -> "the_1 man_1 the_2 woman_1"
   */

  private void setTypeIndex(String word, Map<String,Integer> indexTable) {
    if (indexTable.containsKey(word)) {
      indexTable.put(word, indexTable.get(word)+1);
    } else {
      indexTable.put(word, 1);
    }
  }
  public void createTokenTypeIndex() {
    Map<String,Integer> indexTable = new HashMap<String,Integer>();
    for (int i = 1; i < this.depStruct.getDependenciesArray().length; i++) {
      Dependency typeNode = this.depStruct.getDependenciesArray()[i];
      String word = typeNode.getDependentWord();
      this.setTypeIndex(word, indexTable);
      typeNode.setDependentString(
          word+this.typeTokenIdstring+indexTable.get(word),
          typeNode.getDependentPos());
    }
  }

  // Method for creating the node label in the string representation

  /**
   * Create sentence part of an aligned pair (sentence, dependency tree) Current representation is:
   * "w_i|pos_i" -> as it is usefull with OpenNMT
   */
  public void createFlatWordPOSsequence() {

    for (int i = 1; i < this.depStruct.getDependenciesArray().length; i++) {
      String tokenNode = this.makeSentenceTokenPosNode(this.depStruct.getDependenciesArray()[i]);
      this.flatSentence.add(tokenNode);
    }
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
          Integer.valueOf(lineArray[6]), // headID
          lineArray[7]); // label

      dep.setDependentString(lineArray[1], lineArray[3]); // modID label :== form, pos
      if (Integer.valueOf(lineArray[6]) != 0) {
        // Retrieve head token from sentence array and create
        // headID label
        int head = Integer.valueOf(lineArray[6]) - 1;
        dep.setHeadString(sentArray[head][1], sentArray[head][3]); // headID label :== headIDform, headIDpos
      } else {
        // or a dummy string for the root element
        dep.setHeadString("null", "null");
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


  /**
   * Traverse a dependency tree top-down, depth first left to right. For each dependency relation (mod
   * label head) create a sequence <"(_label" mod sublist ")_label"> where sublist is empty or the
   * linearized modifier dependency nodes of the modifier.
   */

  /**
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


  private void descendFromNode(Dependency dependency, String openNode, String closeNode) {

    this.getLinearizedSentence().add(openNode);
    String word = this.makeLinearizedParseNodeString(dependency);

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


  /**
   * Linearize a dependency tree/dag into a list of tokens. Do left-to-right depth first traversal
   * from root. Start from root node -> assumed to be unique
   *
   * @param ds
   * @return
   */

  public void linearizedDependencyStructure() {

    Dependency root = this.getDepStruct().getDependenciesArray()[this.getDepStruct().getRootPosition()];
    String rootLabel = root.getLabel();
    descendFromNode(root, "(_"+rootLabel, ")_"+rootLabel);
  }

  // Methods for creating the node label in the string representation

  public String toFlatSentenceString() {

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < this.getFlatSentence().size() - 1; i++) {
      result.append(this.getFlatSentence().get(i) + " ");
    }
    result.append(this.getFlatSentence().get(this.getFlatSentence().size() - 1));
    return result.toString();
  }


  public String toLinearizedDependencyString() {

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < this.getLinearizedSentence().size() - 1; i++) {
      result.append(this.getLinearizedSentence().get(i) + " ");
    }
    result.append(this.getLinearizedSentence().get(this.getLinearizedSentence().size() - 1));
    return result.toString();
  }
}
