package de.dfki.lt.mdparser.pil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.dfki.lt.mdparser.algorithm.CovingtonParserState;
import de.dfki.lt.mdparser.algorithm.Dependency;
import de.dfki.lt.mdparser.algorithm.DependencyStructure;
import de.dfki.lt.mdparser.algorithm.ParsingAlgorithm;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.CovingtonFeatureModel;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.FeatureVector;

public class MorphAlgorithm {


	public List<MorphFeatureVector> processCombined(Sentence sent,
			FeatureModel fm, boolean noLabels) {
		List<MorphFeatureVector> fvParserList = new ArrayList<MorphFeatureVector>();
		String[][] sentArray = sent.getSentArray();
	//	double rootProbabilities[] = new double[sentArray.length];
		DependencyStructure curDepStruct = new DependencyStructure(sentArray.length);
		CovingtonFeatureModel fm2 = (CovingtonFeatureModel) fm;
		fm2.initializeStaticFeaturesCombined(sent,false);
		for (int j = 1; j < sentArray.length+1; j++) {
			for (int i = j-1; i >= 0; i--) {
				CovingtonParserState ps = new CovingtonParserState(j,i,sent,curDepStruct);
				ps.checkPermissibility();
				if (ps.isPermissible()) {
					FeatureVector fvParser = fm2.applyCombined(ps, true, noLabels);	
					String label = findOutCorrectLabelCombined(j, i, sentArray);
					String labelsString = sentArray[j-1][5];
					MorphFeatureVector mfv = new MorphFeatureVector(fvParser, labelsString);
				//	System.out.println(i+" "+j+" "+label+" "+fvParser.getFeature("depi")+" "+curDepStruct.getDependencies());
					String labelTrans = "";
					if (label.contains("#")) {
						labelTrans = label.split("#")[0];
					}
					if (labelTrans.equals("j") ) {								
							String depRel = sentArray[j-1][7];					
							sentArray[j-1][9] = depRel; 
							curDepStruct.addDependency(new Dependency(j,i,depRel));
						}
						else if	(labelTrans.equals("i")) {
							String depRel = sentArray[i-1][7];
							sentArray[i-1][9] = depRel;
							curDepStruct.addDependency(new Dependency(i,j,depRel));
						}
						else if (label.equals("terminate")) {							
							i = -1;
						}
						fvParser.setLabel(label);								
						fvParserList.add(mfv);										
				}
			}
			
		}
		return fvParserList;
	}


	public String findOutCorrectLabel(int j, int i, String[][] sentArray) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String findOutCorrectLabelCombined(int j, int i, String[][] sentArray) {
		// 1 - left arc
		// 2 - right arc
		// 3 - shift
		String label = "";
		if (Integer.valueOf(sentArray[j-1][6]) == i) {
			label = "j";
		/*	if (Integer.valueOf(sentArray[j-1][6]) == 0) {
				label = "root";
			}*/
			label+= "#"+sentArray[j-1][7];
			return label;
		}	
		else if (i == 0) {
			label = "shift";
			return label;
		}
		else if (Integer.valueOf(sentArray[i-1][6]) == j) {
			label = "i";
			label+= "#"+sentArray[i-1][7];
			return label;
		}
		else {
			label = "shift";
		}
		if (label.equals("shift")) {
			boolean terminate = true;
			if (Integer.valueOf(sentArray[j-1][6]) < i) {
				terminate = false;
			}
			for (int k=i; k > 1;k--) {
				if (Integer.valueOf(sentArray[k-1][6]) == j) {
					terminate = false;
				}
			}
			if (terminate) {
				label = "terminate";
			}
		}
		return label;
	}
	
}
