package de.dfki.lt.mdparser.parser;

import java.util.HashMap;

import pi.ParIterator;
import de.dfki.lt.mdparser.algorithm.ParsingAlgorithm;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.FeatureModel;

public class ParserWorkerThread extends Thread {

	private ParIterator<Sentence> pi = null;
	private int id = -1;
	private FeatureModel fm;
	private boolean noLabels;
	private HashMap<String,String> splitMap;
	private ParsingAlgorithm pa;
	
	public ParserWorkerThread(int id, ParIterator<Sentence> iter, ParsingAlgorithm pa, FeatureModel fm2, boolean noLabels2, HashMap<String, String> splitMap2) {
		this.id = id;
		this.pi = iter;
		this.fm = fm2;
		this.noLabels = noLabels2;
		this.splitMap = splitMap2;
		this.pa = pa;
	}
	
	public void run() {
		while (pi.hasNext()) {
			Sentence element = pi.next();
	//		System.out.println(element);
		//	System.out.println("Hello from Thread "+id);
			pa.processCombined(element, fm, noLabels, splitMap);
		//	System.out.println(element);
		}
	//	System.out.println("    Thread "+id+" has finished.");
	}

	public void setSplitMap(HashMap<String,String> splitMap) {
		this.splitMap = splitMap;
	}

	public HashMap<String,String> getSplitMap() {
		return splitMap;
	}

	public void setNoLabels(boolean noLabels) {
		this.noLabels = noLabels;
	}

	public boolean isNoLabels() {
		return noLabels;
	}

	public void setFm(FeatureModel fm) {
		this.fm = fm;
	}

	public FeatureModel getFm() {
		return fm;
	}

}
