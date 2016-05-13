package de.dfki.lt.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.dfki.lt.algorithm.CovingtonParserState;
import de.dfki.lt.algorithm.ParserState;
import de.dfki.lt.data.Sentence;

public class CovingtonFeatureModel extends FeatureModel {


	//private Feature[][][] staticFeatures;
	private String[] mergeFeatureNames;


	private void addStaticFeature(List<Feature> jList, Feature f, Alphabet alphaParser) {
		Integer fIndex = alphaParser.getFeatureIndex(f.getFeatureString());
		f.setIndexParser(fIndex);
		jList.add(f);

	}

	public CovingtonFeatureModel(Alphabet alphabetParser, Alphabet alphabetLabeler, FeatureExtractor fe) {
		super(alphabetParser, alphabetLabeler, fe);
	}

	public CovingtonFeatureModel(Alphabet alphabetParser, FeatureExtractor fe) {
		super(alphabetParser, fe);
		mergeFeatureNames = new String[11];
		for (int i=0; i < mergeFeatureNames.length;i++) {
			mergeFeatureNames[i] = "m"+i;
		}
	}



	//new multithreading
	// GN: The reason why j and i are considered is based on the parsing strategy:
	//     For each current word j, check whether there exists a word i to the left, 
	//     so that there is a dependency relation between i and j (either (j,i) or (i,j)
	//     NOTE:  we need j and i features for determining dependency relations later


	public void initializeStaticFeaturesCombinedOrig(Sentence curSent, boolean train) {
		Feature[][][] staticFeatures = new Feature[2][curSent.getSentArray().length+1][];
		String[][] curSentArray = curSent.getSentArray();
		FeatureExtractor fe = this.getFeatureExtractor();
		Alphabet alphaParser = this.getAlphabetParser();
		for (int j=1; j < curSentArray.length+1;j++) {
			List<Feature> jList = new ArrayList<Feature>();
			Feature fpj = fe.templatePos(j, "pj", curSent);
			addStaticFeature(jList,fpj,alphaParser);
			Feature fpjp1 = fe.templatePos(j+1, "pjp1", curSent);
			addStaticFeature(jList,fpjp1,alphaParser);
			Feature fpjp2 = fe.templatePos(j+2, "pjp2", curSent);
			addStaticFeature(jList,fpjp2,alphaParser);
			Feature fpjp3 = fe.templatePos(j+3, "pjp3", curSent);
			addStaticFeature(jList,fpjp3,alphaParser);
			Feature fwfj = fe.templateWf(j, "wfj",curSent);
			addStaticFeature(jList,fwfj,alphaParser);
			Feature fcpj = fe.templateCPos(j, "cpj", curSent);
			addStaticFeature(jList,fcpj,alphaParser);
			//	Feature fpj_fcpj = fe.merge2(9,mergeFeatureNames,fpj,fcpj);
			//	addStaticFeature(jList,fpj_fcpj, alphaParser);
			//	Feature fcase = fe.templateFeat(j, "casej", curSent);
			//	addStaticFeature(jList, fcase, alphaParser);
			//	Feature fullMerge = fe.merge2(10, mergeFeatureNames, fpj_fcpj,fcase);
			//	addStaticFeature(jList, fullMerge, alphaParser);
			Feature wfjp1 = fe.templateWf(j+1, "wfjp1", curSent);
			addStaticFeature(jList,wfjp1,alphaParser);
			Feature fpjp1_fpjp2_fpjp3 = fe.merge3(0,mergeFeatureNames,fpjp1,fpjp2,fpjp3);
			addStaticFeature(jList,fpjp1_fpjp2_fpjp3,alphaParser);	

			Feature fpjp4 = fe.templatePos(j+4, "pjp4", curSent);
			addStaticFeature(jList,fpjp4,alphaParser);
			staticFeatures[0][j] = new Feature[jList.size()];
			staticFeatures[0][j] = jList.toArray(staticFeatures[0][j]);
		}
		for (int i=0; i < curSentArray.length+1;i++) {
			List<Feature> iList = new ArrayList<Feature>();
			Feature fpi = fe.templatePos(i, "pi", curSent);
			addStaticFeature(iList, fpi, alphaParser);
			Feature fpip1 = fe.templatePos(i+1, "pip1", curSent);
			addStaticFeature(iList, fpip1, alphaParser);
			Feature fwfi = fe.templateWf(i, "wfi",curSent);
			addStaticFeature(iList, fwfi, alphaParser);
			Feature fcpi = fe.templateCPos(i, "cpi", curSent);
			addStaticFeature(iList, fcpi, alphaParser);
			Feature fpi_fpip1 = fe.merge2(1,mergeFeatureNames,fpi,fpip1);
			addStaticFeature(iList, fpi_fpip1, alphaParser);	
			Feature fwfi_fcpi = fe.merge2(2,mergeFeatureNames,fwfi,fcpi);
			addStaticFeature(iList, fwfi_fcpi, alphaParser);
			Feature fpip2 = fe.templatePos(i+2, "pip2", curSent);
			addStaticFeature(iList, fpip2, alphaParser);
			staticFeatures[1][i] = new Feature[iList.size()];
			staticFeatures[1][i] = iList.toArray(staticFeatures[1][i]);
		}
		curSent.setStaticFeatures(staticFeatures);

	}

