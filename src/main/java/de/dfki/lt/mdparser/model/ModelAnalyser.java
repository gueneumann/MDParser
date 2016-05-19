package de.dfki.lt.mdparser.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;

import de.bwaldvogel.liblinear.*;
import de.dfki.lt.mdparser.features.Alphabet;

public class ModelAnalyser {
	
	private String trainingDataDir;
	private String newAlphabetFile;
	private String oldAlphabetFile;
	
	public ModelAnalyser(String trainingDataDir,String oldAlphabetFile,String newAlphabetFile) {
		this.trainingDataDir = trainingDataDir;
		this.oldAlphabetFile = oldAlphabetFile;
		this.newAlphabetFile = newAlphabetFile;
	}

	public void computeSums(String individualSumsFile) throws IOException {
		FileInputStream in = new FileInputStream(individualSumsFile);
		InputStreamReader ir = new InputStreamReader(in,"UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line;
		HashMap<String, Double> usefulnessMap = new HashMap<String,Double>();
		while ((line = fr.readLine())!= null) {
			String[] lineArray = line.split("\\s+");
			String feature = lineArray[0];
			String templateName = feature.split("=")[0];
			lineArray[1] = lineArray[1].replaceAll(",", ".");
			Double sum = Double.valueOf(lineArray[1]);
			Double curWeight = usefulnessMap.get(templateName);
			if (curWeight == null) {
				usefulnessMap.put(templateName, sum);
			}
			else {
				usefulnessMap.put(templateName, sum+curWeight);
			}
		//	System.out.println(feature+" "+templateName+" "+sum);
		}
		fr.close();
		System.out.println(usefulnessMap);
	}
	
	public void setTrainingDataDir(String trainingDataDir) {
		this.trainingDataDir = trainingDataDir;
	}

	public String getTrainingDataDir() {
		return trainingDataDir;
	}

	public void setOldAlphabetFile(String oldAlphabetFile) {
		this.oldAlphabetFile = oldAlphabetFile;
	}

	public String getOldAlphabetFile() {
		return oldAlphabetFile;
	}
	
	public void editTrainingData() throws IOException {
		File[] files = new File(this.trainingDataDir).listFiles();
		for (int i=0; i < files.length;i++) {
			File curFile = files[i];
			edit(curFile,oldAlphabetFile,newAlphabetFile);
			
		}
	}

	private void edit(File curFile, String oldAlphabetFile,String newAlphabetFile) throws IOException {
		Alphabet oldAlpha = new Alphabet(oldAlphabetFile);
		Alphabet newAlpha = new Alphabet(newAlphabetFile);
		FileInputStream in = new FileInputStream(curFile);
		InputStreamReader ir = new InputStreamReader(in,"UTF8");
		BufferedReader fr = new BufferedReader(ir);
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = fr.readLine())!= null) {
			String[] trainingInstance = line.split(" ");
			StringBuilder sb2 = new StringBuilder();
			sb2.append(trainingInstance[0]);
			int c = 0;
			for (int k=1; k < trainingInstance.length;k++) {
				Integer index = Integer.valueOf(trainingInstance[k].split(":")[0]);
				String feature = oldAlpha.getIndexToValueArray()[index];
				Integer newIndex = newAlpha.getValueToIndexMap().get(feature);
				if (newIndex != null) {
					sb2.append(" ");
					sb2.append(newIndex);
					sb2.append(":1");
					c++;
				}
		//		System.out.println(index+" "+newIndex+" ("+feature+")");
			}
			if (c != 0) {
				sb.append(sb2.toString());
			}
			sb.append("\n");
		}
		fr.close();
		FileOutputStream out = new FileOutputStream(curFile);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter fw = new BufferedWriter(or);
		fw.append(sb.toString());
		fw.close();
	}

	public HashMap<String, Double> computeUsefulness(String modelFileDir) throws IOException {
		Alphabet alpha = new Alphabet(newAlphabetFile);
		int numberOfClasses = alpha.getMaxLabelIndex()-1;
		HashMap<String, Double> usefulnessMap = new HashMap<String,Double>();
		File[] files = new File(modelFileDir).listFiles();
		HashMap<String,double[]> weightsMap = readWeightsMap(files);
		files = new File(trainingDataDir).listFiles();
		int c = 0;
		String mode = "2";
		for (int i=0; i < files.length;i++) {
			File curFile = files[i];
			String[] indexToValueArray = alpha.getIndexToValueArray();
			double[] curWeights = weightsMap.get(curFile.getName());
			FileInputStream in = new FileInputStream(curFile);
			InputStreamReader ir = new InputStreamReader(in,"UTF8");
			BufferedReader fr = new BufferedReader(ir);
			String line;
			while ((line = fr.readLine())!= null) {
				if (line.length() > 0) {
				String[] trainingInstance = line.split(" ");
				String label = trainingInstance[0];
				Integer labelIndex = Integer.valueOf(label);
				double sum = 0.0;
				for (int k=1; k < trainingInstance.length;k++) {
					Integer index = Integer.valueOf(trainingInstance[k].split(":")[0]);
			//		System.out.println(k+" "+weightsMap+" "+curFile.getName()+" "+curWeights+" "+index+" "+numberOfClasses+" "+labelIndex);
					double featureWeight = curWeights[(index-1)*numberOfClasses+(labelIndex-1)];
					sum += featureWeight;
				}
		//		if (sum < 0.2) {
					c++;
					for (int k=1; k < trainingInstance.length;k++) {
						Integer index = Integer.valueOf(trainingInstance[k].split(":")[0]);
						String feature = indexToValueArray[index];
						String templateName = feature.split("=")[0];
						double featureWeight = curWeights[(index-1)*numberOfClasses+(labelIndex-1)];
						if (mode.equals("1")) {
							Double curWeight = usefulnessMap.get(feature);
							if (curWeight == null) {
								usefulnessMap.put(feature, featureWeight);
							}
							else {
								usefulnessMap.put(feature, featureWeight+curWeight);
							}
						}
						else if (mode.equals("2")) {
							Double curWeight = usefulnessMap.get(templateName);
							if (curWeight == null) {
								usefulnessMap.put(templateName, featureWeight);
							}
							else {
								usefulnessMap.put(templateName, featureWeight+curWeight);
							}
						}
					}
		//		}
			//	System.out.println("="+sum);
				}
			}
			fr.close();
		}
		System.out.println("Less < 0 "+c);
		Iterator<String> iter = usefulnessMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			usefulnessMap.put(key, usefulnessMap.get(key)/Double.valueOf(c));
		}
		return usefulnessMap;
	}

	private HashMap<String, double[]> readWeightsMap(File[] files) throws IOException {
		HashMap<String,double[]> weightsMap = new HashMap<String,double[]>(files.length);
		for (int i=0; i < files.length;i++) {
			File curFile = files[i];
			Model m = Model.load(curFile);
			weightsMap.put(curFile.getName(),m.getFeatureWeights() );
		}
		return weightsMap;
	}

	public String sortUsefulnessMap(HashMap<String, Double> usefulnessMap) {
		StringBuilder sb = new StringBuilder();
		while (!usefulnessMap.isEmpty()) {
			double max = Double.MIN_VALUE;
			String maxKey = "";
			Iterator<String> iter = usefulnessMap.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				double value = usefulnessMap.get(key);
				if (value > max) {
					max = value;
					maxKey = key;
				}
			}
		//	System.out.print(maxKey+" "+max+"\n");
			sb.append(maxKey+" "+max+"\n");
			usefulnessMap.remove(maxKey);
		}
		return sb.toString();
	}
	
}
