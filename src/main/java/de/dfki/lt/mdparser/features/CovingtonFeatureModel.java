package de.dfki.lt.mdparser.features;

import java.util.ArrayList;
import java.util.List;

import de.dfki.lt.mdparser.algorithm.CovingtonParserState;
import de.dfki.lt.mdparser.algorithm.ParserState;
import de.dfki.lt.mdparser.data.Sentence;

public class CovingtonFeatureModel extends FeatureModel {

  private String[] featureNamesForMerging;


  public CovingtonFeatureModel(Alphabet parserAlphabet) {

    super(parserAlphabet);
    // why 11?
    this.featureNamesForMerging = new String[11];
    for (int i = 0; i < this.featureNamesForMerging.length; i++) {
      this.featureNamesForMerging[i] = "m" + i;
    }
  }


  // new multithreading
  // GN: The reason why j and i are considered is based on the parsing strategy:
  //     For each current word j, check whether there exists a word i to the left,
  //     so that there is a dependency relation between i and j (either (j,i) or (i,j)
  //     NOTE:  we need j and i features for determining dependency relations later
  public void initializeStaticFeaturesCombinedOrig(Sentence curSent, boolean train) {

    Feature[][][] staticFeatures = new Feature[2][curSent.getSentArray().length + 1][];
    String[][] curSentArray = curSent.getSentArray();
    Alphabet parserAlpha = this.getParserAlphabet();
    for (int j = 1; j < curSentArray.length + 1; j++) {
      List<Feature> jList = new ArrayList<Feature>();
      Feature fpj = FeatureExtractor.createFeatureForPos(j, "pj", curSent);
      addStaticFeature(jList, fpj, parserAlpha);
      Feature fpjp1 = FeatureExtractor.createFeatureForPos(j + 1, "pjp1", curSent);
      addStaticFeature(jList, fpjp1, parserAlpha);
      Feature fpjp2 = FeatureExtractor.createFeatureForPos(j + 2, "pjp2", curSent);
      addStaticFeature(jList, fpjp2, parserAlpha);
      Feature fpjp3 = FeatureExtractor.createFeatureForPos(j + 3, "pjp3", curSent);
      addStaticFeature(jList, fpjp3, parserAlpha);
      Feature fwfj = FeatureExtractor.createFeatureForWF(j, "wfj", curSent);
      addStaticFeature(jList, fwfj, parserAlpha);
      Feature fcpj = FeatureExtractor.createFeatureForCPos(j, "cpj", curSent);
      addStaticFeature(jList, fcpj, parserAlpha);
      // Begin:
      // GN: why uncomment ? -> templateFeat does not exist
      //Feature fpj_fcpj = FeatureExtractor.mergeFeatures(
      //  9, this.featureNamesForMerging, fpj, fcpj);
      //addStaticFeature(jList, fpj_fcpj, parserAlpha);
      //Feature fcase = FeatureExtractor.templateFeat(j, "casej", curSent);
      //addStaticFeature(jList, fcase, parserAlpha);
      //Feature fullMerge = FeatureExtractor.mergeFeatures(
      //  10, this.featureNamesForMerging, fpj_fcpj, fcase);
      //addStaticFeature(jList, fullMerge, parserAlpha);
      // End

      Feature wfjp1 = FeatureExtractor.createFeatureForWF(j + 1, "wfjp1", curSent);
      addStaticFeature(jList, wfjp1, parserAlpha);
      Feature fpjp1_fpjp2_fpjp3 =
          FeatureExtractor.mergeFeatures(0, this.featureNamesForMerging, fpjp1, fpjp2, fpjp3);
      addStaticFeature(jList, fpjp1_fpjp2_fpjp3, parserAlpha);

      Feature fpjp4 = FeatureExtractor.createFeatureForPos(j + 4, "pjp4", curSent);
      addStaticFeature(jList, fpjp4, parserAlpha);
      staticFeatures[0][j] = new Feature[jList.size()];
      staticFeatures[0][j] = jList.toArray(staticFeatures[0][j]);
    }
    for (int i = 0; i < curSentArray.length + 1; i++) {
      List<Feature> iList = new ArrayList<Feature>();
      Feature fpi = FeatureExtractor.createFeatureForPos(i, "pi", curSent);
      addStaticFeature(iList, fpi, parserAlpha);
      Feature fpip1 = FeatureExtractor.createFeatureForPos(i + 1, "pip1", curSent);
      addStaticFeature(iList, fpip1, parserAlpha);
      Feature fwfi = FeatureExtractor.createFeatureForWF(i, "wfi", curSent);
      addStaticFeature(iList, fwfi, parserAlpha);
      Feature fcpi = FeatureExtractor.createFeatureForCPos(i, "cpi", curSent);
      addStaticFeature(iList, fcpi, parserAlpha);
      Feature fpi_fpip1 =
          FeatureExtractor.mergeFeatures(1, this.featureNamesForMerging, fpi, fpip1);
      addStaticFeature(iList, fpi_fpip1, parserAlpha);
      Feature fwfi_fcpi =
          FeatureExtractor.mergeFeatures(2, this.featureNamesForMerging, fwfi, fcpi);
      addStaticFeature(iList, fwfi_fcpi, parserAlpha);
      Feature fpip2 = FeatureExtractor.createFeatureForPos(i + 2, "pip2", curSent);
      addStaticFeature(iList, fpip2, parserAlpha);
      staticFeatures[1][i] = new Feature[iList.size()];
      staticFeatures[1][i] = iList.toArray(staticFeatures[1][i]);
    }
    curSent.setStaticFeatures(staticFeatures);
  }


