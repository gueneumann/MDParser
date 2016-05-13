package de.dfki.lt.mdparser.pil;

import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.Feature;
import de.dfki.lt.mdparser.features.FeatureExtractor;
import de.dfki.lt.mdparser.features.FeatureVector;

public class MorphModel {

	private Alphabet alphabetMorph;
	private FeatureExtractor fe;
	
	public MorphModel(Alphabet alphabetMorph, FeatureExtractor fe) {
		this.setAlphabetMorph(alphabetMorph);
		this.setFe(fe);
	}

	public void setAlphabetMorph(Alphabet alphabetMorph) {
		this.alphabetMorph = alphabetMorph;
	}

	public Alphabet getAlphabetMorph() {
		return alphabetMorph;
	}

	public void setFe(FeatureExtractor fe) {
		this.fe = fe;
	}

	public FeatureExtractor getFe() {
		return fe;
	}

	public FeatureVector apply(int j, Sentence sent, boolean train) {
		FeatureVector fv = new FeatureVector(train);
		Feature f1 = fe.templateWf(j, "wfj", sent);
		fv.addFeature(f1, alphabetMorph, train);
		Feature f2 = fe.templatePos(j, "pj", sent);
		fv.addFeature(f2, alphabetMorph, train);
		Feature f3 = fe.templatePos(j+1, "pjp1", sent);
		fv.addFeature(f3, alphabetMorph, train);
		Feature f4 = fe.templatePos(j-1, "pjm1", sent);
		fv.addFeature(f4, alphabetMorph, train);
		Feature f5 = fe.templatePos(j+2, "pjp2", sent);
		fv.addFeature(f5, alphabetMorph, train);
		Feature f6 = fe.templatePos(j-2, "pjm2", sent);
		fv.addFeature(f6, alphabetMorph, train);
		Feature f7 = fe.templateWf(j+1, "wfjp1", sent);
		fv.addFeature(f7, alphabetMorph, train);
		Feature f8 = fe.templateWf(j-1, "wfjm1", sent);
		fv.addFeature(f8, alphabetMorph, train);
		Feature f9 = fe.templateWf(j+2, "wfjp2", sent);
		fv.addFeature(f9, alphabetMorph, train);
		Feature f10 = fe.templateWf(j-2, "wfjm2", sent);
		fv.addFeature(f10, alphabetMorph, train);
		Feature f11 = fe.templatePos(j+3, "pjp3", sent);
		fv.addFeature(f11, alphabetMorph, train);
		Feature f12 = fe.templatePos(j-3, "pjm3", sent);
		fv.addFeature(f12, alphabetMorph, train);
		return fv;
	}
	
}
