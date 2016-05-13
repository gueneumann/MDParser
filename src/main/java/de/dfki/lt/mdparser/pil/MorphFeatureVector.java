package de.dfki.lt.mdparser.pil;

import java.util.HashMap;

import de.dfki.lt.mdparser.features.FeatureVector;

public class MorphFeatureVector {

	private FeatureVector fv;
	private HashMap<String,String> labelsMap;
	
	public MorphFeatureVector(FeatureVector fv, String labelsString) {
		String[] labelsArray = labelsString.split("\\|");
		labelsMap = new HashMap<String,String>(labelsArray.length);
		for (int i=0; i < labelsArray.length;i++) {
			String[] valArray = labelsArray[i].split("-");
			if (valArray.length == 1) {
				labelsMap.put(valArray[0],"null");
			}
			else {
				labelsMap.put(valArray[0],valArray[1]);
			}
		}
		this.fv = fv;
	}

	public void setFv(FeatureVector fv) {
		this.fv = fv;
	}

	public FeatureVector getFv() {
		return fv;
	}

	public void setLabelsMap(HashMap<String,String> labelsMap) {
		this.labelsMap = labelsMap;
	}

	public HashMap<String,String> getLabelsMap() {
		return labelsMap;
	}
}
