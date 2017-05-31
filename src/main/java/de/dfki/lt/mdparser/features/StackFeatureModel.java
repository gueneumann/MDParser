package de.dfki.lt.mdparser.features;

import java.util.List;

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


  public StackFeatureModel(Alphabet alphabetParser, FeatureExtractor fe) {
    super(alphabetParser, fe);
    this.mergeFeatureNames = new String[5];
    for (int i = 0; i < this.mergeFeatureNames.length; i++) {
      this.mergeFeatureNames[i] = "m" + i;
    }

  }


  @Override
  public FeatureVector applyCombined(ParserState state, boolean train, boolean noLabels) {

    StackParserState st = (StackParserState)state;
    Sentence curSent = st.getSent();
    FeatureVector fvParser = new FeatureVector(train);
    FeatureExtractor fe = this.getFeatureExtractor();
    Alphabet alphaParser = this.getAlphabetParser();
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
    Feature fwfj = fe.templateWf(st1, "wfj", curSent);
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
    Feature fwfi = fe.templateWf(buf1, "wfi", curSent);
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
    Feature fpi_fpj = fe.merge2(0, this.mergeFeatureNames, fpi, fpj);
    fIndex = alphaParser.getFeatureIndex(fpi_fpj.getFeatureString());
    fpi_fpj.setIndexParser(fIndex);
    fvParser.addFeature(fpi_fpj, alphaParser, train);
    Feature fpjp2_fpjp1_fpi = fe.merge3(1, this.mergeFeatureNames, fpjp1, fpj, fpi);
    fIndex = alphaParser.getFeatureIndex(fpjp2_fpjp1_fpi.getFeatureString());
    fpjp2_fpjp1_fpi.setIndexParser(fIndex);
    fvParser.addFeature(fpjp2_fpjp1_fpi, alphaParser, train);
    Feature fpj_fpi1_fpip1 = fe.merge3(2, this.mergeFeatureNames, fpj, fpi, fpip1);
    fIndex = alphaParser.getFeatureIndex(fpj_fpi1_fpip1.getFeatureString());
    fpj_fpi1_fpip1.setIndexParser(fIndex);
    fvParser.addFeature(fpj_fpi1_fpip1, alphaParser, train);
    Feature fpi_fpip1_fpip2 = fe.merge3(3, this.mergeFeatureNames, fpi, fpip1, fpip2);
    fIndex = alphaParser.getFeatureIndex(fpi_fpip1_fpip2.getFeatureString());
    fpi_fpip1_fpip2.setIndexParser(fIndex);
    fvParser.addFeature(fpi_fpip1_fpip2, alphaParser, train);
    Feature fpip1_fpip2_fpip3 = fe.merge3(4, this.mergeFeatureNames, fpip1, fpip2, fpip3);
    fIndex = alphaParser.getFeatureIndex(fpip1_fpip2_fpip3.getFeatureString());
    fpip1_fpip2_fpip3.setIndexParser(fIndex);
    fvParser.addFeature(fpip1_fpip2_fpip3, alphaParser, train);
    Feature dist = fe.templateDistance(st1, buf1);
    fvParser.addFeature(dist, alphaParser, train);
    //Feature deprelj = fe.templateDepRel(st1, "depj", curSent, train);
    //fvParser.addFeature(deprelj, alphaParser, train);
    return fvParser;
  }


  @Override
  public List<FeatureVector> apply(ParserState state, boolean train, boolean noLabels) {

    return null;
  }


  @Override
  public void initializeStaticFeaturesCombined(Sentence sent, boolean train) {
    // do nothing
  }
}
