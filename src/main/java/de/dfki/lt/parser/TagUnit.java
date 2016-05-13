package de.dfki.lt.parser;

import java.util.List;

public class TagUnit {
	
	private List<String> sentence;
	private int id;

	public TagUnit(List<String> sentence, int id) {
		this.setSentence(sentence);
		this.setId(id);
		
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setSentence(List<String> sentence) {
		this.sentence = sentence;
	}

	public List<String> getSentence() {
		return sentence;
	}
}
