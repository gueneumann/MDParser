package de.dfki.lt.mdparser.features;

import java.util.List;

import de.dfki.lt.mdparser.algorithm.ParserState;
import de.dfki.lt.mdparser.data.Sentence;


public abstract class FeatureModel {
	
	private Alphabet alphabetParser;
	private Alphabet alphabetLabeler;
	private FeatureExtractor fe;
	
	public FeatureModel(Alphabet alphabetParser, Alphabet alphabetLabeler, FeatureExtractor fe) {
		this.alphabetParser = alphabetParser;
		this.alphabetLabeler = alphabetLabeler;
		this.fe = fe;
	}
	
	public FeatureModel(Alphabet alphabetParser, FeatureExtractor fe) {
		this.alphabetParser = alphabetParser;
		this.fe = fe;
	}
	
	public abstract List<FeatureVector> apply(ParserState state, boolean train, boolean noLabels);	
	
	public abstract FeatureVector applyCombined(ParserState state, boolean train, boolean noLabels);		
	
	public abstract void initializeStaticFeaturesCombined(Sentence sent, boolean train);
	
	public void setAlphabetParser(Alphabet alphabetParser) {
		this.alphabetParser = alphabetParser;
	}

	public Alphabet getAlphabetParser() {
		return alphabetParser;
	}

	public void setFeatureExtractor(FeatureExtractor fe) {
		this.fe = fe;
	}

	public FeatureExtractor getFeatureExtractor() {
		return fe;
	}

	public void setAlphabetLabeler(Alphabet alphabetLabeler) {
		this.alphabetLabeler = alphabetLabeler;
	}

	public Alphabet getAlphabetLabeler() {
		return alphabetLabeler;
	}

	
}
