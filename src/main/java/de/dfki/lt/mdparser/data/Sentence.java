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
		return sentArray;
	}
	public void setRootPosition(int rootPosition) {
		this.rootPosition = rootPosition;
	}
	public Integer getRootPosition() {
		return rootPosition;
	}

	public void setStaticFeatures(Feature[][][] staticFeatures) {
		this.staticFeatures = staticFeatures;
	}
	public Feature[][][] getStaticFeatures() {
		return staticFeatures;
	}
	
	// Init class
	public Sentence(Integer sentSize, int infoSize) {
		sentArray = new String[sentSize][infoSize];
		rootPosition = -1;
	}

	public Sentence(String[][] sentArray) {
		this.sentArray = sentArray;
		rootPosition = -1;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < sentArray.length; i++) {
			for (int j=0; j < sentArray[0].length-1; j++) {
				if(sentArray[i][j] == null) {
					sentArray[i][j] = "_";
				}
				sb.append(sentArray[i][j]);
				sb.append("\t");
			}
			sb.append(sentArray[i][sentArray[0].length-1]);
			sb.append("\n");
		}
		return sb.toString();
	}

}
