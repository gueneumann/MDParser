package de.dfki.lt.mdparser.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
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
import de.dfki.lt.mdparser.features.FeatureVector;
import de.dfki.lt.mdparser.features.StackFeatureModel;

public class Parser {

	private long time;
	
	private double[] weightsParser;
	private double[] weightsLabeler;
	private int numberOfClassesParser;
	private int numberOfClassesLabeler;
	private double prob1;
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
		System.out.println("Time to read models: "+(end-st)+" milliseconds.");
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
		System.out.println("Time to parse: "+Double.valueOf(time));
		System.out.println("Speed (sent/s): " + (sentences.length*1000)/Double.valueOf(time));
		System.out.println("Number of configurations: "+pa.getNumberOfConfigurations());
		System.out.println("Average number of configurations per sentence: "+pa.getNumberOfConfigurations()/sentences.length);
	}
	
	public void parseCombined(String algorithm, Data d, Alphabet alphabetParser, boolean noLabels) throws IOException {
		long st = System.currentTimeMillis();
		splitWeightsMap = new HashMap<String,double[]>();
		splitMap = new HashMap<String,String>();
		BufferedReader fp = new BufferedReader(new FileReader("temp/split.txt"));
		String line;
		while ((line = fp.readLine())!= null) {
			String[] lineArray = line.split(" ");
			splitMap.put(lineArray[0], lineArray[1]);
		}
		fp.close();
		splitModelMap = new HashMap<String,Model>();
		Set<String> values = new HashSet<String>(splitMap.values());
		Iterator<String> iter = values.iterator();
		while (iter.hasNext()) {
			String modelNameOriginal = iter.next();
			String modelName = "splitModels/"+modelNameOriginal.substring(6);		
			InputStream is = new FileInputStream(modelName);
			Model m = Model.load(new InputStreamReader(is));
			splitModelMap.put(modelNameOriginal,m);
			splitWeightsMap.put(modelNameOriginal,m.getFeatureWeights());
			is.close();
		}
		long end = System.currentTimeMillis();
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
		pa.setParser(this);
		long start = System.currentTimeMillis();
		for (int n=0; n < sentences.length;n++) {
			pa.processCombined(sentences[n], fm, noLabels, splitMap);
		}	
		long end2 = System.currentTimeMillis();
		time+= end2-start;
		System.out.println("Time to parse: "+Double.valueOf(end2-start)/1000);
		System.out.println("Number of configurations: "+pa.getNumberOfConfigurations());
		System.out.println("Average number of configurations per sentence: "+pa.getNumberOfConfigurations()/sentences.length);
	}
	
	public void parseCombined(String algorithm, Data d, Alphabet alphabetParser, boolean noLabels, File splitFile, String splitModels) throws IOException {
		long st = System.currentTimeMillis();
		splitWeightsMap = new HashMap<String,double[]>();
		splitMap = new HashMap<String,String>();
		BufferedReader fp = new BufferedReader(new FileReader(splitFile));
		String line;
		while ((line = fp.readLine())!= null) {
			String[] lineArray = line.split(" ");
			splitMap.put(lineArray[0], lineArray[1]);
		}
		fp.close();
		splitModelMap = new HashMap<String,Model>();
		Set<String> values = new HashSet<String>(splitMap.values());
		Iterator<String> iter = values.iterator();
		while (iter.hasNext()) {
			String modelNameOriginal = iter.next();
			String mn= modelNameOriginal.split("/")[1];
			String modelName = splitModels+"/"+mn;		
			InputStream is = new FileInputStream(modelName);
			Model m = Model.load(new InputStreamReader(is));
			splitModelMap.put(modelNameOriginal,m);
			splitWeightsMap.put(modelNameOriginal,m.getFeatureWeights());
			is.close();
		}
		long end = System.currentTimeMillis();
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
		pa.setParser(this);
		long start = System.currentTimeMillis();
		for (int n=0; n < sentences.length;n++) {
			pa.processCombined(sentences[n], fm, noLabels, splitMap);
		}	
		long end2 = System.currentTimeMillis();
		time+= end2-start;
		System.out.println("Time to parse: "+Double.valueOf(end2-start)/1000);
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
	
	public void classify(boolean parser, FeatureVector featureVector, Alphabet alpha,boolean labels) {
		int numberOfClasses = -1;
		double[] weights = null;
		if (parser) {
			numberOfClasses = this.numberOfClassesParser;
			weights = this.weightsParser;
		}
		else {
			numberOfClasses = this.numberOfClassesLabeler;
			weights = this.weightsLabeler;
		}

		String[] indexes = featureVector.getIntegerRepresentation(alpha,labels).split(" ");	
		double[] scores = new double[numberOfClasses];
		for (int f=1; f < indexes.length;f++) {
			int index = Integer.valueOf(indexes[f].split(":")[0]);
			if (index < weights.length/numberOfClasses) {
				for (int c=0; c < numberOfClasses;c++) {
					int loc = (index-1)*numberOfClasses+c;					
					scores[c] += weights[loc];
				}
			}
		}
	//	System.out.println(scores[0]+" "+scores[1]+" "+scores[2]);
		int predictedLabelIndex = -1;
		double max = Double.NEGATIVE_INFINITY;
		double[] probArray = new double[scores.length];
		// determining the index of the top scored label
		for (int i=0; i < scores.length; i++) {
			if (scores[i] > max ) {
				max = scores[i];
				predictedLabelIndex = i+1;
			}
		}
		// determining the index of the second top scored label
		max = Double.NEGATIVE_INFINITY;
		predictedSecondBestLabelIndex = -1;
		for (int i=0; i < scores.length; i++) {
			if ((scores[i] > max) && (i!= predictedLabelIndex-1) ) {
				max = scores[i];
				predictedSecondBestLabelIndex = i+1;
			}
		}
		// calculating probabilities out of the scores
		double sum = 0.0;
		for (int i=0; i < probArray.length; i++) {
			probArray[i] = Math.exp(scores[i]-max);
			sum += probArray[i];
		}
		// normalization
		for (int i=0; i < probArray.length; i++) {
			probArray[i] /= sum;
		}
		Arrays.sort(probArray);
	//	prob1 = probArray[alpha.getLabelIndexMap().get("1")-1];
	/*	de.dfki.lt.mdparser.util.SortedArrayList<Double> sa = new  de.dfki.lt.mdparser.util.SortedArrayList<Double>();
		for (int i=0; i < probArray.length; i++) {
			sa.add(probArray[i]);
		}
		Object[] sortedArray = sa.toArray();
		for (int i=0; i < sortedArray.length; i++) {
			probArray[i] = (Double) sortedArray[sortedArray.length-1-i];
		}*/
		featureVector.setProbArray(probArray);
		featureVector.setLabel(alpha.getIndexLabelArray()[predictedLabelIndex]);
		
	}

	public void classifySplit(boolean parser, FeatureVector featureVector,
			Alphabet alpha, boolean labels) {
		int numberOfClasses = -1;
		double[] weights = null;
		if (parser) {
			numberOfClasses = this.numberOfClassesParser;
	//		System.out.println(splitMap);
	//		System.out.println(featureVector.getFeature("pj"));
	//		System.out.println(splitMap.get(featureVector.getFeature("pj").getFeatureString()));
			String mName = "";
			if (splitMap.get(featureVector.getFeature("pj").getFeatureString()) == null) {
				List<String> mNames = new ArrayList<String>(splitMap.values());
				mName = mNames.get(0).substring(6);
		//		System.out.println("* "+mName);
			}
			else {
				mName = splitMap.get(featureVector.getFeature("pj").getFeatureString()).substring(6);
			//	System.out.println("asd "+mName);
			}
		//	System.out.println(mName+" "+splitWeightsMap);	
			weights = splitWeightsMap.get(mName);
		}
		else {
			numberOfClasses = this.numberOfClassesLabeler;
			String mName = "";
			if (splitMapL.get(featureVector.getFeature("pj").getFeatureString()) == null) {
				List<String> mNames = new ArrayList<String>(splitMapL.values());
				mName = mNames.get(0).substring(7);
		//		System.out.println("* "+mName);
			}
			else {
				mName = splitMapL.get(featureVector.getFeature("pj").getFeatureString()).substring(7);
			//	System.out.println("asd "+mName);
			}
		//	System.out.println(mName+" "+splitWeightsMap);	
			weights = splitWeightsMapL.get(mName);
		//	weights = this.weightsLabeler;
		}
	//	System.out.println(weights+" "+numberOfClasses);
		String[] indexes = featureVector.getIntegerRepresentation(alpha, labels).split(" ");	
		double[] scores = new double[numberOfClasses];
		for (int f=1; f < indexes.length;f++) {
			int index = Integer.valueOf(indexes[f].split(":")[0]);
			if (index < weights.length/numberOfClasses) {
				for (int c=0; c < numberOfClasses;c++) {
					int loc = (index-1)*numberOfClasses+c;
				//	System.out.println(index+" "+loc+" "+c);
					scores[c] += weights[loc];
				}
			}
		}
	//	System.out.println(scores[0]+" "+scores[1]+" "+scores[2]);
		int predictedLabelIndex = -1;
		double max = Double.NEGATIVE_INFINITY;
		double[] probArray = new double[scores.length];
		// determining the index of the top scored label
		for (int i=0; i < scores.length; i++) {
			if (scores[i] > max ) {
				max = scores[i];
				predictedLabelIndex = i+1;
			}
		}
		// determining the index of the second top scored label
		max = Double.NEGATIVE_INFINITY;
		predictedSecondBestLabelIndex = -1;
		for (int i=0; i < scores.length; i++) {
			if ((scores[i] > max) && (i!= predictedLabelIndex-1) ) {
				max = scores[i];
				predictedSecondBestLabelIndex = i+1;
			}
		}
		// calculating probabilities out of the scores
		double sum = 0.0;
		for (int i=0; i < probArray.length; i++) {
			probArray[i] = Math.exp(scores[i]-max);
			sum += probArray[i];
		}
		// normalization
		for (int i=0; i < probArray.length; i++) {
			probArray[i] /= sum;
		}
	//	prob1 = probArray[alpha.getLabelIndexMap().get("1")-1];
		Arrays.sort(probArray);
	/*	de.dfki.lt.mdparser.util.SortedArrayList<Double> sa = new  de.dfki.lt.mdparser.util.SortedArrayList<Double>();
		for (int i=0; i < probArray.length; i++) {
			sa.add(probArray[i]);
		}
		Object[] sortedArray = sa.toArray();
		for (int i=0; i < sortedArray.length; i++) {
			probArray[i] = (Double) sortedArray[sortedArray.length-1-i];
		}*/
		featureVector.setProbArray(probArray);
		featureVector.setLabel(alpha.getIndexLabelArray()[predictedLabelIndex]);
		
	}

	public void readSplitModels(String splitFile, String modelsDir) throws IOException {
		splitWeightsMap = new HashMap<String,double[]>();
		splitMap = readSplitFile(splitFile);
		splitModelMap = new HashMap<String,Model>();
		File[] models = new File(modelsDir).listFiles();
		for (int i=0; i < models.length;i++) {
			Model m = Model.load(models[i]);
			splitModelMap.put(models[i].getName(),m);
			splitWeightsMap.put(models[i].getName(),m.getFeatureWeights());
		//	System.out.println(models[i].getName());
		}

	}
	
	private void readSplitModelsL(String splitFileL, String modelsDirL) throws IOException {		
		splitWeightsMapL = new HashMap<String,double[]>();
		setSplitMapL(readSplitFile(splitFileL));
		splitModelMapL = new HashMap<String,Model>();
		File[] models = new File(modelsDirL).listFiles();
		for (int i=0; i < models.length;i++) {
			Model m = Model.load(models[i]);
			splitModelMapL.put(models[i].getName(),m);
			splitWeightsMapL.put(models[i].getName(),m.getFeatureWeights());
		//	System.out.println(models[i].getName());
		}
		
	}

	private void readSplitAlphabets(Archivator arch) throws IOException {
		splitAlphabetsMap = new HashMap<String,Alphabet>();
		Set<String> values = new HashSet<String>(splitMap.values());
		Iterator<String> iter = values.iterator();
		while (iter.hasNext()) {
			String modelNameOriginal = iter.next();
			String alphabetName = "splitA/"+modelNameOriginal.substring(6);	
			System.out.println(alphabetName);
			Alphabet alpha = new Alphabet(alphabetName);
			splitAlphabetsMap.put(alphabetName, alpha);
		}
		
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
	
	public void readSplitModelsL(Archivator arch) throws IOException {	

		splitWeightsMapL = new HashMap<String,double[]>();
		splitMapL = readSplitFile(arch.getSplitLFileInputStream());
		splitModelMapL = new HashMap<String,Model>();
		Set<String> values = new HashSet<String>(splitMapL.values());
		Iterator<String> iter = values.iterator();
		while (iter.hasNext()) {
			String modelNameOriginal = iter.next();
			String	modelName = "splitModelsL/"+modelNameOriginal.substring(7);
			InputStream is = arch.getInputStream(modelName);
			Model m = Model.load(new InputStreamReader(is));
			splitModelMapL.put(modelNameOriginal,m);
			splitWeightsMapL.put(modelNameOriginal,m.getFeatureWeights());
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
		this.weightsLabeler = featureWeights;
		
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

	public void setTime(long time) {
		this.time = time;
	}

	public long getTime() {
		return time;
	}
}