	/*

1. 	wfj 	OK (used as static)
2. 	pj  	OK (used as static)
	cposj	EXTRA in Implementation, but basically the same as pj
	-> but harmless according to AV -> but I do not use it -> leads to lower result
3. 	wfjp1 	OK (used as static)
4. 	pjp1 	OK (used as static)
5. 	wfjp2 	NOT USED -> when added, it hurts
6. 	pjp2 	OK (used as static)
7. 	wfjp3 	NOT USED -> when added, it hurts
8. 	pjp3 	OK (used as static)
    pjp4	EXTRA in Implementation
9. 	wfi 	OK (used as static)
10. pi 		OK (used as static)
	cposi	EXTRA in Implementation, but basically the same as pi 
	-> but harmless according to AV -> but I do not use it
11. pip1 	OK (used as static)
	pip2	EXTRA in Implementation

12. wfhi 	OK (used as dynamic)
13. phi 	OK (used as dynamic)
14. depi 	OK (used as dynamic)
15. depldi 	OK (used as dynamic)
16. deprdi 	OK (used as dynamic)
17. depldj 	OK (used as dynamic)
18. dist 	OK (used as dynamic)

19. merge2(pi,pip1)				OK (used as static no. 0)
20. merge2(wfi,pi)				UNCLEAR (used as static no. 1) 
	-> is/was actually used with cposi, which is basically the same as pi, but unclear why is this defined
21. merge3(pjp1,pjp2,pjp3)		OK (used as static no. 2)

22. merge2(depldj,pj)			OK (used as dynamic no. 3; access to static features)
23. merge3(pi,deprdi,depldi)	OK (used as dynamic no. 4; access to static features)
24. merge2(depi,wfhi)			OK (used as dynamic no. 5)
25. merge3(phi,pjp1,pip1)		OK (used as dynamic no. 6; access to static features)
26. merge3(wfj,wfi,pjp3)		UNCLEAR (used as dynamic no. 7) -> corrected in code
27. merge3(dist,pj,wfjp1)		UNCLEAR (used as dynamic no. 8) -> corrected in code

	 */

