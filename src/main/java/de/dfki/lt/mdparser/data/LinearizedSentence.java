package de.dfki.lt.mdparser.data;

import java.util.ArrayList;
import java.util.List;

import de.dfki.lt.mdparser.algorithm.Dependency;
import de.dfki.lt.mdparser.algorithm.DependencyStructure;

public class LinearizedSentence {

	private Sentence sentence = null;
	private List<String> linearizedSentence = new ArrayList<String>();
	private DependencyStructure ds = null;

	// Getters and setters
	public Sentence getSentence() {
		return sentence;
	}
	public void setSentence(Sentence sentence) {
		this.sentence = sentence;
	}
	public List<String> getLinearizedSentence() {
		return linearizedSentence;
	}
	public void setLinearizedSentence(List<String> linearizedSentence) {
		this.linearizedSentence = linearizedSentence;
	}

	public DependencyStructure getDs() {
		return ds;
	}
	public void setDs(DependencyStructure ds) {
		this.ds = ds;
	}

	// Init class


	public LinearizedSentence (Sentence sentence){
		this.setSentence(sentence);
		this.setDs(this.fillDependencyStructure(this.getSentence()));
	}


	/**
	 * Transform the CONLL 2D-array format returned by the parser
	 * to a DependencyStructure which is a set of Dependencies (plus index-structure for 
	 * efficient access of the Dependency elements.
	 * 
	 * A Dependency is a directed labeled dependency link with string representation (modifierID edge headID) 
	 * plus additional information for labels and props.
	 * 
	 * CONLL to Dependency representation: 
	 * For each token array of form 
	 * 	"ID FORM LEMMA CPOSTAG POSTAG FEATS HEAD DEPREL PHEAD PDEPREL PRED ARG1 ARG2"
	 * 	which correspond to array counts from 0 to 12
	 * create a dependency link 
	 * (modID:form:POS label headID:hForm:hPOS) 
	 * and add it to the dependency structure.
	 * 
	 * The DependencyStructure comes with a set of useful operations for 
	 * testing the shape of the underlying dependency graph
	 * @param sent
	 * @return
	 */
	private DependencyStructure fillDependencyStructure(Sentence sent) {
		List<Dependency> parsedDependencies = new ArrayList<Dependency>();

		String[][] sentArray = sent.getSentArray();
		// For each token of sentence
		for (int i=0; i < sentArray.length;i++) {
			// Get conll information for token
			String[] lineArray = sentArray[i];
			Dependency dep = new Dependency(
					Integer.valueOf(lineArray[0]), // modID
					Integer.valueOf(lineArray[6]), // label
					lineArray[7]);					// headID

			dep.setDependentString(lineArray[1]+":"+lineArray[3]); // modID label :== form:pos
			if (Integer.valueOf(lineArray[6]) != 0) {
				// Retrieve head token from sentence array and create
				// headID label
				int head = Integer.valueOf(lineArray[6])-1;
				dep.setHeadString(sentArray[head][1]+":"+sentArray[head][3]); // headID label :== headIDform:headIDpos
			}
			else {
				// or a dummy string for the root element
				dep.setHeadString("null:null");
			}
			parsedDependencies.add(dep);
		}
		DependencyStructure ds = new DependencyStructure(parsedDependencies.size());
		for (int i=0;i < parsedDependencies.size(); i++) {
			ds.addDependency(parsedDependencies.get(i));
		}
		ds.constructDependenciesArray();
		return ds;
	}

	/*
	 * get-Modifiers(headId) -> list of depRels of all direct modifiers (level one)
	 */
	private List<Dependency> getDepRelsWithHeadId(int headId) {
		List<Dependency> modifiers = new ArrayList<Dependency>();
		for (int i=1;i < getDs().getDependenciesArray().length; i++) {
			if (getDs().getDependenciesArray()[i].getHead() == headId){
				modifiers.add(getDs().getDependenciesArray()[i]);
			}
		}
		return modifiers;
	}

	/**
	 * Traverse a dependency tree top-down, depth first left to right. 
	 * if the current node is a VerbPhraseNode then construct an extraction, else
	 * recursively call method for the modifiers of the node;
	 * 
	 * For each label, which has a daughter create an opening node "(_label" and a closing node ")_label"
	 * Also add the word
	 */

	//TODO HIERIX
	private void descendFromNode(Dependency dependency, String openNode, String closeNode) {

		this.getLinearizedSentence().add(openNode);
		String word = dependency.getDependentWord()+":"+dependency.getDependent();
		this.getLinearizedSentence().add(word);
		// Modifiers are processed from left to right
		List<Dependency> modifiers = getDepRelsWithHeadId(dependency.getDependent());
		for (int i=0; i < modifiers.size(); i++) {
			descendFromNode(modifiers.get(i), 
					"(_"+modifiers.get(i).getLabel(), 
					")_"+modifiers.get(i).getLabel());
		}
		this.getLinearizedSentence().add(closeNode);
	}

	/**
	 * Linearize a dependency tree/dag into a list of tokens. 
	 * Do left-to-right depth first traversal from root.
	 * Start from root node -> assumed to be unique
	 * 
	 * @param ds
	 * @return
	 */

	public void linearizedDependencyStructure() {
		Dependency root = this.getDs().getDependenciesArray()[this.getDs().getRootPosition()];
		System.out.println("Root:" + root);
		descendFromNode(root, "(_RT", ")_RT");
	}

	public void inverseLinearizedSentence(){
		// given a linearized sentence, create the dependency structure/2-Dim sentence object
		// the resulting sentence object should be equal with the original one
	}

}
