package de.dfki.lt.mdparser.features;

import de.dfki.lt.mdparser.data.Sentence;

// GN: OBACHT: templates are functions and return exactly one feature.
public class FeatureExtractor {


	// GN: return POS of token index in sent 
	// -> is it basically index because array and deptree are counted from 0/1 ?!
	public Feature templatePos(int index, String indexName, Sentence sent) {
		String pos = "";
		if (index <= 0) {
			pos = "null";
		}
		else if (index > sent.getSentArray().length) {
			pos = "null";
		}
		else {
			// 3 -> 4th column in CONLL format -> coarse-grained POS
			pos = sent.getSentArray()[index-1][3];
		}		
		Feature f = new Feature(indexName, pos);
		return f;
	}

	// GN:	same as templatePos() -> used as static feature
	public Feature templateCPos(int index, String indexName, Sentence sent) {
		String pos = "";
		if (index <= 0) {
			pos = "null";
		}
		else if (index > sent.getSentArray().length) {
			pos = "null";
		}
		else {
			// 3 -> 4th column in CONLL format -> coarse-grained POS
			pos = sent.getSentArray()[index-1][3];
			//	String[] feats = sent.getSentArray()[index-1][4].split("\\|");
			//	pos = feats[feats.length-1];
		}		
		Feature f = new Feature(indexName, pos);
		return f;
	}

	// GN: return WF of j token in sent
	public Feature templateWf(int index, String indexName, Sentence sent) {
		String wf = "";
		if (index <= 0) {
			wf = "null";
		}
		else if (index > sent.getSentArray().length) {
			wf = "null";
		}
		else {
			// 1 -> 2nd column in CONLL format -> word-form
			wf = sent.getSentArray()[index-1][1];
		}		
		Feature f = new Feature(indexName, wf);
		return f;
	}

	// Returns the dependency relation of head of index, which is parent dependency relation PDEPREL at cell s[9]
	public Feature templateDepRel(int index, String indexName, Sentence sent, boolean train) {
		//	String depRel = curDepStruct.getLabels()[index];
		String depRel;
		if (index < 0) {
			depRel = "null";
		}
		else if (index == 0) {
			depRel = "none";
		}
		else {
			// 9 -> 10th column used for "storing" oracle/predicted label 
			// -> at least, seems so; because also these columns are overwritten with "_" when training is read in Data()
			if (train) {
//							System.out.println(sent.toString());
				if (sent.getSentArray()[index-1][9] == null || sent.getSentArray()[index-1][9].equals("_")) {
					depRel = "none";
				}
				else {
					depRel = sent.getSentArray()[index-1][9];
				}
			}
			else {
				if (sent.getSentArray()[index-1][9] == null || sent.getSentArray()[index-1][9].equals("_")) {
					depRel = "none";
				}
				else {
					depRel = sent.getSentArray()[index-1][9];
				}
			}
		}	
		
		Feature f = new Feature(indexName, depRel);
		// System.out.println(f.toString());
		// computes something like: deprdi=null(null), depldj=SB(null), depi=ROOT(null)
		return f;
	}
	
	public Feature merge2(int i, String[] mergeFeatureNames, Feature f1, Feature f2) {	
		String name = mergeFeatureNames[i];
		String value = f1.getValue()+"_"+f2.getValue();	
		//	String value = String.valueOf(Integer.valueOf(f1.getValue().hashCode())*Integer.valueOf(f2.getValue().hashCode()));
		Feature f = new Feature(name, value);
		return f;	
	}

	public Feature merge3(int i, String[] mergeFeatureNames, Feature f1, Feature f2, Feature f3) {
		String name = mergeFeatureNames[i];
		String value = f1.getValue()+"_"+f2.getValue()+"_"+f3.getValue();	
		//	String value = String.valueOf(Integer.valueOf(f1.getValue().hashCode())*Integer.valueOf(f2.getValue().hashCode())*Integer.valueOf(f3.getValue().hashCode()));
		Feature f = new Feature(name, value);
		return f;	
	}

	public Feature templateDistance(int j, int i) {		
		int dist = j-i;
		int distClass;
		if (j < 0 || i < 0) {
			distClass = 0;
		}
		else if (dist == 1) {
			distClass = 1;
		}
		else if (dist == 2) {
			distClass = 2;
		}
		else if (dist == 3) {
			distClass = 3;
		}
		else if (dist < 6) {
			distClass = 4;
		}
		else if (dist < 10) {
			distClass = 5;
		}
		else {
			distClass = 6;
		}
		Feature f = new Feature("dist",String.valueOf(distClass));
		return f;
	}
}