	// GN: changed to be as close as possible to Thesis and by playing with 
	// NER specific spelling features.
	public void initializeStaticFeaturesCombined(Sentence curSent, boolean train) {
		// GN:   keeps i-features and j-features separately. What is third dimension used for ?
		Feature[][][] staticFeatures = new Feature[2][curSent.getSentArray().length+1][];
		String[][] curSentArray = curSent.getSentArray();
		FeatureExtractor fe = this.getFeatureExtractor();
		Alphabet alphaParser = this.getAlphabetParser();
		for (int j=1; j < curSentArray.length+1;j++) {
			List<Feature> jList = new ArrayList<Feature>();

			Feature fpj = fe.templatePos(j, "pj", curSent);
			addStaticFeature(jList,fpj,alphaParser);
			Feature fpjp1 = fe.templatePos(j+1, "pjp1", curSent);
			addStaticFeature(jList,fpjp1,alphaParser);
			Feature fpjp2 = fe.templatePos(j+2, "pjp2", curSent);
			addStaticFeature(jList,fpjp2,alphaParser);
			Feature fpjp3 = fe.templatePos(j+3, "pjp3", curSent);
			addStaticFeature(jList,fpjp3,alphaParser);

			Feature fwfj = fe.templateWf(j, "wfj",curSent);
			addStaticFeature(jList,fwfj,alphaParser);

			Feature wfjp1 = fe.templateWf(j+1, "wfjp1", curSent);
			addStaticFeature(jList,wfjp1,alphaParser);

			Feature fpjp1_fpjp2_fpjp3 = fe.merge3(0,mergeFeatureNames,fpjp1,fpjp2,fpjp3);
			addStaticFeature(jList,fpjp1_fpjp2_fpjp3,alphaParser);	

			Feature fpjp4 = fe.templatePos(j+4, "pjp4", curSent);
			addStaticFeature(jList,fpjp4,alphaParser);

			Feature fcpj = fe.templateCPos(j, "cpj", curSent);
			addStaticFeature(jList,fcpj,alphaParser);

			staticFeatures[0][j] = new Feature[jList.size()];
			staticFeatures[0][j] = jList.toArray(staticFeatures[0][j]);
		}
		for (int i=0; i < curSentArray.length+1;i++) {
			List<Feature> iList = new ArrayList<Feature>();
			Feature fpi = fe.templatePos(i, "pi", curSent);
			addStaticFeature(iList, fpi, alphaParser);
			Feature fpip1 = fe.templatePos(i+1, "pip1", curSent);
			addStaticFeature(iList, fpip1, alphaParser);
			Feature fwfi = fe.templateWf(i, "wfi",curSent);
			addStaticFeature(iList, fwfi, alphaParser);

			Feature fcpi = fe.templateCPos(i, "cpi", curSent);
			addStaticFeature(iList, fcpi, alphaParser);

			Feature fpi_fpip1 = fe.merge2(1,mergeFeatureNames,fpi,fpip1);
			addStaticFeature(iList, fpi_fpip1, alphaParser);	
			// changed fcpi to fpi as defined in thesis
			Feature fwfi_fcpi = fe.merge2(2,mergeFeatureNames,fwfi,fcpi);
			addStaticFeature(iList, fwfi_fcpi, alphaParser);

			Feature fpip2 = fe.templatePos(i+2, "pip2", curSent);
			addStaticFeature(iList, fpip2, alphaParser);

			staticFeatures[1][i] = new Feature[iList.size()];
			staticFeatures[1][i] = iList.toArray(staticFeatures[1][i]);
		}
		curSent.setStaticFeatures(staticFeatures);

	}