  /*
  1.  wfj   OK (used as static)
  2.  pj    OK (used as static)
  cposj EXTRA in Implementation, but basically the same as pj
  -> but harmless according to AV -> but I do not use it -> leads to lower result
  3.  wfjp1 OK (used as static)
  4.  pjp1  OK (used as static)
  5.  wfjp2 NOT USED -> when added, it hurts
  6.  pjp2  OK (used as static)
  7.  wfjp3 NOT USED -> when added, it hurts
  8.  pjp3  OK (used as static)
    pjp4    EXTRA in Implementation
  9.  wfi   OK (used as static)
  10. pi    OK (used as static)
  cposi EXTRA in Implementation, but basically the same as pi
  -> but harmless according to AV -> but I do not use it
  11. pip1  OK (used as static)
  pip2  EXTRA in Implementation
  
  12. wfhi  OK (used as dynamic)
  13. phi   OK (used as dynamic)
  14. depi  OK (used as dynamic)
  15. depldi  OK (used as dynamic)
  16. deprdi  OK (used as dynamic)
  17. depldj  OK (used as dynamic)
  18. dist  OK (used as dynamic)
  
  19. merge2(pi,pip1)       OK (used as static no. 0)
  20. merge2(wfi,pi)        UNCLEAR (used as static no. 1)
  -> is/was actually used with cposi, which is basically the same as pi, 
  but unclear why is this defined
  21. merge3(pjp1,pjp2,pjp3)    OK (used as static no. 2)
  
  22. merge2(depldj,pj)     OK (used as dynamic no. 3; access to static features)
  23. merge3(pi,deprdi,depldi)  OK (used as dynamic no. 4; access to static features)
  24. merge2(depi,wfhi)     OK (used as dynamic no. 5)
  25. merge3(phi,pjp1,pip1)   OK (used as dynamic no. 6; access to static features)
  26. merge3(wfj,wfi,pjp3)    UNCLEAR (used as dynamic no. 7) -> corrected in code
  27. merge3(dist,pj,wfjp1)   UNCLEAR (used as dynamic no. 8) -> corrected in code
  */


