package de.dfki.lt.mdparser.features;

public class Feature {

	private String name;
	private String value;
	private String featureString;
	private Integer indexParser;
	private Integer indexLabeler;
	private int nameIndex;
	
	public Feature(String name, String value) {
		this.name = name;
		this.value = value;
		
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append("=");
		sb.append(value);
		this.setFeatureString(sb.toString());
	}
	
	public Feature(int nameIndex, int indexParser) {
		this.indexParser = indexParser;
		this.nameIndex = nameIndex;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setFeatureString(String featureString) {
		this.featureString = featureString;
	}

	public String getFeatureString() {
		return featureString;
	}

	public Integer getIndexParser() {
		return this.indexParser;
	}

	public void setIndexParser(Integer fIndex) {
		this.indexParser = fIndex;
	}
	public Integer getIndexLabeler() {
		return this.indexLabeler;
	}
	
	public void setIndexLabeler(Integer fIndex) {
		this.indexLabeler = fIndex;
		
	}
	
	public String toString() {
		return String.valueOf(this.featureString+"("+this.indexParser+")");
	}

	public void setNameIndex(int nameIndex) {
		this.nameIndex = nameIndex;
	}

	public int getNameIndex() {
		return nameIndex;
	}
	

	
}