	// GN on JULY, 2014: 
	// Added special features used for parsing NER
	public void initializeStaticFeaturesCombinedMDNer(Sentence curSent, boolean train) {
		// GN:   keeps i-features and j-features separately. What is third dimension used for ?
		Feature[][][] staticFeatures = new Feature[2][curSent.getSentArray().length+1][];
		String[][] curSentArray = curSent.getSentArray();
		FeatureExtractor fe = this.getFeatureExtractor();
		Alphabet alphaParser = this.getAlphabetParser();
		for (int j=1; j < curSentArray.length+1;j++) {
			List<Feature> jList = new ArrayList<Feature>();

			Feature fpj = fe.templatePos(j, "pj", curSent);
			addStaticFeature(jList,fpj,alphaParser);
			Feature fpjp1 = fe.templatePos(j+1, "pjp1", curSent);
			addStaticFeature(jList,fpjp1,alphaParser);
			Feature fpjp2 = fe.templatePos(j+2, "pjp2", curSent);
			addStaticFeature(jList,fpjp2,alphaParser);
			Feature fpjp3 = fe.templatePos(j+3, "pjp3", curSent);
			addStaticFeature(jList,fpjp3,alphaParser);

			Feature fwfj = fe.templateWf(j, "wfj",curSent);
			addStaticFeature(jList,fwfj,alphaParser);

			Feature wfjp1 = fe.templateWf(j+1, "wfjp1", curSent);
			addStaticFeature(jList,wfjp1,alphaParser);

			Feature fpjp1_fpjp2_fpjp3 = fe.merge3(0,mergeFeatureNames,fpjp1,fpjp2,fpjp3);
			addStaticFeature(jList,fpjp1_fpjp2_fpjp3,alphaParser);	

			//			Feature fpjp4 = fe.templatePos(j+4, "pjp4", curSent);
			//			addStaticFeature(jList,fpjp4,alphaParser);

			//			Feature fcpj = fe.templateCPos(j, "cpj", curSent);
			//			addStaticFeature(jList,fcpj,alphaParser);

			// ************************* 
			// GN: July 2014, add static features for MDNer

			/*Feature gnFeat4 = fe.templateWFsuffix(j, "wfsuff4",curSent, 4);
			addStaticFeature(jList, gnFeat4, alphaParser);
			Feature gnFeat3 = fe.templateWFsuffix(j, "wfsuff3",curSent, 3);
			addStaticFeature(jList, gnFeat3, alphaParser);
			Feature gnFeat2 = fe.templateWFsuffix(j, "wfsuff2",curSent, 2);
			addStaticFeature(jList, gnFeat2, alphaParser);
			Feature gnFeat1 = fe.templateWFsuffix(j, "wfsuff1",curSent, 1);
			addStaticFeature(jList, gnFeat1, alphaParser);

			Feature gnPref4 = fe.templateWFprefix(j, "wfpref4",curSent, 4);
			addStaticFeature(jList, gnPref4, alphaParser);
			Feature gnPref3 = fe.templateWFprefix(j, "wfpref3",curSent, 3);
			addStaticFeature(jList, gnPref3, alphaParser);
			Feature gnPref2 = fe.templateWFprefix(j, "wfpref2",curSent, 2);
			addStaticFeature(jList, gnPref2, alphaParser);
			Feature gnPref1 = fe.templateWFprefix(j, "wfpref1",curSent, 1);
			addStaticFeature(jList, gnPref1, alphaParser);

//			Feature gnCaps = fe.templateWFcaps(j, "wfcap",curSent);
//			addStaticFeature(jList, gnCaps, alphaParser);

			Feature gnLeftFeat = fe.templateWFwindowLeft(j, "wfwinleft",curSent);
			addStaticFeature(jList, gnLeftFeat, alphaParser);

			Feature gnShape = fe.templateWFshape(j, "wfshapej",curSent);
			addStaticFeature(jList, gnShape, alphaParser);*/

			// ************************* GN END: added for MDNer

			staticFeatures[0][j] = new Feature[jList.size()];
			staticFeatures[0][j] = jList.toArray(staticFeatures[0][j]);
		}
		for (int i=0; i < curSentArray.length+1;i++) {
			List<Feature> iList = new ArrayList<Feature>();
			Feature fpi = fe.templatePos(i, "pi", curSent);
			addStaticFeature(iList, fpi, alphaParser);
			Feature fpip1 = fe.templatePos(i+1, "pip1", curSent);
			addStaticFeature(iList, fpip1, alphaParser);
			Feature fwfi = fe.templateWf(i, "wfi",curSent);
			addStaticFeature(iList, fwfi, alphaParser);

			//			Feature fcpi = fe.templateCPos(i, "cpi", curSent);
			//			addStaticFeature(iList, fcpi, alphaParser);

			Feature fpi_fpip1 = fe.merge2(1,mergeFeatureNames,fpi,fpip1);
			addStaticFeature(iList, fpi_fpip1, alphaParser);	
			// changed fcpi to fpi as defined in thesis
			Feature fwfi_fcpi = fe.merge2(2,mergeFeatureNames,fwfi,fpi);
			addStaticFeature(iList, fwfi_fcpi, alphaParser);

			//			Feature fpip2 = fe.templatePos(i+2, "pip2", curSent);
			//			addStaticFeature(iList, fpip2, alphaParser);

			staticFeatures[1][i] = new Feature[iList.size()];
			staticFeatures[1][i] = iList.toArray(staticFeatures[1][i]);
		}
		curSent.setStaticFeatures(staticFeatures);

	}

