package de.dfki.lt.mdparser.eval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;

public class Eval {


	int correctParents;
	int correctLabels;
	int correctParentsAndLabels;
	int correctPunct;
	int totalPunct;
	int total;
	int unattached;

	private HashMap<String,Integer> posIndexMap;
	private HashMap<String,Integer> labelIndexMap;
	private String[] labelsArray;
	private int[] posCounts;
	private int[] posCorrectCounts;

	public Eval(String goldStandardFile, String parsedFile, int headIndexGold, int headIndexParsed, int labelIndexGold, int labelIndexParsed) throws IOException {
		FileInputStream in = new FileInputStream(goldStandardFile);
		InputStreamReader ir = new InputStreamReader(in,"UTF8");
		BufferedReader frGold = new BufferedReader(ir);
		String lineGold;
		FileInputStream in2 = new FileInputStream(parsedFile);
		InputStreamReader ir2 = new InputStreamReader(in2,"UTF8");
		BufferedReader frParsed = new BufferedReader(ir2);
		String lineParsed;
		while ((lineGold = frGold.readLine())!= null) {
			// XXX
			// GN: added by GN on 30.06.2015:
			// if first element is not a single number, but contains an interval - as it is the case for Universal grammars
			// ignore such a line
			if (!((lineGold.length() > 0) &
					(lineGold.split("\t")[0].contains("-")))){
			lineParsed = frParsed.readLine();
			if (lineGold.length() > 0) {
				//System.out.println("Compare: " + lineGold + "\n         " + lineParsed);
				String[] parsedArray = lineParsed.split("\\s");
				String[] goldArray = lineGold.split("\\s");
				if (!parsedArray[headIndexParsed].equals("_"))
					//System.out.println(parsedArray[headIndexParsed]+" "+(goldArray[headIndexGold]+" --> "+parsedArray[1]+" "+goldArray[1]));
					if (parsedArray[headIndexParsed].equals(goldArray[headIndexGold])) {
						correctParents++;
						if (parsedArray[labelIndexParsed].equals(goldArray[labelIndexGold])) {
							correctLabels++;
						}
					}
				total++;
			}
		}
		}
		frGold.close();
		frParsed.close();

	}
	
	public double getParentsAccuracy() {
		return Double.valueOf(this.correctParents) / Double.valueOf(this.total);
	}

	public double getLabelsAccuracy() {
		return Double.valueOf(this.correctLabels) / Double.valueOf(this.total);
	}

	// GN: the parts below are  not used
	private void createLabelIndexMap(String trainingDataFile, int labelIndexGold) throws IOException {
		FileInputStream in = new FileInputStream(trainingDataFile);
		InputStreamReader ir = new InputStreamReader(in,"UTF8");
		BufferedReader frGold = new BufferedReader(ir);
		String lineGold;
		int curLabelIndex = 0;
		this.labelIndexMap = new HashMap<String, Integer>();
		while ((lineGold = frGold.readLine())!= null) {
			if (lineGold.length() > 0) {
				String[] goldArray = lineGold.split("\\s");
				String label = goldArray[labelIndexGold];
				if (labelIndexMap.get(label) == null) {
					labelIndexMap.put(label,curLabelIndex);
					curLabelIndex++;
				}
			}
		}
		// GN: added this
		frGold.close();
		labelIndexMap.put("mk1", curLabelIndex);
		curLabelIndex++;
		labelsArray = new String[labelIndexMap.size()];
		Iterator<String> iter = labelIndexMap.keySet().iterator();
		while (iter.hasNext()) {
			String label = iter.next();
			Integer index = labelIndexMap.get(label);
			labelsArray[index] = label;
		}
	}
	private void computeLabelConfusionMatrix(String goldStandardFile, String parsedFile, int headIndexGold, 
			int headIndexParsed, int labelIndexGold, int labelIndexParsed, String outputFile) throws IOException {
		FileInputStream in = new FileInputStream(goldStandardFile);
		InputStreamReader ir = new InputStreamReader(in,"UTF8");
		BufferedReader frGold = new BufferedReader(ir);
		String lineGold;
		FileInputStream in2 = new FileInputStream(parsedFile);
		InputStreamReader ir2 = new InputStreamReader(in2,"UTF8");
		BufferedReader frParsed = new BufferedReader(ir2);
		String lineParsed;
		int[][] confusionMatrix = new int[labelIndexMap.size()][labelIndexMap.size()];
		while ((lineGold = frGold.readLine())!= null) {
			lineParsed = frParsed.readLine();
			if (lineGold.length() > 0) {
				String[] parsedArray = lineParsed.split("\\s");
				String[] goldArray = lineGold.split("\\s");
				String curLabelGold = goldArray[labelIndexGold];
				String curLabelParsed = parsedArray[labelIndexParsed];
				//	System.out.println(labelIndexMap+" "+curLabelGold);
				int curLabelGoldIndex = labelIndexMap.get(curLabelGold);
				int curLabelParsedIndex = labelIndexMap.get(curLabelParsed);
				String headGold = goldArray[headIndexGold];
				String headParsed = parsedArray[headIndexParsed];
				if (headGold.equals(headParsed)) {
					confusionMatrix[curLabelParsedIndex][curLabelGoldIndex]++;
				}
			}
		}
		FileOutputStream out = new FileOutputStream(outputFile);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter fw = new BufferedWriter(or);
		frGold.close();
		frParsed.close();
		for (int i=0; i < confusionMatrix.length;i++) {
			fw.append("\t"+labelsArray[i]);
		}
		fw.append("\n");
		for (int i=0; i < confusionMatrix.length;i++) {
			fw.append(labelsArray[i]);
			for (int j=0; j < confusionMatrix.length;j++) {
				fw.append("\t"+confusionMatrix[i][j]);
			}
			double curAcc = 0;
			int sum = 0;
			int cor = 0;
			for (int j=0; j < confusionMatrix.length;j++) {
				sum += confusionMatrix[j][i];
				if (i == j) {
					cor = confusionMatrix[j][i];
				}
			}
			curAcc = Double.valueOf(cor)/Double.valueOf(sum);
			fw.append(String.format("\t%2f",curAcc));
			fw.append("\n");
		}
		for (int i=0; i < confusionMatrix.length;i++) {
			double curAcc = 0;
			int sum = 0;
			int cor = 0;
			for (int j=0; j < confusionMatrix.length;j++) {
				sum += confusionMatrix[i][j];
				if (i == j) {
					cor = confusionMatrix[i][j];
				}
			}
			curAcc = Double.valueOf(cor)/Double.valueOf(sum);
			fw.append(String.format("\t%2f",curAcc));
		}
		fw.close();
	}


