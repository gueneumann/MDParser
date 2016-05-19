package de.dfki.lt.mdparser.features;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.bwaldvogel.liblinear.*;

public class FeatureVector {

	private List<Feature> fList;
	private String standardRepresentation;
	private String integerRepresentation;

	private String label;
	private double[] probArray;

	public FeatureVector(boolean train) {
		fList = new ArrayList<Feature>(40);
	}
	public void addFeature(Feature f, Alphabet alpha, boolean train) {
		fList.add(f);
		if (train && !alpha.getValueToIndexMap().keySet().contains((f.getFeatureString()))) {
			alpha.addFeature(f.getFeatureString());
		}
	}

	public void updateFeature(String featureName, String newValue) {
		for (int i=fList.size()-1; i >= 0; i--) {
			Feature f = fList.get(i);
			if (f.getName().equals(featureName)) {
				System.out.println(f.getValue()+" vs "+newValue);
				f.setValue(newValue);
				f.setFeatureString(featureName+"="+newValue);
				i = -1;
			}
		}
	}

	public Feature getFeature(String featureName) {
		for (int i=0; i < fList.size(); i++) {
			Feature f = fList.get(i);
			if (f.getName().equals(featureName)) {
				return f;
			}
		}
		return null;
	}

	public void setStandardRepresentation(String standardRepresentation) {
		this.standardRepresentation = standardRepresentation;
	}

	public String getStandardRepresentation() {
		return standardRepresentation;
	}

	public void setIntegerRepresentation(String integerRepresentation) {
		this.integerRepresentation = integerRepresentation;
	}

	public String getIntegerRepresentation(Alphabet alpha, boolean labels) {
		StringBuilder sb = new StringBuilder();
		List<Integer> indexList = new ArrayList<Integer>();
		sb.append(alpha.getLabelIndexMap().get(label));
		for (int i = 0; i < fList.size();i++) {
			Feature f = fList.get(i);	
			Integer fIndex = null;
			if (labels)  {
				fIndex = f.getIndexLabeler();
			}
			else {
				fIndex = f.getIndexParser();
			}
			if (fIndex == null) {
				fIndex = alpha.getFeatureIndex(f.getFeatureString());	
				//	System.out.println(fIndex);
				/*	fIndex = f.getValue().hashCode()*(i+1);
				if (fIndex < 0) {
					fIndex *= -1;
				}*/
				//		System.out.println("old: "+alpha.getFeatureIndex(f.getFeatureString())+" new: "+fIndex);
			}
			if (fIndex != null) {
				indexList.add(fIndex);
			}
		}
		Collections.sort(indexList);
		for (int i = 0; i < indexList.size();i++) {
			Integer fIndex = indexList.get(i);
			sb.append(" ");
			sb.append(fIndex);
			sb.append(":");
			sb.append(1);
		}

		this.integerRepresentation = sb.toString();
		return integerRepresentation;
	}

	public FeatureNode[] getLiblinearRepresentation(boolean train, boolean labels, Alphabet alpha) {
		List<Integer> indexList = new ArrayList<Integer>(40);
		for (int i = 0; i < fList.size();i++) {
			Feature f = fList.get(i);	
			Integer fIndex = null;
			if (labels) {
				fIndex = f.getIndexLabeler();
			}
			else {
				fIndex =  f.getIndexParser();
			}
			if (fIndex == null) {
				//	System.out.println(f.getFeatureString()+" "+f.getValue().hashCode()*i);
				/*	fIndex = f.getValue().hashCode()*(i+1);
				if (fIndex < 0) {
					fIndex *= -1;
				}*/
				fIndex = alpha.getFeatureIndex(f.getFeatureString());
			}

			if (fIndex != null) {
				indexList.add(fIndex);
			}
		}
		if (train) {
			Collections.sort(indexList);
		}
		FeatureNode[] fnArray = new FeatureNode[indexList.size()];
		for (int i = 0; i < indexList.size();i++) {
			Integer fIndex = indexList.get(i);
			FeatureNode fn = new FeatureNode(fIndex,1);
			fnArray[i] = fn;
		}
		return fnArray;
	}
	
	public void setfList(List<Feature> fList) {
		this.fList = fList;
	}

	public List<Feature> getfList() {
		return fList;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getLabel() {
		return label;
	}

	public void setProbArray(double[] probArray) {
		this.probArray = probArray;	
	}

	public double[] getProbArray() {
		return this.probArray;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(label);
		for (int i=0; i < fList.size();i++) {
			sb.append(" ");
			sb.append(fList.get(i).getFeatureString());
		}
		return sb.toString();
	}

}