	@Override
	public FeatureVector applyCombined(ParserState state, boolean train, boolean noLabels) {

		CovingtonParserState st = (CovingtonParserState) state;
		Sentence curSent = st.getCurSent();
		int j = st.getJ();
		int i = st.getI();
		FeatureVector fvParser = new FeatureVector(train);	

		FeatureExtractor fe = this.getFeatureExtractor();
		Alphabet alphaParser = this.getAlphabetParser();
		Feature[][][] staticFeatures = curSent.getStaticFeatures(); //new	
		Feature[] curStaticFeaturesJ = staticFeatures[0][j];

		// GN: show contents of static features J
		// System.out.println("curStaticFeaturesJ: ");

		// GN: I read this as such as the static features are inserted dynamically for each new training example.
		// The advantage is that we do not have to build the feature-value strings again.
		// So, this is how static features are used in memorization manner.
		// Since, static features are stored in a vector, and later accessed by the dynamic templates
		// I wonder whether ordering is crucial -> yes, it is crucial.
		for (int k=0; k < curStaticFeaturesJ.length;k++) {
			Feature f = curStaticFeaturesJ[k];
			// System.out.print("feat[" + k +"]="+ f.toString() + "; ");
			fvParser.addFeature(f,alphaParser,train);

		}	
		// System.out.println();

		Feature[] curStaticFeaturesI = staticFeatures[1][i];
		for (int k=0; k < curStaticFeaturesI.length;k++) {
			Feature f = curStaticFeaturesI[k];
			// System.out.print("feat[" + k +"]="+ f.toString() + "; ");
			fvParser.addFeature(f,alphaParser,train);
		}			
		// System.out.println();

		Feature dist = fe.templateDistance(j,i);
		fvParser.addFeature(dist, alphaParser, train);

		int hi = st.getCurDepStruct().getHeads()[i];
		Feature wfhi = fe.templateWf(hi, "wfhi", curSent);
		fvParser.addFeature(wfhi, alphaParser, train);

		Feature fphi = fe.templatePos(hi, "phi", curSent);
		fvParser.addFeature(fphi, alphaParser, train);

		Feature fdepi = fe.templateDepRel(i,"depi",curSent,train);
		fvParser.addFeature(fdepi, alphaParser, train);

		int ldj = st.getCurDepStruct().getFarthestLeftDependent(j);
		Feature fdepldj = fe.templateDepRel(ldj, "depldj", curSent, train);
		fvParser.addFeature(fdepldj, alphaParser, train);

		int ldi = st.getCurDepStruct().getFarthestLeftDependent(i);
		Feature fdepldi = fe.templateDepRel(ldi, "depldi", curSent, train);
		fvParser.addFeature(fdepldi, alphaParser, train);		

		int rdi = st.getCurDepStruct().getFarthestRightDependent(i);
		Feature fdeprdi = fe.templateDepRel(rdi, "deprdi", curSent, train);
		fvParser.addFeature(fdeprdi, alphaParser, train);	

		// curStaticFeaturesJ[0]: should be pj -> OK
		Feature fdepri_fpj = fe.merge2(3,mergeFeatureNames,fdepldj, curStaticFeaturesJ[0]);
		fvParser.addFeature(fdepri_fpj, alphaParser, train);

		// curStaticFeaturesI[0]: should be pi -> OK
		Feature fpi_fdeprdi_fdepldi = fe.merge3(4,mergeFeatureNames,curStaticFeaturesI[0], fdeprdi, fdepldi);
		fvParser.addFeature(fpi_fdeprdi_fdepldi, alphaParser, train);

		Feature fdepi_wfhi = fe.merge2(5,mergeFeatureNames,fdepi,wfhi);
		fvParser.addFeature(fdepi_wfhi, alphaParser, train);

		// curStaticFeaturesJ/I[1]: should be pjp1 and pip1 -> OK
		Feature fphi_fpjp1_fpip1 = fe.merge3(6,mergeFeatureNames,fphi, curStaticFeaturesI[1], curStaticFeaturesJ[1]);
		fvParser.addFeature(fphi_fpjp1_fpip1, alphaParser, train);

		// curStaticFeaturesJ[4] wfj is , curStaticFeaturesI[2] is wfi, curStaticFeaturesJ[2] is pjp2
		// laut thesis muesste das sein: wfj,wfi,pjp3
		Feature fst2 = fe.merge3(7,mergeFeatureNames,curStaticFeaturesJ[4], curStaticFeaturesI[2], curStaticFeaturesJ[3]);
		fvParser.addFeature(fst2, alphaParser, train);

		// curStaticFeaturesJ[5] is cpos, curStaticFeaturesJ[6] is wfjp1
		// laut Thesis muesste es sein: pj und wfjp1, also 0 und 5
		Feature fst3 = fe.merge3(8,mergeFeatureNames,dist, curStaticFeaturesJ[0], curStaticFeaturesJ[5]);
		fvParser.addFeature(fst3, alphaParser, train);



		/*
			// ************************* GN: July 2014, added for MDNer


			// ************************* GN END: added for MDNer

		 */

		return fvParser;
	}