	private double getAttachedParentsAccuracy() {
		return Double.valueOf(this.correctParents) / Double.valueOf(this.total-this.unattached);
	}
	

	private double getLabeledParentsAccuracy() {
		return Double.valueOf(this.correctParentsAndLabels) / Double.valueOf(this.total);
	}
	private double getAttachedPercentage() {
		return (Double.valueOf(this.total)-Double.valueOf(this.unattached))/Double.valueOf(this.total);
	}

	private double getPunctAccuracy() {
		return Double.valueOf(this.correctPunct) / Double.valueOf(this.totalPunct);
	}
	private void printOutPosAccuracyMap(String outputFile) throws IOException {
		FileOutputStream out = new FileOutputStream(outputFile);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter fw = new BufferedWriter(or);
		Iterator<String> iter = this.posIndexMap.keySet().iterator();
		while (iter.hasNext()) {
			String curPos = iter.next();
			Integer curIndex = this.posIndexMap.get(curPos);
			Double curPosAccuracy = Double.valueOf(this.posCorrectCounts[curIndex])/
					Double.valueOf(this.posCounts[curIndex]);
			if (curPosAccuracy == 0) {
				curPosAccuracy = 1.0;
			}
			Double weightBoost = 1/curPosAccuracy;
			fw.append(String.format("%s\t%.8f\t%.8f\n",curPos, curPosAccuracy, weightBoost));
		}
		fw.close();
	}

	private void computeAverageProbabilityForCorrectAndWrongDependencies(String parsedFile, int headIndex, int goldStandardHeadIndex, 
			int labelIndex, int goldStandardLabelIndex) throws IOException {
		FileInputStream in = new FileInputStream(parsedFile);
		InputStreamReader ir = new InputStreamReader(in,"UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line;
		double avgProbCor = 0.0;
		double avgProbWrong = 0.0;
		int totalCor = 0;
		int totalWrong = 0;
		while ((line = fr.readLine())!= null) {
			if (line.length() > 0) {
				String[] lineArray = line.split("\t");
				if (lineArray[headIndex].equals(lineArray[goldStandardHeadIndex])) {
					totalCor++;
					avgProbCor += Double.valueOf(lineArray[10]);
				}
				else {
					totalWrong++;
					System.out.println(Double.valueOf(lineArray[10]));
					avgProbWrong += Double.valueOf(lineArray[10]);
				}
			}
		}
		fr.close();
		System.out.println("Average probability for correct dependencies: "+avgProbCor/totalCor);
		System.out.println("Average probability for wrong dependencies: "+avgProbWrong/totalWrong);
	}

	private double getUAS() {
		return Double.valueOf(this.correctParents) / Double.valueOf(this.total);
	}


	// GN: not called; actually only Eval() constructor is called
	public static void main(String[] args) throws IOException {
		Eval ev = new Eval("PIL/devel/devel-htb-ver0.5.my.utf8.conll", "temp/parsed.txt", 6, 6, 7, 7);
		ev.createLabelIndexMap("PIL/train/train-htb-ver0.5.gold.utf8.conll", 7);
		ev.computeLabelConfusionMatrix("PIL/devel/devel-htb-ver0.5.my.utf8.conll", "temp/parsed.txt", 6, 6, 7,7, "temp/pos.txt");
		System.out.println(ev.getParentsAccuracy());

	}

}
