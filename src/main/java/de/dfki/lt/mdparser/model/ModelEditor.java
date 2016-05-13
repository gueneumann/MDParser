package de.dfki.lt.mdparser.model;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import de.dfki.lt.mdparser.features.Alphabet;

public class ModelEditor {
	
	private String solverType;
	private String nr_class;
	private String label;
	private int numberOfClasses;
	private String nr_feature;
	private String bias;
	private HashMap<Integer,Integer> oldIndexToNewIndexMap;
	private double[] weights;
	private Alphabet alpha;
	private HashSet<Integer> neverUsed;
	private HashSet<Integer> lowWeighted;
	private File modelDirectory;
	private File modelFile;
	
	public ModelEditor(String modelFile, String alphabetFile) throws IOException {
		alpha = new Alphabet(alphabetFile);
		alpha.createIndexToValueArray();
		FileInputStream in = new FileInputStream(modelFile);
		BufferedInputStream bis = new BufferedInputStream(in, 8000);
		InputStreamReader ir = new InputStreamReader(bis,"UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line; 	
		solverType = fr.readLine();
		nr_class  = fr.readLine();
		label = fr.readLine();
		nr_feature = fr.readLine();
		numberOfClasses = Integer.valueOf(nr_class.split(" ")[1]);
		bias = fr.readLine();
		fr.readLine();
		if (numberOfClasses != 2) {
			weights = new double[Integer.valueOf(nr_feature.split(" ")[1])*numberOfClasses];
		}
		else {
			weights = new double[Integer.valueOf(nr_feature.split(" ")[1])];
		}
		int i = 0;
		while ((line = fr.readLine())!= null) {
			String[] weightsArray = line.split(" ");
		//	System.out.println(numberOfClasses);
			if (numberOfClasses != 2) {
				for (int c = 0; c < numberOfClasses; c++) {
					weights[i*numberOfClasses+c] = Double.valueOf(weightsArray[c]);
					if (weightsArray[c].contains("e") || weightsArray[c].contains("E") ) {
						weights[i*numberOfClasses+c] = 0;
					}
				}
				i++;
			}
			else {
					weights[i] = Double.valueOf(weightsArray[0]);
					if (weightsArray[0].contains("e") || weightsArray[0].contains("E") ) {
						weights[i] = 0;
					}
					i++;
			}
		}
	/*	neverUsed = new HashSet<Integer>();
//		System.out.println(alphabetFile+": "+this.alpha.getMaxIndex()+" "+wIndex);
		for (int k=0; k < this.alpha.getMaxIndex();k++) {
			boolean neverUsedFeature = zeroes[i];
			if (neverUsedFeature) {
				neverUsed.add(i+1);
			}
		}*/
	}
	
	public ModelEditor(File modelFile, String alphabetFile,boolean newVer) throws IOException {
		alpha = new Alphabet(alphabetFile);
		this.modelFile = modelFile;
		boolean[] zeroes = new boolean[this.alpha.getMaxIndex()];
			FileInputStream in = new FileInputStream(modelFile);
			BufferedInputStream bis = new BufferedInputStream(in, 8000);
			InputStreamReader ir = new InputStreamReader(bis,"UTF8");
			BufferedReader fr = new BufferedReader(ir);
			for (int k=0; k < 6;k++) {
				fr.readLine();
			}
			String line;
			int wIndex = 0;
			while ((line = fr.readLine())!= null) {
				String[] lineArray = line.split("\\s+");
				boolean zeroLine = true;
				for (int k=0; k < lineArray.length && zeroLine;k++) {
					if (Math.abs(Double.valueOf(lineArray[k])) > 0.1) {
				//	if (Math.abs(Double.valueOf(lineArray[k])) != 0 && (!lineArray[k].contains("e") && !lineArray[k].contains("E"))) {
						zeroLine = false;
					}
				}
				if (zeroLine) {
			//		System.out.println(wIndex+" "+zeroes.length+" "+lineArray.length);
					zeroes[wIndex] = true;
				}
		//		else {
		//			System.out.println(line);
		//		}
				wIndex++;
			}
			fr.close();
			
		neverUsed = new HashSet<Integer>();
//		System.out.println(alphabetFile+": "+this.alpha.getMaxIndex()+" "+wIndex);
		for (int i=0; i < this.alpha.getMaxIndex();i++) {
			boolean neverUsedFeature = zeroes[i];
			if (neverUsedFeature) {
				neverUsed.add(i+1);
			}
		}
	//	System.out.println(neverUsed.size());
	}
	

	public ModelEditor(File modelDirectory, String alphabetFile) throws IOException {
		File[] models = modelDirectory.listFiles();
		this.modelDirectory = modelDirectory;
		alpha = new Alphabet(alphabetFile);
		boolean[][] zeroes = new boolean[models.length][this.alpha.getMaxIndex()];
		for (int i=0; i < models.length;i++) {
			File modelFile = models[i];
			FileInputStream in = new FileInputStream(modelFile);
			BufferedInputStream bis = new BufferedInputStream(in, 8000);
			InputStreamReader ir = new InputStreamReader(bis,"UTF8");
			BufferedReader fr = new BufferedReader(ir);
			for (int k=0; k < 6;k++) {
				fr.readLine();
			}
			String line;
			int wIndex = 0;
			while ((line = fr.readLine())!= null) {
				String[] lineArray = line.split("\\s+");
				boolean zeroLine = true;
				for (int k=0; k < lineArray.length && zeroLine;k++) {
					if (Math.abs(Double.valueOf(lineArray[k])) != 0 && (!lineArray[k].contains("e") && !lineArray[k].contains("E"))) {
						zeroLine = false;
					}
				}
				if (zeroLine) {
					zeroes[i][wIndex] = true;
				}
		//		System.out.println(line+" "+zeroLine);
				wIndex++;
			}
		}
		neverUsed = new HashSet<Integer>();
		
		for (int i=0; i < this.alpha.getMaxIndex();i++) {
			boolean neverUsedFeature = true;
			for (int k=0; k < models.length && neverUsedFeature;k++) {
				if (zeroes[k][i] == false) {
					neverUsedFeature = false;
				}
	//			System.out.print(models[k].getName()+" "+(i+1)+" "+neverUsedFeature+" ");
			}
			
			
			if (neverUsedFeature) {
				neverUsed.add(i+1);
			}
		}
		System.out.println("never used "+neverUsed.size()+" out of "+alpha.getMaxIndex()+" features.");
	//	System.out.println(neverUsed.size());


	}
	
	public void editAlphabetAndModels(String alphabetFile) throws IOException {	
		editAlphabet(neverUsed,alphabetFile);
		editModels(neverUsed,modelDirectory);	
	}
	
	public void editAlphabetAndModel(String alphabetFile,String modelFile) throws IOException {	
		editAlphabet(neverUsed,alphabetFile);
		editModel(neverUsed,modelFile);	
	}
	
	public void editAlphabetAndModelInt(String alphabetFile,String modelFile) throws IOException {	
		editAlphabetInt(neverUsed,alphabetFile);
		editModelInt(neverUsed,modelFile);	
	}
	
	
	public void editAlphabetAndModels2(String alphabetFile) throws IOException {	
		editAlphabet(lowWeighted,alphabetFile);
		editModels(lowWeighted,modelDirectory);	
	}
	

	private void editModel(Set<Integer> neverUsed,String modelFile) throws IOException {


			FileInputStream in = new FileInputStream(this.modelFile);
			BufferedInputStream bis = new BufferedInputStream(in, 8000);
			InputStreamReader ir = new InputStreamReader(bis,"UTF8");
			BufferedReader fr = new BufferedReader(ir);
			String line; 	
			solverType = fr.readLine();
			nr_class  = fr.readLine();
			label = fr.readLine();
		//	nr_feature = fr.readLine();
			fr.readLine();
			nr_feature = "nr_feature "+String.valueOf(this.alpha.getMaxIndex()-1);
			numberOfClasses = Integer.valueOf(nr_class.split(" ")[1]);
			bias = fr.readLine();
			fr.readLine();
			if (numberOfClasses != 2) {
				weights = new double[Integer.valueOf(nr_feature.split(" ")[1])*numberOfClasses];
			}
			else {
				weights = new double[Integer.valueOf(nr_feature.split(" ")[1])];
			}
			int k = 0;
			int l = 1;
			while ((line = fr.readLine())!= null) {
				if (!neverUsed.contains(l)) {
					String[] weightsArray = line.split(" ");
					if (numberOfClasses != 2) {
						for (int c = 0; c < numberOfClasses; c++) {
							weights[k*numberOfClasses+c] = Double.valueOf(weightsArray[c]);
						}
						k++;
					}
					else {
						weights[k] = Double.valueOf(weightsArray[0]);		
						k++;
					}
				}
				l++;
			}
			fr.close();
			printToFile(modelFile);
		
		
	}
	private void editModelInt(Set<Integer> neverUsed,String modelFile) throws IOException {


		FileInputStream in = new FileInputStream(modelFile);
		BufferedInputStream bis = new BufferedInputStream(in, 8000);
		InputStreamReader ir = new InputStreamReader(bis,"UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line; 	
		solverType = fr.readLine();
		nr_class  = fr.readLine();
		label = fr.readLine();
		String a = fr.readLine();
	//	fr.readLine();
		nr_feature = "nr_feature "+String.valueOf(this.alpha.getMaxIndex());
		numberOfClasses = Integer.valueOf(nr_class.split(" ")[1]);
		bias = fr.readLine();
		fr.readLine();
		if (numberOfClasses != 2) {
			weights = new double[Integer.valueOf(nr_feature.split(" ")[1])*numberOfClasses];
		}
		else {
			weights = new double[Integer.valueOf(nr_feature.split(" ")[1])];
		}
		int k = 0;
		int l = 1;
		while ((line = fr.readLine())!= null) {
			if (!neverUsed.contains(l)) {
				String[] weightsArray = line.split(" ");
				if (numberOfClasses != 2) {
					for (int c = 0; c < numberOfClasses; c++) {
				//			System.out.println(line);
				//		System.out.println(modelFile+" "+this.modelFile+" "+k+" "+numberOfClasses+" "+weights.length+" "+(k*numberOfClasses+c)+" "+(this.alpha.getMaxIndex()-1)+" "+a);
						weights[k*numberOfClasses+c] = Double.valueOf(weightsArray[c]);
					}
					k++;
				}
				else {
					weights[k] = Double.valueOf(weightsArray[0]);		
					k++;
				}
			}
			l++;
		}
		fr.close();
		printToFile(modelFile);
	
	
}
	
	
	private void editModels(Set<Integer> neverUsed,File modelDirectory) throws IOException {
		File[] models = modelDirectory.listFiles();
		for (int i=0; i < models.length;i++) {
			String modelFile = models[i].getAbsolutePath();
			System.out.print(modelFile+" "+models[i].length()+" ");
			FileInputStream in = new FileInputStream(modelFile);
			BufferedInputStream bis = new BufferedInputStream(in, 8000);
			InputStreamReader ir = new InputStreamReader(bis,"UTF8");
			BufferedReader fr = new BufferedReader(ir);
			String line; 	
			solverType = fr.readLine();
			nr_class  = fr.readLine();
			label = fr.readLine();
		//	nr_feature = fr.readLine();
			fr.readLine();
			nr_feature = "nr_feature "+String.valueOf(this.alpha.getMaxIndex()-1);
			numberOfClasses = Integer.valueOf(nr_class.split(" ")[1]);
			bias = fr.readLine();
			fr.readLine();
			weights = new double[Integer.valueOf(nr_feature.split(" ")[1])*numberOfClasses];
			int k = 0;
			int l = 1;
			while ((line = fr.readLine())!= null) {
				if (!neverUsed.contains(l)) {
					String[] weightsArray = line.split(" ");
					for (int c = 0; c < numberOfClasses; c++) {
						weights[k*numberOfClasses+c] = Double.valueOf(weightsArray[c]);
						if (weightsArray[c].contains("e") || weightsArray[c].contains("E") ) {
							weights[k*numberOfClasses+c] = 0;
						}
					}
					k++;
				}
				l++;
			}
			fr.close();
			printToFile(modelFile);
			System.out.println(new File(modelFile).length());
		}
		
	}

	private void editAlphabet(Set<Integer> neverUsed, String alphabetFile) throws IOException {
		int maxIndex = 1;
		HashMap<String,Integer> valueToIndexMap = this.alpha.getValueToIndexMap();
		HashMap<String,Integer> newValueToIndexMap = new HashMap<String,Integer>(valueToIndexMap.size()-neverUsed.size()+1);
		String[] indexToValueArray = this.alpha.getIndexToValueArray();
		String[] newIndexToValueArray = new String[valueToIndexMap.size()-neverUsed.size()+1];
		for (int i=1; i < this.alpha.getMaxIndex();i++) {
			if (!neverUsed.contains(i)) {
				String oldString = indexToValueArray[i];
				newValueToIndexMap.put(oldString,maxIndex);
				newIndexToValueArray[maxIndex] = oldString;
				maxIndex++;
			}
		}
		this.alpha.setMaxIndex(maxIndex);
		this.alpha.setValueToIndexMap(newValueToIndexMap);
		this.alpha.setIndexToValueArray(newIndexToValueArray);
		this.alpha.printToFile(alphabetFile);
	}

	private void editAlphabetInt(Set<Integer> neverUsed, String alphabetFile) throws IOException {
		int maxIndex = 1;
		HashMap<String,Integer> valueToIndexMap = this.alpha.getValueToIndexMap();
		HashMap<String,Integer> newValueToIndexMap = new HashMap<String,Integer>(valueToIndexMap.size()-neverUsed.size()+1);
		String[] indexToValueArray = this.alpha.getIndexToValueArray();
		String[] newIndexToValueArray = new String[valueToIndexMap.size()-neverUsed.size()+2];
	//	System.out.println(neverUsed);
		for (int i=1; i < this.alpha.getMaxIndex();i++) {
	//		System.out.println(i+" "+neverUsed.contains(i)+" "+indexToValueArray[i]);
			if (!neverUsed.contains(i)) {
				String oldString = indexToValueArray[i];
				newValueToIndexMap.put(oldString,maxIndex);
				newIndexToValueArray[maxIndex] = oldString;
				maxIndex++;
			}
		}
		this.alpha.setMaxIndex(maxIndex);
		this.alpha.setValueToIndexMap(newValueToIndexMap);
		this.alpha.setIndexToValueArray(newIndexToValueArray);
		this.alpha.printToFile(alphabetFile);
	}
	
	public void setSolverType(String solverType) {
		this.solverType = solverType;
	}

	public String getSolverType() {
		return solverType;
	}

	public void setNr_class(String nr_class) {
		this.nr_class = nr_class;
	}

	public String getNr_class() {
		return nr_class;
	}

	public void setNumberOfClasses(int numberOfClasses) {
		this.numberOfClasses = numberOfClasses;
	}

	public int getNumberOfClasses() {
		return numberOfClasses;
	}

	public void setNr_feature(String nr_feature) {
		this.nr_feature = nr_feature;
	}

	public String getNr_feature() {
		return nr_feature;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void setOldIndexToNewIndexMap(HashMap<Integer,Integer> oldIndexToNewIndexMap) {
		this.oldIndexToNewIndexMap = oldIndexToNewIndexMap;
	}

	public HashMap<Integer,Integer> getOldIndexToNewIndexMap() {
		return oldIndexToNewIndexMap;
	}

	public void setWeights(double[] weights) {
		this.weights = weights;
	}

	public double[] getWeights() {
		return weights;
	}
	
	public void setAlpha(Alphabet alpha) {
		this.alpha = alpha;
	}

	public Alphabet getAlpha() {
		return alpha;
	}
	
	public void printToFile(String outputFile) throws IOException {
		FileOutputStream out = new FileOutputStream(outputFile);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter fw = new BufferedWriter(or);
		fw.append(solverType); fw.append("\n");
		fw.append(nr_class); fw.append("\n");
		fw.append(label); fw.append("\n");
		fw.append(nr_feature); fw.append("\n");
		fw.append(bias); fw.append("\n");
		fw.append("w\n");
		if (numberOfClasses != 2) {
			for (int i=0; i < weights.length/numberOfClasses;i++) {
				for (int c=0; c < numberOfClasses; c++) {
					if (weights[i*numberOfClasses+c] == 0) {
						fw.append("0");
					}
					else {
						fw.append(String.valueOf(weights[i*numberOfClasses+c]));
					}
					fw.append(" ");
				}
	
				fw.append("\n");
			}
			fw.close();
		}
		else {
			for (int i=0; i < weights.length;i++) {
				if (weights[i] == 0) {
					fw.append("0");
				}
				else {
					fw.append(String.valueOf(weights[i]));
				}
				fw.append(" ");					
				fw.append("\n");
			}
			fw.close();
		}
	}
	
	public void removeZeroes(String newAlphabetFile, String newModelFile) throws IOException {
		Alphabet newAlphabet = new Alphabet();
		double[] newWeights = new double[weights.length];
		int k = 0;
		for (int i=1; i < alpha.getIndexLabelArray().length; i++) {
			newAlphabet.addLabel(alpha.getIndexLabelArray()[i]);
		}
		for (int i=0; i < weights.length/numberOfClasses;i++) {
			boolean allZeroes = true;
			for (int c=0; c < numberOfClasses; c++) {			
				if (weights[i*numberOfClasses+c] != 0) {
					allZeroes = false;
				}
				newWeights[k*numberOfClasses+c] = weights[i*numberOfClasses+c];
			}
			if (!allZeroes) {
				String featureString = alpha.getIndexToValueArray()[i+1];
				newAlphabet.addFeature(featureString);
				k++;			
			}
		}
		double[] finalWeights = new double[k*numberOfClasses];
		for (int i=0; i < k*numberOfClasses;i++) {			
			finalWeights[i] = newWeights[i];
		}
		weights = finalWeights;
		nr_feature = "nr_feature "+k;
		printToFile(newModelFile);
		newAlphabet.printToFile(newAlphabetFile);
		
	}

	public void setNeverUsed(HashSet<Integer> neverUsed) {
		this.neverUsed = neverUsed;
	}

	public HashSet<Integer> getNeverUsed() {
		return neverUsed;
	}

	public void secureOldAlphabet(String oldAlphaFile) throws IOException {
		this.alpha.printToFile(oldAlphaFile);	
	}

	public void setLowWeighted(HashSet<Integer> lowWeighted) {
		this.lowWeighted = lowWeighted;
	}

	public HashSet<Integer> getLowWeighted() {
		return lowWeighted;
	}
	
	

}
