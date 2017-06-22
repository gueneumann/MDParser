package de.dfki.lt.mdparser.features;

import java.util.List;

import de.dfki.lt.mdparser.algorithm.ParserState;
import de.dfki.lt.mdparser.algorithm.StackParserState;
import de.dfki.lt.mdparser.data.Sentence;

public class StackFeatureModel extends FeatureModel {

  private String[] featureNamesForMerging;


  public StackFeatureModel(Alphabet parserAlphabet, Alphabet labelerAlphabet) {

    super(parserAlphabet, labelerAlphabet);
  }


  public StackFeatureModel(Alphabet parserAlphabet) {

    super(parserAlphabet);
    this.featureNamesForMerging = new String[5];
    for (int i = 0; i < this.featureNamesForMerging.length; i++) {
      this.featureNamesForMerging[i] = "m" + i;
    }
  }


  @Override
  public FeatureVector applyCombined(ParserState state, boolean train, boolean noLabels) {

    StackParserState st = (StackParserState)state;
    Sentence curSent = st.getSent();
    FeatureVector featureVector = new FeatureVector();
    Alphabet parserAlpha = this.getParserAlphabet();
    int st1 = st.getStackToken(0);
    int st2 = st.getStackToken(1);
    int buf1 = st.getBufferToken(0);
    int buf2 = st.getBufferToken(1);
    int buf3 = st.getBufferToken(2);
    int buf4 = st.getBufferToken(3);
    Feature fpj = FeatureExtractor.createFeatureForPos(st1, "pj", curSent);
    Integer fIndex = parserAlpha.getFeatureIndex(fpj.getFeatureString());
    fpj.setParserIndex(fIndex);
    featureVector.addFeature(fpj, parserAlpha, train);
    Feature fpjp1 = FeatureExtractor.createFeatureForPos(st2, "pjp1", curSent);
    fIndex = parserAlpha.getFeatureIndex(fpjp1.getFeatureString());
    fpjp1.setParserIndex(fIndex);
    featureVector.addFeature(fpjp1, parserAlpha, train);
    Feature fwfj = FeatureExtractor.createFeatureForWF(st1, "wfj", curSent);
    fIndex = parserAlpha.getFeatureIndex(fwfj.getFeatureString());
    fwfj.setParserIndex(fIndex);
    featureVector.addFeature(fwfj, parserAlpha, train);
    Feature fcpj = FeatureExtractor.createFeatureForCPos(st1, "cpj", curSent);
    fIndex = parserAlpha.getFeatureIndex(fcpj.getFeatureString());
    fcpj.setParserIndex(fIndex);
    featureVector.addFeature(fcpj, parserAlpha, train);
    Feature fpi = FeatureExtractor.createFeatureForPos(buf1, "pi", curSent);
    fIndex = parserAlpha.getFeatureIndex(fpi.getFeatureString());
    fpi.setParserIndex(fIndex);
    featureVector.addFeature(fpi, parserAlpha, train);
    Feature fpip1 = FeatureExtractor.createFeatureForPos(buf2, "pip1", curSent);
    fIndex = parserAlpha.getFeatureIndex(fpip1.getFeatureString());
    fpip1.setParserIndex(fIndex);
    featureVector.addFeature(fpip1, parserAlpha, train);
    Feature fpip2 = FeatureExtractor.createFeatureForPos(buf3, "pip2", curSent);
    fIndex = parserAlpha.getFeatureIndex(fpip2.getFeatureString());
    fpip2.setParserIndex(fIndex);
    featureVector.addFeature(fpip2, parserAlpha, train);
    Feature fpip3 = FeatureExtractor.createFeatureForPos(buf4, "pip3", curSent);
    fIndex = parserAlpha.getFeatureIndex(fpip3.getFeatureString());
    fpip3.setParserIndex(fIndex);
    featureVector.addFeature(fpip3, parserAlpha, train);
    Feature fwfi = FeatureExtractor.createFeatureForWF(buf1, "wfi", curSent);
    fIndex = parserAlpha.getFeatureIndex(fwfi.getFeatureString());
    fwfi.setParserIndex(fIndex);
    featureVector.addFeature(fwfi, parserAlpha, train);
    Feature wfip1 = FeatureExtractor.createFeatureForWF(buf2, "wfip1", curSent);
    fIndex = parserAlpha.getFeatureIndex(wfip1.getFeatureString());
    wfip1.setParserIndex(fIndex);
    featureVector.addFeature(wfip1, parserAlpha, train);
    Feature fcpi = FeatureExtractor.createFeatureForCPos(buf1, "cpi", curSent);
    fIndex = parserAlpha.getFeatureIndex(fcpi.getFeatureString());
    fcpi.setParserIndex(fIndex);
    featureVector.addFeature(fcpi, parserAlpha, train);
    int hj = st.getCurDepStruct().getHeads()[st1];
    Feature wfhj = FeatureExtractor.createFeatureForWF(hj, "wfhj", curSent);
    featureVector.addFeature(wfhj, parserAlpha, train);
    Feature fpi_fpj = FeatureExtractor.mergeFeatures(0, this.featureNamesForMerging, fpi, fpj);
    fIndex = parserAlpha.getFeatureIndex(fpi_fpj.getFeatureString());
    fpi_fpj.setParserIndex(fIndex);
    featureVector.addFeature(fpi_fpj, parserAlpha, train);
    Feature fpjp2_fpjp1_fpi = FeatureExtractor.mergeFeatures(1, this.featureNamesForMerging, fpjp1, fpj, fpi);
    fIndex = parserAlpha.getFeatureIndex(fpjp2_fpjp1_fpi.getFeatureString());
    fpjp2_fpjp1_fpi.setParserIndex(fIndex);
    featureVector.addFeature(fpjp2_fpjp1_fpi, parserAlpha, train);
    Feature fpj_fpi1_fpip1 = FeatureExtractor.mergeFeatures(2, this.featureNamesForMerging, fpj, fpi, fpip1);
    fIndex = parserAlpha.getFeatureIndex(fpj_fpi1_fpip1.getFeatureString());
    fpj_fpi1_fpip1.setParserIndex(fIndex);
    featureVector.addFeature(fpj_fpi1_fpip1, parserAlpha, train);
    Feature fpi_fpip1_fpip2 = FeatureExtractor.mergeFeatures(3, this.featureNamesForMerging, fpi, fpip1, fpip2);
    fIndex = parserAlpha.getFeatureIndex(fpi_fpip1_fpip2.getFeatureString());
    fpi_fpip1_fpip2.setParserIndex(fIndex);
    featureVector.addFeature(fpi_fpip1_fpip2, parserAlpha, train);
    Feature fpip1_fpip2_fpip3 = FeatureExtractor.mergeFeatures(4, this.featureNamesForMerging, fpip1, fpip2, fpip3);
    fIndex = parserAlpha.getFeatureIndex(fpip1_fpip2_fpip3.getFeatureString());
    fpip1_fpip2_fpip3.setParserIndex(fIndex);
    featureVector.addFeature(fpip1_fpip2_fpip3, parserAlpha, train);
    Feature dist = FeatureExtractor.createFeatureForDistance(st1, buf1);
    featureVector.addFeature(dist, parserAlpha, train);
    //Feature deprelj = FeatureExtractor.createFeatureForDepRel(st1, "depj", curSent, train);
    //featureVector.addFeature(deprelj, parserAlpha, train);
    return featureVector;
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
