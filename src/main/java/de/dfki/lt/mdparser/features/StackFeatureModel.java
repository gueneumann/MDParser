package de.dfki.lt.mdparser.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.dfki.lt.mdparser.algorithm.CovingtonParserState;
import de.dfki.lt.mdparser.algorithm.ParserState;
import de.dfki.lt.mdparser.algorithm.StackParserState;
import de.dfki.lt.mdparser.data.Sentence;

public class StackFeatureModel extends FeatureModel {

	private Feature[][][] staticFeatures;
	private String[] mergeFeatureNames;

	public StackFeatureModel(Alphabet alphabetParser, Alphabet alphabetLabeler,
			FeatureExtractor fe) {
		super(alphabetParser, alphabetLabeler, fe);
		
	}

	public StackFeatureModel(Alphabet alphabetParser,	FeatureExtractor fe) {
		super(alphabetParser, fe);
		mergeFeatureNames = new String[5];
		for (int i=0; i < mergeFeatureNames.length;i++) {
			mergeFeatureNames[i] = "m"+i;
		}
		
	}
	
	@Override
	public List<FeatureVector> apply(ParserState state, boolean train,
			boolean noLabels) {
		List<FeatureVector> fvList = new ArrayList<FeatureVector>(2);
		StackParserState st = (StackParserState) state;
		Sentence curSent = st.getSent();
		FeatureVector fvParser = new FeatureVector(train);	
		FeatureVector fvLabeler = new FeatureVector(train);	
		FeatureExtractor fe = this.getFeatureExtractor();
		Alphabet alphaParser = this.getAlphabetParser();
		Alphabet alphaLabeler = this.getAlphabetLabeler();
		int st1 = st.getStackToken(0);
		int st2 = st.getStackToken(1);
		int buf1 = st.getBufferToken(0);
		int buf2 = st.getBufferToken(1);
		int buf3 = st.getBufferToken(2);
		int buf4 = st.getBufferToken(3);		
		Feature fpj = fe.templatePos(st1, "pj", curSent);
		Integer fIndex = alphaParser.getFeatureIndex(fpj.getFeatureString());
		fpj.setIndexParser(fIndex);
		Integer fIndex2 = alphaLabeler.getFeatureIndex(fpj.getFeatureString());
		fpj.setIndexLabeler(fIndex2);
		fvParser.addFeature(fpj, alphaParser, train);
		fvLabeler.addFeature(fpj, alphaLabeler, train);
		Feature fpjp1 = fe.templatePos(st2, "pjp1", curSent);
		fIndex = alphaParser.getFeatureIndex(fpjp1.getFeatureString());
		fpjp1.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fpjp1.getFeatureString());
		fpjp1.setIndexLabeler(fIndex2);
		fvParser.addFeature(fpjp1, alphaParser, train);
		fvLabeler.addFeature(fpjp1, alphaLabeler, train);
		Feature fwfj = fe.templateWf(st1, "wfj",curSent);
		fIndex = alphaParser.getFeatureIndex(fwfj.getFeatureString());
		fwfj.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fwfj.getFeatureString());
		fwfj.setIndexLabeler(fIndex2);
		fvParser.addFeature(fwfj, alphaParser, train);
		fvLabeler.addFeature(fwfj, alphaLabeler, train);
		Feature fcpj = fe.templateCPos(st1, "cpj", curSent);
		fIndex = alphaParser.getFeatureIndex(fcpj.getFeatureString());
		fcpj.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fcpj.getFeatureString());
		fcpj.setIndexLabeler(fIndex2);
		fvParser.addFeature(fcpj, alphaParser, train);
		fvLabeler.addFeature(fcpj, alphaLabeler, train);
		Feature fpi = fe.templatePos(buf1, "pi", curSent);
		fIndex = alphaParser.getFeatureIndex(fpi.getFeatureString());
		fpi.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fpi.getFeatureString());
		fpi.setIndexLabeler(fIndex2);
		fvParser.addFeature(fpi, alphaParser, train);
		fvLabeler.addFeature(fpi, alphaLabeler, train);
		Feature fpip1 = fe.templatePos(buf2, "pip1", curSent);
		fIndex = alphaParser.getFeatureIndex(fpip1.getFeatureString());
		fpip1.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fpip1.getFeatureString());
		fpip1.setIndexLabeler(fIndex2);
		fvParser.addFeature(fpip1, alphaParser, train);
		fvLabeler.addFeature(fpip1, alphaLabeler, train);
		Feature fpip2 = fe.templatePos(buf3, "pip2", curSent);
		fIndex = alphaParser.getFeatureIndex(fpip2.getFeatureString());
		fpip2.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fpip2.getFeatureString());
		fpip2.setIndexLabeler(fIndex2);
		fvParser.addFeature(fpip2, alphaParser, train);
		fvLabeler.addFeature(fpip2, alphaLabeler, train);
		Feature fpip3 = fe.templatePos(buf4, "pip3", curSent);
		fIndex = alphaParser.getFeatureIndex(fpip3.getFeatureString());
		fpip3.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fpip3.getFeatureString());
		fpip3.setIndexLabeler(fIndex2);
		fvParser.addFeature(fpip3, alphaParser, train);
		fvLabeler.addFeature(fpip3, alphaLabeler, train);	
		Feature fwfi = fe.templateWf(buf1, "wfi",curSent);
		fIndex = alphaParser.getFeatureIndex(fwfi.getFeatureString());
		fwfi.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fwfi.getFeatureString());
		fwfi.setIndexLabeler(fIndex2);
		fvParser.addFeature(fwfi, alphaParser, train);
		fvLabeler.addFeature(fwfi, alphaLabeler, train);
		Feature wfip1 = fe.templateWf(buf2, "wfip1", curSent);
		fIndex = alphaParser.getFeatureIndex(wfip1.getFeatureString());
		wfip1.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(wfip1.getFeatureString());
		wfip1.setIndexLabeler(fIndex2);
		fvParser.addFeature(wfip1, alphaParser, train);
		fvLabeler.addFeature(wfip1, alphaLabeler, train);
		Feature fcpi = fe.templateCPos(buf1, "cpi", curSent);
		fIndex = alphaParser.getFeatureIndex(fcpi.getFeatureString());
		fcpi.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fcpi.getFeatureString());
		fcpi.setIndexLabeler(fIndex2);
		fvParser.addFeature(fcpi, alphaParser, train);
		fvLabeler.addFeature(fcpi, alphaLabeler, train);
		int hj = st.getCurDepStruct().getHeads()[st1];
		Feature wfhj = fe.templateWf(hj, "wfhj", curSent);
		fvParser.addFeature(wfhj, alphaParser, train);
		fvLabeler.addFeature(wfhj, alphaLabeler, train);
		Feature fpi_fpj = fe.merge2(0,mergeFeatureNames,fpi,fpj);
		fIndex = alphaParser.getFeatureIndex(fpi_fpj.getFeatureString());
		fpi_fpj.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fpi_fpj.getFeatureString());
		fpi_fpj.setIndexLabeler(fIndex2);
		fvParser.addFeature(fpi_fpj, alphaParser, train);
		fvLabeler.addFeature(fpi_fpj, alphaLabeler, train);
		Feature fpjp2_fpjp1_fpi = fe.merge3(1,mergeFeatureNames,fpjp1,fpj,fpi);
		fIndex = alphaParser.getFeatureIndex(fpjp2_fpjp1_fpi.getFeatureString());
		fpjp2_fpjp1_fpi.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fpjp2_fpjp1_fpi.getFeatureString());
		fpjp2_fpjp1_fpi.setIndexLabeler(fIndex2);
		fvParser.addFeature(fpjp2_fpjp1_fpi, alphaParser, train);
		fvLabeler.addFeature(fpjp2_fpjp1_fpi, alphaLabeler, train);
		Feature fpj_fpi1_fpip1 = fe.merge3(2,mergeFeatureNames,fpj,fpi,fpip1);
		fIndex = alphaParser.getFeatureIndex(fpj_fpi1_fpip1.getFeatureString());
		fpj_fpi1_fpip1.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fpj_fpi1_fpip1.getFeatureString());
		fpj_fpi1_fpip1.setIndexLabeler(fIndex2);
		fvParser.addFeature(fpj_fpi1_fpip1, alphaParser, train);
		fvLabeler.addFeature(fpj_fpi1_fpip1, alphaLabeler, train);
		Feature fpi_fpip1_fpip2 = fe.merge3(3,mergeFeatureNames,fpi,fpip1,fpip2);
		fIndex = alphaParser.getFeatureIndex(fpi_fpip1_fpip2.getFeatureString());
		fpi_fpip1_fpip2.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fpi_fpip1_fpip2.getFeatureString());
		fpi_fpip1_fpip2.setIndexLabeler(fIndex2);
		fvParser.addFeature(fpi_fpip1_fpip2, alphaParser, train);
		fvLabeler.addFeature(fpi_fpip1_fpip2, alphaLabeler, train);
		Feature fpip1_fpip2_fpip3 = fe.merge3(4,mergeFeatureNames,fpip1,fpip2,fpip3);
		fIndex = alphaParser.getFeatureIndex(fpip1_fpip2_fpip3.getFeatureString());
		fpip1_fpip2_fpip3.setIndexParser(fIndex);
		fIndex2 = alphaLabeler.getFeatureIndex(fpip1_fpip2_fpip3.getFeatureString());
		fpip1_fpip2_fpip3.setIndexLabeler(fIndex2);
		fvParser.addFeature(fpip1_fpip2_fpip3, alphaParser, train);
		fvLabeler.addFeature(fpip1_fpip2_fpip3, alphaLabeler, train);
		Feature dist = fe.templateDistance(st1,buf1);
		fvParser.addFeature(dist, alphaParser, train);
		fvLabeler.addFeature(dist, alphaLabeler, train);
		Feature deprelj = fe.templateDepRel(st1, "depj", curSent, train);
		fvParser.addFeature(deprelj, alphaParser, train);
		fvLabeler.addFeature(deprelj, alphaLabeler, train);
		fvList.add(fvParser);
		fvList.add(fvLabeler);
		return fvList;
	}
	
	public void initializeStaticFeaturesCombined(Sentence curSent, boolean train) {
		staticFeatures = new Feature[2][curSent.getSentArray().length+1][];
		String[][] curSentArray = curSent.getSentArray();
		FeatureExtractor fe = this.getFeatureExtractor();
		Alphabet alphaParser = this.getAlphabetParser();
		for (int j=0; j < curSentArray.length+1;j++) {
				staticFeatures[0][j] = new Feature[8];
				Feature fpj = fe.templatePos(j, "pj", curSent);
				Integer fIndex = alphaParser.getFeatureIndex(fpj.getFeatureString());
				fpj.setIndexParser(fIndex);
				staticFeatures[0][j][0] = fpj;
				Feature fpjp1 = fe.templatePos(j+1, "pjp1", curSent);
				fIndex = alphaParser.getFeatureIndex(fpjp1.getFeatureString());
				fpjp1.setIndexParser(fIndex);
				staticFeatures[0][j][1] = fpjp1;
				Feature fpjp2 = fe.templatePos(j+2, "pjp2", curSent);
				fIndex = alphaParser.getFeatureIndex(fpjp2.getFeatureString());
				fpjp2.setIndexParser(fIndex);
				staticFeatures[0][j][2] = fpjp2;
				Feature fpjp3 = fe.templatePos(j+3, "pjp3", curSent);
				fIndex = alphaParser.getFeatureIndex(fpjp3.getFeatureString());
				fpjp3.setIndexParser(fIndex);
				staticFeatures[0][j][3] = fpjp3;
				Feature fwfj = fe.templateWf(j, "wfj",curSent);
				fIndex = alphaParser.getFeatureIndex(fwfj.getFeatureString());
				fwfj.setIndexParser(fIndex);
				staticFeatures[0][j][4] = fwfj;
				Feature fcpj = fe.templateCPos(j, "cpj", curSent);
				fIndex = alphaParser.getFeatureIndex(fcpj.getFeatureString());
				fcpj.setIndexParser(fIndex);
				staticFeatures[0][j][5] = fcpj;
				Feature wfjp1 = fe.templateWf(j+1, "wfjp1", curSent);
				staticFeatures[0][j][6] = wfjp1;
				fIndex = alphaParser.getFeatureIndex(wfjp1.getFeatureString());
				wfjp1.setIndexParser(fIndex);
				Feature fpjp1_fpjp2_fpjp3 = fe.merge3(0,mergeFeatureNames,fpjp1,fpjp2,fpjp3);
				fIndex = alphaParser.getFeatureIndex(fpjp1_fpjp2_fpjp3.getFeatureString());
				fpjp1_fpjp2_fpjp3.setIndexParser(fIndex);;
				staticFeatures[0][j][7] = fpjp1_fpjp2_fpjp3;	
		}
		for (int i=0; i < curSentArray.length+1;i++) {
			staticFeatures[1][i] = new Feature[6];
			Feature fpi = fe.templatePos(i, "pi", curSent);
			Integer fIndex = alphaParser.getFeatureIndex(fpi.getFeatureString());
			fpi.setIndexParser(fIndex);
			staticFeatures[1][i][0] = fpi;
			Feature fip1 = fe.templatePos(i+1, "pip1", curSent);
			fIndex = alphaParser.getFeatureIndex(fip1.getFeatureString());
			fip1.setIndexParser(fIndex);
			staticFeatures[1][i][1] = fip1;
			Feature fwfi = fe.templateWf(i, "wfi",curSent);
			fIndex = alphaParser.getFeatureIndex(fwfi.getFeatureString());
			fwfi.setIndexParser(fIndex);
			staticFeatures[1][i][2] = fwfi;
			Feature fcpi = fe.templateCPos(i, "cpi", curSent);
			fIndex = alphaParser.getFeatureIndex(fcpi.getFeatureString());
			fcpi.setIndexParser(fIndex);
			staticFeatures[1][i][3] = fcpi;
			Feature fpi_fpip1 = fe.merge2(1,mergeFeatureNames,fpi,fip1);
			fIndex = alphaParser.getFeatureIndex(fpi_fpip1.getFeatureString());
			fpi_fpip1.setIndexParser(fIndex);
			staticFeatures[1][i][4] = fpi_fpip1;	
			Feature fwfi_fcpi = fe.merge2(2,mergeFeatureNames,fwfi,fcpi);
			fIndex = alphaParser.getFeatureIndex(fwfi_fcpi.getFeatureString());
			fwfi_fcpi.setIndexParser(fIndex);
			staticFeatures[1][i][5] = fwfi_fcpi;	
		}

	}
	

	@Override
	public FeatureVector applyCombined(ParserState state, boolean train, boolean noLabels) {
		StackParserState st = (StackParserState) state;
		Sentence curSent = st.getSent();
		FeatureVector fvParser = new FeatureVector(train);	
		FeatureExtractor fe = this.getFeatureExtractor();
		Alphabet alphaParser = this.getAlphabetParser();
		Alphabet alphaLabeler = this.getAlphabetLabeler();
		int st1 = st.getStackToken(0);
		int st2 = st.getStackToken(1);
		int buf1 = st.getBufferToken(0);
		int buf2 = st.getBufferToken(1);
		int buf3 = st.getBufferToken(2);
		int buf4 = st.getBufferToken(3);		
		Feature fpj = fe.templatePos(st1, "pj", curSent);
		Integer fIndex = alphaParser.getFeatureIndex(fpj.getFeatureString());
		fpj.setIndexParser(fIndex);
		fvParser.addFeature(fpj, alphaParser, train);
		Feature fpjp1 = fe.templatePos(st2, "pjp1", curSent);
		fIndex = alphaParser.getFeatureIndex(fpjp1.getFeatureString());
		fpjp1.setIndexParser(fIndex);
		fvParser.addFeature(fpjp1, alphaParser, train);
		Feature fwfj = fe.templateWf(st1, "wfj",curSent);
		fIndex = alphaParser.getFeatureIndex(fwfj.getFeatureString());
		fwfj.setIndexParser(fIndex);
		fvParser.addFeature(fwfj, alphaParser, train);
		Feature fcpj = fe.templateCPos(st1, "cpj", curSent);
		fIndex = alphaParser.getFeatureIndex(fcpj.getFeatureString());
		fcpj.setIndexParser(fIndex);
		fvParser.addFeature(fcpj, alphaParser, train);
		Feature fpi = fe.templatePos(buf1, "pi", curSent);
		fIndex = alphaParser.getFeatureIndex(fpi.getFeatureString());
		fpi.setIndexParser(fIndex);
		fvParser.addFeature(fpi, alphaParser, train);
		Feature fpip1 = fe.templatePos(buf2, "pip1", curSent);
		fIndex = alphaParser.getFeatureIndex(fpip1.getFeatureString());
		fpip1.setIndexParser(fIndex);
		fvParser.addFeature(fpip1, alphaParser, train);
		Feature fpip2 = fe.templatePos(buf3, "pip2", curSent);
		fIndex = alphaParser.getFeatureIndex(fpip2.getFeatureString());
		fpip2.setIndexParser(fIndex);
		fvParser.addFeature(fpip2, alphaParser, train);
		Feature fpip3 = fe.templatePos(buf4, "pip3", curSent);
		fIndex = alphaParser.getFeatureIndex(fpip3.getFeatureString());
		fpip3.setIndexParser(fIndex);
		fvParser.addFeature(fpip3, alphaParser, train);
		Feature fwfi = fe.templateWf(buf1, "wfi",curSent);
		fIndex = alphaParser.getFeatureIndex(fwfi.getFeatureString());
		fwfi.setIndexParser(fIndex);
		fvParser.addFeature(fwfi, alphaParser, train);
		Feature wfip1 = fe.templateWf(buf2, "wfip1", curSent);
		fIndex = alphaParser.getFeatureIndex(wfip1.getFeatureString());
		wfip1.setIndexParser(fIndex);
		fvParser.addFeature(wfip1, alphaParser, train);
		Feature fcpi = fe.templateCPos(buf1, "cpi", curSent);
		fIndex = alphaParser.getFeatureIndex(fcpi.getFeatureString());
		fcpi.setIndexParser(fIndex);
		fvParser.addFeature(fcpi, alphaParser, train);
		int hj = st.getCurDepStruct().getHeads()[st1];
		Feature wfhj = fe.templateWf(hj, "wfhj", curSent);
		fvParser.addFeature(wfhj, alphaParser, train);
		Feature fpi_fpj = fe.merge2(0,mergeFeatureNames,fpi,fpj);
		fIndex = alphaParser.getFeatureIndex(fpi_fpj.getFeatureString());
		fpi_fpj.setIndexParser(fIndex);
		fvParser.addFeature(fpi_fpj, alphaParser, train);
		Feature fpjp2_fpjp1_fpi = fe.merge3(1,mergeFeatureNames,fpjp1,fpj,fpi);
		fIndex = alphaParser.getFeatureIndex(fpjp2_fpjp1_fpi.getFeatureString());
		fpjp2_fpjp1_fpi.setIndexParser(fIndex);
		fvParser.addFeature(fpjp2_fpjp1_fpi, alphaParser, train);
		Feature fpj_fpi1_fpip1 = fe.merge3(2,mergeFeatureNames,fpj,fpi,fpip1);
		fIndex = alphaParser.getFeatureIndex(fpj_fpi1_fpip1.getFeatureString());
		fpj_fpi1_fpip1.setIndexParser(fIndex);
		fvParser.addFeature(fpj_fpi1_fpip1, alphaParser, train);
		Feature fpi_fpip1_fpip2 = fe.merge3(3,mergeFeatureNames,fpi,fpip1,fpip2);
		fIndex = alphaParser.getFeatureIndex(fpi_fpip1_fpip2.getFeatureString());
		fpi_fpip1_fpip2.setIndexParser(fIndex);
		fvParser.addFeature(fpi_fpip1_fpip2, alphaParser, train);
		Feature fpip1_fpip2_fpip3 = fe.merge3(4,mergeFeatureNames,fpip1,fpip2,fpip3);
		fIndex = alphaParser.getFeatureIndex(fpip1_fpip2_fpip3.getFeatureString());
		fpip1_fpip2_fpip3.setIndexParser(fIndex);
		fvParser.addFeature(fpip1_fpip2_fpip3, alphaParser, train);
		Feature dist = fe.templateDistance(st1,buf1);
		fvParser.addFeature(dist, alphaParser, train);
	//	Feature deprelj = fe.templateDepRel(st1, "depj", curSent, train);
	//	fvParser.addFeature(deprelj, alphaParser, train);
		return fvParser;
	}

}
