package de.dfki.lt.mdparser.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import pi.ParIterator;
import pi.ParIteratorFactory;

import de.bwaldvogel.liblinear.*;
import de.dfki.lt.mdparser.algorithm.CovingtonAlgorithm;
import de.dfki.lt.mdparser.algorithm.ParsingAlgorithm;
import de.dfki.lt.mdparser.algorithm.StackAlgorithm;
import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.data.Data;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.CovingtonFeatureModel;
import de.dfki.lt.mdparser.features.FeatureExtractor;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.StackFeatureModel;

public class Parser {

	public static long time;

	private double[] weightsParser;
	private int numberOfClassesParser;
	private int numberOfClassesLabeler;
	private int predictedSecondBestLabelIndex;

	private HashMap<String,Model> splitModelMap;
	private HashMap<String, String> splitMap;
	private HashMap<String, Model> splitModelMapL;
	private HashMap<String, String> splitMapL;

	private HashMap<String,double[]> splitWeightsMap;
	private HashMap<String,double[]> splitWeightsMapL;

	private HashMap<String,Alphabet> splitAlphabetsMap;

	// GN: Used in MDNer
	public void parseCombined(String algorithm, Data d, Archivator arch, Alphabet alphabetParser,	 boolean noLabels) throws IOException {
		long st = System.currentTimeMillis();
		readSplitModels(arch);
		//	readSplitAlphabets(arch);
		long end = System.currentTimeMillis();
		System.out.println("Time to read model (msec): " + (end-st));
		//gds	readSplitModelsL(arch);
		FeatureExtractor fe = new FeatureExtractor();
		Sentence[] sentences = d.getSentences();
		FeatureModel fm = null;
		ParsingAlgorithm pa = null;
		if (algorithm.equals("covington")) {
			fm = new CovingtonFeatureModel(alphabetParser,  fe);
			pa = new CovingtonAlgorithm();
			pa.setNumberOfConfigurations(0);

		}
		else if (algorithm.equals("stack")) {
			fm = new StackFeatureModel(alphabetParser, fe);
			pa = new StackAlgorithm();
			pa.setNumberOfConfigurations(0);
		}
		
		pa.initLabelFreqMap();
		pa.setParser(this);
		long start = System.currentTimeMillis();
		Runtime runtime = Runtime.getRuntime();
		int numberOfProcessors = runtime.availableProcessors();
		//  System.out.println("Number of processors used: "+numberOfProcessors);
		int threadCount = numberOfProcessors;
		//    int threadCount = 1;
		List<Sentence> sentencesList = new ArrayList<Sentence>(sentences.length);
		for (int n=0; n < sentences.length;n++) {
			sentencesList.add(sentences[n]);
		}
		ParIterator<Sentence> iter = ParIteratorFactory.createParIterator(sentencesList, threadCount);
		Thread[] threadPool = new ParserWorkerThread[threadCount];
		for (int i = 0; i < threadCount; i++) {
			threadPool[i] = new ParserWorkerThread(i, iter, pa,fm, noLabels,splitMap);
			threadPool[i].start();
		}
		/*	for (int n=0; n < sentences.length;n++) {
			pa.processCombined(sentences[n], fm, noLabels, splitMap);
		}	*/
		// Main thread waits for worker threads to complete
		for (int i = 0; i < threadCount; i++) {
			try {
				threadPool[i].join();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		//	System.out.println("All worker threads have completed.");
		long end2 = System.currentTimeMillis();
		time+= end2-start;
		System.out.println("No. of threads: " + threadCount);
		System.out.println("Time to parse (msec): " + Double.valueOf(time));
		System.out.println("Speed (sent/s): " + (sentences.length*1000)/Double.valueOf(time));
		System.out.println("Number of configurations: "+pa.getNumberOfConfigurations());
		System.out.println("Average number of configurations per sentence: "+pa.getNumberOfConfigurations()/sentences.length);
	}

	public HashMap<String,String> readSplitFile(String splitFile) throws IOException {
		HashMap<String,String> splitMap = new HashMap<String,String>();
		BufferedReader fp = new BufferedReader(new FileReader(splitFile));
		String line;
		while ((line = fp.readLine())!= null) {
			String[] lineArray = line.split(" ");
			//	System.out.println(line);
			splitMap.put(lineArray[0], lineArray[1]);
		}
		fp.close();
		return splitMap;
	}

	private HashMap<String,String> readSplitFile(InputStream splitFileIs) throws IOException {
		HashMap<String,String> splitMap = new HashMap<String,String>();
		InputStreamReader ir = new InputStreamReader(splitFileIs,"UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line;
		while ((line = fr.readLine())!= null) {
			String[] lineArray = line.split(" ");
			splitMap.put(lineArray[0], lineArray[1]);
		}
		return splitMap;
	}
	
	public void readSplitModels(Archivator arch) throws IOException {

		splitWeightsMap = new HashMap<String,double[]>();
		splitMap = readSplitFile(arch.getSplitFileInputStream());
		splitModelMap = new HashMap<String,Model>();
		Set<String> values = new HashSet<String>(splitMap.values());
		Iterator<String> iter = values.iterator();
		while (iter.hasNext()) {
			String modelNameOriginal = iter.next();
			String modelName = "splitModels/"+modelNameOriginal.substring(6);		
			InputStream is = arch.getInputStream(modelName);
			Model m = Model.load(new InputStreamReader(is));
			splitModelMap.put(modelNameOriginal,m);
			splitWeightsMap.put(modelNameOriginal,m.getFeatureWeights());
		}
	}

	public void setNumberOfClassesParser(int numberOfClassesParser) {
		this.numberOfClassesParser = numberOfClassesParser;
	}

	public int getNumberOfClassesParser() {
		return numberOfClassesParser;
	}

	public double[] getWeightsParser() {
		return weightsParser;
	}

	public void setWeightsParser(double[] featureWeights) {
		this.weightsParser = featureWeights;

	}

	public void setNumberOfClassesLabeler(int numberOfClassesLabeler) {
		this.numberOfClassesLabeler = numberOfClassesLabeler;
	}

	public int getNumberOfClassesLabeler() {
		return numberOfClassesLabeler;
	}

	public void setWeightsLabeler(double[] featureWeights) {

	}

	public void setPredictedSecondBestLabelIndex(
			int predictedSecondBestLabelIndex) {
		this.predictedSecondBestLabelIndex = predictedSecondBestLabelIndex;
	}

	public int getPredictedSecondBestLabelIndex() {
		return predictedSecondBestLabelIndex;
	}

	public void setSplitModelMap(HashMap<String,Model> splitWeightsMap) {
		this.splitModelMap = splitWeightsMap;
	}

	public HashMap<String,Model> getSplitModelMap() {
		return splitModelMap;
	}

	public void setSplitModelMapL(HashMap<String,Model> splitWeightsMap) {
		this.splitModelMapL = splitWeightsMap;
	}

	public HashMap<String,Model> getSplitModelMapL() {
		return splitModelMapL;
	}

	public HashMap<String,String> getSplitMap() {
		return this.splitMap;
	}

	public void setSplitMapL(HashMap<String, String> splitMapL) {
		this.splitMapL = splitMapL;
	}

	public HashMap<String, String> getSplitMapL() {
		return splitMapL;
	}

	public void setSplitWeightsMap(HashMap<String,double[]> splitWeightsMap) {
		this.splitWeightsMap = splitWeightsMap;
	}

	public HashMap<String,double[]> getSplitWeightsMap() {
		return splitWeightsMap;
	}

	public void setSplitWeightsMapL(HashMap<String,double[]> splitWeightsMapL) {
		this.splitWeightsMapL = splitWeightsMapL;
	}

	public HashMap<String,double[]> getSplitWeightsMapL() {
		return splitWeightsMapL;
	}

	public void setSplitAlphabetsMap(HashMap<String,Alphabet> splitAlphabetsMap) {
		this.splitAlphabetsMap = splitAlphabetsMap;
	}

	public HashMap<String,Alphabet> getSplitAlphabetsMap() {
		return splitAlphabetsMap;
	}
}
