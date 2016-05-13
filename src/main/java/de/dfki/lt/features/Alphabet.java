package de.dfki.lt.features;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Alphabet {
	
	private List<HashMap<String,Integer>> valueToIndexMapList;
	private HashMap<String,Integer> valueToIndexMap;
	private String[] indexToValueArray;
	private List<HashMap<Integer,String>> indexToValueArrayList;
	private int maxIndex;
//	private List<Integer> maxIndexList;
	
	private HashMap<String,Integer> labelIndexMap;
	private String[] indexLabelArray;
	private int maxLabelIndex;
	
	public Alphabet() {
		setValueToIndexMap(new HashMap<String,Integer>(1000000));
	//	maxIndexList = new ArrayList<Integer>();
		labelIndexMap = new HashMap<String,Integer>(100);
		maxIndex = 1;
		maxLabelIndex = 1;
	}
	
	public Alphabet(String alphabetFile) throws IOException {
		FileInputStream in = new FileInputStream(alphabetFile);
		BufferedInputStream bis = new BufferedInputStream(in, 8000);
		InputStreamReader ir = new InputStreamReader(bis,"UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line;
		int c = 0;
		this.maxLabelIndex = 1;
		this.maxIndex = 1;
		this.labelIndexMap = new HashMap<String, Integer>(1000000);
		this.valueToIndexMap = new HashMap<String, Integer>(100);
		this.indexLabelArray = new String[labelIndexMap.size()+1];
		this.indexToValueArray = new String[valueToIndexMap.size()+1];
		while ((line = fr.readLine()) != null) {
			if (line.length() == 0) {
				c++;
			}
			else {
				String[] lineArray;
				if (c == 0) {
					lineArray = line.split(" ");
					this.labelIndexMap.put(lineArray[1], Integer.valueOf(lineArray[0]));
			//		indexLabelArray[maxLabelIndex] = lineArray[1];
					this.maxLabelIndex++;
				}
				else if (c == 1) {
					lineArray = line.split(" ");					
					this.valueToIndexMap.put(lineArray[1], Integer.valueOf(lineArray[0]));
				//	indexToValueArray[maxIndex] = lineArray[1];
					this.maxIndex++;
				}		
			}
		}
		fr.close();
		createIndexToValueArray();
	}

	public Alphabet(InputStream is) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(is, 8000);
		InputStreamReader ir = new InputStreamReader(bis,"UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line;
		int c = 0;
		this.maxLabelIndex = 1;
		this.maxIndex = 1;
		this.labelIndexMap = new HashMap<String, Integer>(1000000);
		this.valueToIndexMap = new HashMap<String, Integer>(100);
		this.indexLabelArray = new String[labelIndexMap.size()+1];
		this.indexToValueArray = new String[valueToIndexMap.size()+1];
		while ((line = fr.readLine()) != null) {
			if (line.length() == 0) {
				c++;
			}
			else {
				String[] lineArray;
				if (c == 0) {
					lineArray = line.split(" ");
					this.labelIndexMap.put(lineArray[1], Integer.valueOf(lineArray[0]));
			//		indexLabelArray[maxLabelIndex] = lineArray[1];
					this.maxLabelIndex++;
				}
				else if (c == 1) {
					lineArray = line.split(" ");					
					this.valueToIndexMap.put(lineArray[1], Integer.valueOf(lineArray[0]));
				//	indexToValueArray[maxIndex] = lineArray[1];
					this.maxIndex++;
				}		
			}
		}
		fr.close();
		createIndexToValueArray();
	}	
	
	
	public Integer getFeatureIndex(String value) {
		return this.valueToIndexMap.get(value);
	}
	
	
	
	public void setValueToIndexMap(HashMap<String,Integer> valueToIndexMap) {
		this.valueToIndexMap = valueToIndexMap;
	}

	public HashMap<String,Integer> getValueToIndexMap() {
		return valueToIndexMap;
	}

	public void setMaxIndex(int maxIndex) {
		this.maxIndex = maxIndex;
	}

	public int getMaxIndex() {
		return maxIndex;
	}

	public void setMaxLabelIndex(int maxIndex) {
		this.maxLabelIndex = maxIndex;
	}

	public int getMaxLabelIndex() {
		return maxLabelIndex;
	}
	
	public void addFeature(String featureString) {
		valueToIndexMap.put(featureString, maxIndex);
		maxIndex++;		
	}

	public void addLabel(String label) {
		Integer index = labelIndexMap.get(label);
		if (index == null) {
			labelIndexMap.put(label, maxLabelIndex);
			maxLabelIndex++;
		}
		
	}

	public void createIndexToValueArray() {
		this.indexToValueArray = new String[maxIndex];
		Iterator<String> keyIter = this.valueToIndexMap.keySet().iterator();
		while (keyIter.hasNext()) {
			String curKey = keyIter.next();
			Integer curVal = this.valueToIndexMap.get(curKey);
			this.indexToValueArray[curVal] = curKey;
		}
		this.indexLabelArray = new String[maxLabelIndex];
		keyIter = this.labelIndexMap.keySet().iterator();
		while (keyIter.hasNext()) {
			String curKey = keyIter.next();
			Integer curVal = this.labelIndexMap.get(curKey);
			this.indexLabelArray[curVal] = curKey;
		}
	}	
	
	public void printToFile(String alphabetFile) throws IOException {
		createIndexToValueArray();
		FileOutputStream out = new FileOutputStream(alphabetFile);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter fw = new BufferedWriter(or);
		for (int i=1; i < maxLabelIndex; i++) {
			String line = String.format("%d %s\n",i,indexLabelArray[i]);
			fw.append(line);
		}
		fw.append("\n");
		for (int i=1; i < maxIndex; i++) {
			String val = indexToValueArray[i];
			if (val != null) {
				String line = i+" "+val+"\n";
				fw.append(line);
			}
		}
		fw.close();
	}
	
	public void setIndexToValueArray(String[] indexToValueArray) {
		this.indexToValueArray = indexToValueArray;
	}

	public String[] getIndexToValueArray() {
		return indexToValueArray;
	}

	public void setIndexLabelArray(String[] indexLabelArray) {
		this.indexLabelArray = indexLabelArray;
	}

	public String[] getIndexLabelArray() {
		return indexLabelArray;
	}
	
	public void setLabelIndexMap(HashMap<String,Integer> labelIndexMap) {
		this.labelIndexMap = labelIndexMap;
	}
	
	public HashMap<String,Integer> getLabelIndexMap() {
		return this.labelIndexMap;
	}



}
