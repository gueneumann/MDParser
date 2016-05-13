package de.dfki.lt.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.dfki.lt.algorithm.DependencyStructure;
import de.dfki.lt.data.Sentence;

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
			//	String[] feats = sent.getSentArray()[index-1][3].split("\\|");
			//	pos = feats[feats.length-1];
			//	System.out.println(pos);

			// GN: for testing
			//System.out.println("templatePos j: " + index);
			//System.out.println(sent.toString());

			pos = sent.getSentArray()[index-1][3];

			//System.out.println("POS: " + pos);
		}		
		Feature f = new Feature(indexName, pos);
		return f;
	}

	// GN: not used
	public String templatePosInt(int index, Sentence sent) {
		String pos = "";
		if (index <= 0) {
			pos = "null";
		}
		else if (index > sent.getSentArray().length) {
			pos = "null";
		}
		else {
			//	String[] feats = sent.getSentArray()[index-1][3].split("\\|");
			//	pos = feats[feats.length-1];
			//	System.out.println(pos);
			pos = sent.getSentArray()[index-1][3];
		}		
		return pos;
	}

	// GN:	same as templatePos() -> used as static feature in 
	//		de.dfki.lt.mdparser.features.FeatureExtractor.templateCPos(int, String, Sentence)
	public Feature templateCPos(int index, String indexName, Sentence sent) {
		String pos = "";
		if (index <= 0) {
			pos = "null";
		}
		else if (index > sent.getSentArray().length) {
			pos = "null";
		}
		else {
			pos = sent.getSentArray()[index-1][3];
			//	String[] feats = sent.getSentArray()[index-1][4].split("\\|");
			//	pos = feats[feats.length-1];
		}		
		Feature f = new Feature(indexName, pos);
		return f;
	}	

	// GN: not used
	// Returns feature-FEAT from 3-cell ! (seems to be a different conll-table)
	public Feature templateFeat(int index,String indexName, String featFunction, Sentence sent) {
		String caseVal = "";
		if (index <= 0) {
			caseVal = "null";
		}
		else if (index > sent.getSentArray().length) {
			caseVal = "null";
		}
		else {
			String[] feats = sent.getSentArray()[index-1][3].split("\\|");
			for (int i=0; i < feats.length;i++) {
				String[] curFeat = feats[i].split("_");
				if (curFeat.length > 1) {
					String funct = curFeat[0];
					if (funct.equals(featFunction)) {
						caseVal = curFeat[1];
					}
				}
			}
		}		
		Feature f = new Feature(featFunction+indexName, caseVal);
		return f;
	}

	// GN: not used
	// // Returns feature-FEAT from 5-cell !
	public Feature templateFeat(int index,String indexName, Sentence sent) {
		String caseVal = "";
		if (index <= 0) {
			caseVal = "null";
		}
		else if (index > sent.getSentArray().length) {
			caseVal = "null";
		}
		else {
			caseVal = sent.getSentArray()[index-1][5];
		}		
		Feature f = new Feature("case"+indexName, caseVal);
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
			wf = sent.getSentArray()[index-1][1];
		}		
		Feature f = new Feature(indexName, wf);
		return f;
	}

	// GN: not used, unclear here what conll cell is referring to
	public Feature templateAmb(int index, String indexName, Sentence sent) {
		String amb = "";
		if (index <= 0) {
			amb = "null";
		}
		else if (index > sent.getSentArray().length) {
			amb = "null";
		}
		else {
			amb = sent.getSentArray()[index-1][10];
			/*	Integer ambClass = Integer.valueOf(amb);
			if (ambClass > 5) {
				amb = "6";
			}*/

		}		
		Feature f = new Feature(indexName, amb);
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
			if (train) {
				//			System.out.println(sent.getSentArray()[index-1][9]);
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
		//	System.out.println(train+" "+depRel);
		Feature f = new Feature(indexName, depRel);
		return f;
	}

	// GN: not used, 
	public Feature templateDepReverb(int index, String indexName, Sentence sent) {
		//	String depRel = curDepStruct.getLabels()[index];
		String depRel;
		if (index < 0) {
			depRel = "null";
		}
		else if (index == 0) {
			depRel = "none";
		}
		else if (index > sent.getSentArray().length) {
			depRel = "null";
		}
		else {
			depRel = sent.getSentArray()[index-1][7];
		}
		Feature f = new Feature(indexName, depRel);
		return f;
	}

	// GN: Not used

	public Feature templatePrevTag(int index,String indexName,Sentence sent, int col) {
		String pos = "";
		if (index <= 0) {
			pos = "null";
		}
		else if (index > sent.getSentArray().length) {
			pos = "null";
		}
		else {
			pos = sent.getSentArray()[index-1][col];
		}		
		Feature f = new Feature(indexName, pos);
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

	public Feature[] templateFeatsFinnish(int j, String indexName, Sentence curSent) {

		String featsVal = curSent.getSentArray()[j-1][3];
		String[] feats = featsVal.split("\\|");
		if (feats.length == 1) {
			return null;
		}
		else {
			Feature[] featsFeatureArray = new Feature[feats.length-1];
			for (int i=0; i < feats.length-1;i++) {
				String[] curFeatArray = feats[i].split("_");
				Feature f = new Feature(curFeatArray[0]+indexName,curFeatArray[1]);
				featsFeatureArray[i] = f;
			}
			return featsFeatureArray;
		}	
	}

	public Feature[] templateFeatsIndian(int j, String indexName, Sentence curSent) {		
		String featsVal = curSent.getSentArray()[j-1][5];
		String[] feats = featsVal.split("\\|");
		if (feats.length == 1) {
			return null;
		}
		else {			
			List<Feature> featureList = new ArrayList<Feature>();
			for (int i=0; i < feats.length-1;i++) {
				//	System.out.println(feats[i]);
				String[] curFeatArray = feats[i].split("-");
				String val = "";
				if (curFeatArray.length != 1) {
					val = curFeatArray[1];
				}

				if (curFeatArray[0].equals("pers")||
						curFeatArray[0].equals("cat")||
						curFeatArray[0].equals("num")||
						curFeatArray[0].equals("case") ||
						curFeatArray[0].equals("gen")||
						curFeatArray[0].equals("tam")||
						curFeatArray[0].equals("vib")
						//	||curFeatArray[0].equals("chunkType")||curFeatArray[0].equals("chunkId")
						||curFeatArray[0].equals("stype")||
						curFeatArray[0].equals("voicetype")) {
					Feature f = new Feature(curFeatArray[0]+indexName,val);
					//	System.out.println(f.getFeatureString());
					featureList.add(f);
				}
			}
			Feature[] featsFeatureArray = new Feature[featureList.size()];
			featsFeatureArray = featureList.toArray(featsFeatureArray);
			return featsFeatureArray;
		}

	}

	//******************************* Features for MDNer by GN, July 2014

	// GN: Could also be defined as merge2(wfi,wfj)

	public Feature templateWFwindow(int index, String indexName, Sentence sent) {
		String wf= "";
		if (index <= 0) {
			wf = "null";
		}
		else if (index > sent.getSentArray().length) {
			wf = "null";
		}
		else {
			String leftWord = ((index-1)==0) ? "bos" : sent.getSentArray()[index-2][1];
			String rightWord = ((index-1)== (sent.getSentArray().length-1)) ? "eos" : sent.getSentArray()[index][1];
			wf = leftWord+"_"+rightWord;	
		}		
		Feature f = new Feature(indexName, wf);
		return f;
	}

	// I think this is different from computing the wordform of token i (which is usually any token left to
	// the current token, where windowLeft actually computes token j-1
	public Feature templateWFwindowLeft(int index, String indexName, Sentence sent) {
		String wf= "";
		if (index <= 0) {
			wf = "null";
		}
		else if (index > sent.getSentArray().length) {
			wf = "null";
		}
		else {
			String leftWord = ((index-1)==0) ? "bos" : sent.getSentArray()[index-2][1];
			wf = leftWord+"_";	
		}		
		Feature f = new Feature(indexName, wf);
		return f;
	}

	// this should basically be the same as computing wfjp1, which is already done, and hence, shows no effect
	public Feature templateWFwindowRight(int index, String indexName, Sentence sent) {
		String wf= "";
		if (index <= 0) {
			wf = "null";
		}
		else if (index > sent.getSentArray().length) {
			wf = "null";
		}
		else {
			String rightWord = ((index-1)== (sent.getSentArray().length-1)) ? "eos" : sent.getSentArray()[index][1];
			wf = "_"+rightWord;	
		}		
		Feature f = new Feature(indexName, wf);
		return f;
	}


	// GN: return WF of j token in sent
	public Feature templateWFprefix(int index, String indexName, Sentence sent, int sl) {
		//			System.out.println("wf: "+sent.getSentArray()[index-1][1] + 
		//					" sl: " + sl + " len: " + sent.getSentArray()[index-1][1].length());
		String suffix= "";
		if (index <= 0) {
			suffix = "null";
		}
		else if (index > sent.getSentArray().length) {
			suffix = "null";
		}
		else {
			suffix = ((sent.getSentArray()[index-1][1].length() - sl) <= 0) ? "null" : 
				sent.getSentArray()[index-1][1].substring(0,sl);	
			//				System.out.println("Suff: "+ suffix);
		}		
		Feature f = new Feature(indexName, suffix);
		return f;
	}

	// GN: return WF of j token in sent
	public Feature templateWFsuffix(int index, String indexName, Sentence sent, int sl) {
		//					System.out.println("wf: "+sent.getSentArray()[index-1][1] + 
		//							" sl: " + sl + " len: " + sent.getSentArray()[index-1][1].length());
		String suffix= "";
		if (index <= 0) {
			suffix = "null";
		}
		else if (index > sent.getSentArray().length) {
			suffix = "null";
		}
		else {
			suffix = ((sent.getSentArray()[index-1][1].length() - sl) <= 0) ? "null" : 
				sent.getSentArray()[index-1][1].substring(
						(sent.getSentArray()[index-1][1].length()-sl), 
						sent.getSentArray()[index-1][1].length());	
			//						System.out.println("Suff: "+ suffix);
		}		
		Feature f = new Feature(indexName, suffix);
		return f;
	}

	// GN: return WF of j token in sent
	public Feature templateWFcaps(int index, String indexName, Sentence sent) {
		String wf= "";
		if (index <= 0) {
			wf = "null";
		}
		else if (index > sent.getSentArray().length) {
			wf = "null";
		}
		else {
			if ((index-1)==0) {
				wf="null";
			}
			else
				wf = (Character.isUpperCase(sent.getSentArray()[index-1][1].charAt(0))) ? "yesCap" : "noCap";
		}		
		Feature f = new Feature(indexName, wf);
		return f;
	}

	// Map Strings to shape, e.g., Jenny -> Xxxx, IL-2 -> XX-#
	// Similar to Stanford-NER
	public Feature templateWFshape(int index, String indexName, Sentence sent) {
		String wf= "";
		if (index <= 0) {
			wf = "null";
		}
		else if (index > sent.getSentArray().length) {
			wf = "null";
		}
		else {
			if ((index-1)==0) {
				wf="null";
			}
			else{
				wf = sent.getSentArray()[index-1][1];
				StringBuilder shape = new StringBuilder(wf);
				//System.out.println("Before shaping: " + shape);
				for (int i = 0; i < wf.length(); i++){
					if (Character.isUpperCase(wf.charAt(i))) shape.setCharAt(i, 'X'); 
					else
						if (Character.isLowerCase(wf.charAt(i))) shape.setCharAt(i, 'x');
				}

				//System.out.println("After shaping: " + shape);
				wf = shape.toString();
			}
		}		
		Feature f = new Feature(indexName, wf);
		return f;
	}

	// return the dependency label of the j-i element of current element j
	// NOTE: I am assuming CHAINS of modifier elements
	// So, I am returning just the label of the index-i element, if its head node is index
	
	// HURTS !!!
	public Feature templateModLabel(int index, String indexName, Sentence sent, int i) {
		String depRel;
		int ithPos = 0;
		if (index < 0) {
			depRel = "null";
		}
		else if (index == 0) {
			depRel = "none";
		}
		else {
			ithPos = index-i-1;
//			System.out.println("J: " + index + " I: " + i + " ithPos: "+ (ithPos+1));
//			System.out.println(sent.toString());

			depRel = ((ithPos >= 0) 
					//&& (Integer.parseInt(sent.getSentArray()[ithPos][6]) == index)
					) ?
					sent.getSentArray()[ithPos][7] : "null";

		}

		Feature f = new Feature(indexName, depRel);
		return f;
	}
}