	// GN: only applied with StackAlgorithm
	public List<FeatureVector> apply(ParserState state, boolean train, boolean noLabels) {
		List<FeatureVector> fvList = new ArrayList<FeatureVector>(2);
		CovingtonParserState st = (CovingtonParserState) state;
		Sentence curSent = st.getCurSent();
		int j = st.getJ();
		int i = st.getI();
		FeatureVector fvParser = new FeatureVector(train);	
		FeatureVector fvLabeler = new FeatureVector(train);	
		FeatureExtractor fe = this.getFeatureExtractor();
		Alphabet alphaParser = this.getAlphabetParser();
		Alphabet alphaLabeler = this.getAlphabetLabeler();	
		Feature[][][] staticFeatures = curSent.getStaticFeatures(); // new
		Feature[] curStaticFeaturesJ = staticFeatures[0][j];
		for (int k=0; k < curStaticFeaturesJ.length;k++) {
			Feature f = curStaticFeaturesJ[k];
			fvParser.addFeature(f,alphaParser,train);
			fvLabeler.addFeature(f, alphaLabeler, train);
		}	

		Feature[] curStaticFeaturesI = staticFeatures[1][i];
		for (int k=0; k < curStaticFeaturesI.length;k++) {
			Feature f = curStaticFeaturesI[k];
			fvParser.addFeature(f,alphaParser,train);
			fvLabeler.addFeature(f, alphaLabeler, train);
		}		

		Feature dist = fe.templateDistance(j,i);
		fvParser.addFeature(dist, alphaParser, train);
		fvLabeler.addFeature(dist, alphaLabeler, train);		

		int hi = st.getCurDepStruct().getHeads()[i];
		Feature wfhi = fe.templateWf(hi, "wfhi", curSent);
		fvParser.addFeature(wfhi, alphaParser, train);
		fvLabeler.addFeature(wfhi, alphaLabeler, train);


		Feature fphi = fe.templatePos(hi, "phi", curSent);
		fvParser.addFeature(fphi, alphaParser, train);
		fvLabeler.addFeature(fphi, alphaLabeler, train);

		if (!noLabels) {
			Feature fdepi = fe.templateDepRel(i,"depi",curSent,train);
			fvParser.addFeature(fdepi,alphaParser,train);
			fvLabeler.addFeature(fdepi,alphaParser,train);
		}


		fvList.add(fvParser);
		fvList.add(fvLabeler);
		return fvList;
	}
}