  // GN: changed to be as close as possible to Thesis and by playing with
  // NER specific spelling features.
  @Override
  public void initializeStaticFeaturesCombined(Sentence curSent, boolean train) {

    // GN:   keeps i-features and j-features separately. What is third dimension used for ?
    Feature[][][] staticFeatures = new Feature[2][curSent.getSentArray().length + 1][];
    String[][] curSentArray = curSent.getSentArray();
    Alphabet parserAlpha = this.getParserAlphabet();
    for (int j = 1; j < curSentArray.length + 1; j++) {
      List<Feature> jList = new ArrayList<Feature>();

      Feature fpj = FeatureExtractor.createFeatureForPos(j, "pj", curSent);
      addStaticFeature(jList, fpj, parserAlpha);
      Feature fpjp1 = FeatureExtractor.createFeatureForPos(j + 1, "pjp1", curSent);
      addStaticFeature(jList, fpjp1, parserAlpha);
      Feature fpjp2 = FeatureExtractor.createFeatureForPos(j + 2, "pjp2", curSent);
      addStaticFeature(jList, fpjp2, parserAlpha);
      Feature fpjp3 = FeatureExtractor.createFeatureForPos(j + 3, "pjp3", curSent);
      addStaticFeature(jList, fpjp3, parserAlpha);

      Feature fwfj = FeatureExtractor.createFeatureForWF(j, "wfj", curSent);
      addStaticFeature(jList, fwfj, parserAlpha);

      Feature wfjp1 = FeatureExtractor.createFeatureForWF(j + 1, "wfjp1", curSent);
      addStaticFeature(jList, wfjp1, parserAlpha);

      Feature fpjp1_fpjp2_fpjp3 =
          FeatureExtractor.mergeFeatures(0, this.featureNamesForMerging, fpjp1, fpjp2, fpjp3);
      addStaticFeature(jList, fpjp1_fpjp2_fpjp3, parserAlpha);

      Feature fpjp4 = FeatureExtractor.createFeatureForPos(j + 4, "pjp4", curSent);
      addStaticFeature(jList, fpjp4, parserAlpha);

      Feature fcpj = FeatureExtractor.createFeatureForCPos(j, "cpj", curSent);
      addStaticFeature(jList, fcpj, parserAlpha);

      staticFeatures[0][j] = new Feature[jList.size()];
      staticFeatures[0][j] = jList.toArray(staticFeatures[0][j]);
    }
    for (int i = 0; i < curSentArray.length + 1; i++) {
      List<Feature> iList = new ArrayList<Feature>();
      Feature fpi = FeatureExtractor.createFeatureForPos(i, "pi", curSent);
      addStaticFeature(iList, fpi, parserAlpha);
      Feature fpip1 = FeatureExtractor.createFeatureForPos(i + 1, "pip1", curSent);
      addStaticFeature(iList, fpip1, parserAlpha);
      Feature fwfi = FeatureExtractor.createFeatureForWF(i, "wfi", curSent);
      addStaticFeature(iList, fwfi, parserAlpha);

      Feature fcpi = FeatureExtractor.createFeatureForCPos(i, "cpi", curSent);
      addStaticFeature(iList, fcpi, parserAlpha);

      Feature fpi_fpip1 =
          FeatureExtractor.mergeFeatures(1, this.featureNamesForMerging, fpi, fpip1);
      addStaticFeature(iList, fpi_fpip1, parserAlpha);
      // changed fcpi to fpi as defined in thesis
      Feature fwfi_fcpi =
          FeatureExtractor.mergeFeatures(2, this.featureNamesForMerging, fwfi, fcpi);
      addStaticFeature(iList, fwfi_fcpi, parserAlpha);

      Feature fpip2 = FeatureExtractor.createFeatureForPos(i + 2, "pip2", curSent);
      addStaticFeature(iList, fpip2, parserAlpha);

      staticFeatures[1][i] = new Feature[iList.size()];
      staticFeatures[1][i] = iList.toArray(staticFeatures[1][i]);
    }
    curSent.setStaticFeatures(staticFeatures);
  }


  private static void addStaticFeature(
      List<Feature> featureList, Feature feature, Alphabet parserAlpha) {

    Integer featureIndex = parserAlpha.getFeatureIndex(feature.getFeatureString());
    feature.setParserIndex(featureIndex);
    featureList.add(feature);
  }


  @Override
  public FeatureVector applyCombined(ParserState state, boolean train, boolean noLabels) {

    CovingtonParserState covState = (CovingtonParserState)state;
    Sentence curSent = covState.getCurSent();
    int j = covState.getJ();
    int i = covState.getI();
    FeatureVector featureVector = new FeatureVector();

    Alphabet parserAlpha = this.getParserAlphabet();
    Feature[][][] staticFeatures = curSent.getStaticFeatures(); //new
    Feature[] curStaticFeaturesJ = staticFeatures[0][j];

    // GN: show contents of static features J
    // System.out.println("curStaticFeaturesJ: ");

    // GN: I read this as such as the static features are inserted dynamically for 
    // each new training example.
    // The advantage is that we do not have to build the feature-value strings again.
    // So, this is how static features are used in memorization manner.
    // Since, static features are stored in a vector, and later accessed by the dynamic templates
    // I wonder whether ordering is crucial -> yes, it is crucial.
    for (Feature oneFeature : curStaticFeaturesJ) {
      featureVector.addFeature(oneFeature, parserAlpha, train);
    }

    Feature[] curStaticFeaturesI = staticFeatures[1][i];
    for (Feature oneFeature : curStaticFeaturesI) {
      featureVector.addFeature(oneFeature, parserAlpha, train);
    }

    Feature dist = FeatureExtractor.createFeatureForDistance(j, i);
    featureVector.addFeature(dist, parserAlpha, train);

    int hi = covState.getCurDepStruct().getHeads()[i];
    Feature wfhi = FeatureExtractor.createFeatureForWF(hi, "wfhi", curSent);
    featureVector.addFeature(wfhi, parserAlpha, train);

    Feature fphi = FeatureExtractor.createFeatureForPos(hi, "phi", curSent);
    featureVector.addFeature(fphi, parserAlpha, train);

    Feature fdepi = FeatureExtractor.createFeatureForDepRel(i, "depi", curSent, train);
    featureVector.addFeature(fdepi, parserAlpha, train);

    int ldj = covState.getCurDepStruct().getFarthestLeftDependent(j);
    Feature fdepldj = FeatureExtractor.createFeatureForDepRel(ldj, "depldj", curSent, train);
    featureVector.addFeature(fdepldj, parserAlpha, train);

    int ldi = covState.getCurDepStruct().getFarthestLeftDependent(i);
    Feature fdepldi = FeatureExtractor.createFeatureForDepRel(ldi, "depldi", curSent, train);
    featureVector.addFeature(fdepldi, parserAlpha, train);

    int rdi = covState.getCurDepStruct().getFarthestRightDependent(i);
    Feature fdeprdi = FeatureExtractor.createFeatureForDepRel(rdi, "deprdi", curSent, train);
    featureVector.addFeature(fdeprdi, parserAlpha, train);

    // curStaticFeaturesJ[0]: should be pj -> OK
    Feature fdepri_fpj = FeatureExtractor.mergeFeatures(
        3, this.featureNamesForMerging, fdepldj, curStaticFeaturesJ[0]);
    featureVector.addFeature(fdepri_fpj, parserAlpha, train);

    // curStaticFeaturesI[0]: should be pi -> OK
    Feature fpi_fdeprdi_fdepldi = FeatureExtractor.mergeFeatures(
        4, this.featureNamesForMerging, curStaticFeaturesI[0], fdeprdi, fdepldi);
    featureVector.addFeature(fpi_fdeprdi_fdepldi, parserAlpha, train);

    Feature fdepi_wfhi =
        FeatureExtractor.mergeFeatures(5, this.featureNamesForMerging, fdepi, wfhi);
    featureVector.addFeature(fdepi_wfhi, parserAlpha, train);

    // curStaticFeaturesJ/I[1]: should be pjp1 and pip1 -> OK
    Feature fphi_fpjp1_fpip1 = FeatureExtractor.mergeFeatures(
        6, this.featureNamesForMerging, fphi, curStaticFeaturesI[1], curStaticFeaturesJ[1]);
    featureVector.addFeature(fphi_fpjp1_fpip1, parserAlpha, train);

    // curStaticFeaturesJ[4] wfj is , curStaticFeaturesI[2] is wfi, curStaticFeaturesJ[2] is pjp2
    // laut thesis muesste das sein: wfj,wfi,pjp3
    Feature fst2 = FeatureExtractor.mergeFeatures(
        7, this.featureNamesForMerging, curStaticFeaturesJ[4], curStaticFeaturesI[2],
        curStaticFeaturesJ[3]);
    featureVector.addFeature(fst2, parserAlpha, train);

    // curStaticFeaturesJ[5] is cpos, curStaticFeaturesJ[6] is wfjp1
    // laut Thesis muesste es sein: pj und wfjp1, also 0 und 5
    Feature fst3 = FeatureExtractor.mergeFeatures(
        8, this.featureNamesForMerging, dist, curStaticFeaturesJ[0], curStaticFeaturesJ[5]);
    featureVector.addFeature(fst3, parserAlpha, train);
    return featureVector;
  }


  @Override
  public List<FeatureVector> apply(ParserState state, boolean train, boolean noLabels) {

    return null;
  }
}
