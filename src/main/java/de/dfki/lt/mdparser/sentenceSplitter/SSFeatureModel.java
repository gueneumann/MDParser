package de.dfki.lt.mdparser.sentenceSplitter;

import java.util.List;
import java.util.Set;

import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.Feature;
import de.dfki.lt.mdparser.features.FeatureExtractor;
import de.dfki.lt.mdparser.features.FeatureVector;

public class SSFeatureModel {

  // GN: This defines the feature vector basically by enumerating all feature function
  // i denotes the token for which the feature vector is constructed and tokens is a reference
  // to the context defined by all other tokens.
  // Then for each feature vector cell, the corresponding feature is named and its value is computed.

  public FeatureVector apply(boolean train, int i, List<String> tokens, List<String> posTags,
      List<Set<String>> sets, Alphabet alpha) {

    FeatureVector fv = new FeatureVector(train);
    SSFeatureExtractor ssfe = new SSFeatureExtractor();
    FeatureExtractor fe = new FeatureExtractor();
    String[] mfn = { "fm0", "fm1", "fm2", "fm3", "fm4", "fm5" };

    String wfj = ssfe.wordFormFeature(tokens, i);
    Feature f1 = new Feature("wfj", wfj);
    fv.addFeature(f1, alpha, train);
    String wfjp1 = ssfe.wordFormFeature(tokens, i + 1);
    Feature f2 = new Feature("wfjp1", wfjp1);
    fv.addFeature(f2, alpha, train);
    String wfjm1 = ssfe.wordFormFeature(tokens, i - 1);
    Feature f3 = new Feature("wfjm1", wfjm1);
    fv.addFeature(f3, alpha, train);
    String wfjp2 = ssfe.wordFormFeature(tokens, i + 2);
    Feature f4 = new Feature("wfjp2", wfjp2);
    fv.addFeature(f4, alpha, train);
    String wfjm2 = ssfe.wordFormFeature(tokens, i - 2);
    Feature f5 = new Feature("wfjm2", wfjm2);
    fv.addFeature(f5, alpha, train);
    String wfjp3 = ssfe.wordFormFeature(tokens, i + 3);
    Feature f14 = new Feature("wfjp3", wfjp3);
    fv.addFeature(f14, alpha, train);
    String wfjm3 = ssfe.wordFormFeature(tokens, i - 3);
    Feature f15 = new Feature("wfjm3", wfjm3);
    fv.addFeature(f15, alpha, train);

    Feature f6 = new Feature("wfjp1c", ssfe.isUpperCase(wfjp1));
    fv.addFeature(f6, alpha, train);
    Feature f7 = new Feature("wfjm1c", ssfe.isUpperCase(wfjm1));
    fv.addFeature(f7, alpha, train);
    Feature f8 = new Feature("wfjp2c", ssfe.isUpperCase(wfjp2));
    fv.addFeature(f8, alpha, train);
    Feature f9 = new Feature("wfjm2c", ssfe.isUpperCase(wfjm2));
    fv.addFeature(f9, alpha, train);
    Feature f16 = new Feature("wfjp3c", ssfe.isUpperCase(wfjp3));
    fv.addFeature(f16, alpha, train);
    Feature f17 = new Feature("wfjm3c", ssfe.isUpperCase(wfjm3));
    fv.addFeature(f17, alpha, train);
    Feature f10 = new Feature("wfjp1n", ssfe.isNumber(wfjp1));
    fv.addFeature(f10, alpha, train);
    Feature f11 = new Feature("wfjm1n", ssfe.isNumber(wfjm1));
    fv.addFeature(f11, alpha, train);
    Feature f12 = new Feature("wfjp1lc", ssfe.isInTheList(wfjp1, sets.get(0)));
    fv.addFeature(f12, alpha, train);
    Feature f12b = new Feature("wfjp2lc", ssfe.isInTheList(wfjp2, sets.get(0)));
    fv.addFeature(f12b, alpha, train);
    Feature f13 = fe.merge2(0, mfn, f1, f2);
    fv.addFeature(f13, alpha, train);
    String pjp1 = ssfe.posFeature(posTags, i + 1);
    String pjp2 = ssfe.posFeature(posTags, i + 2);
    /*
    Feature f18 = new Feature("pjm1",ssfe.posFeature(posTags,i-1));
    fv.addFeature(f18, alpha, train);
    Feature f19 = new Feature("pjp1",ssfe.posFeature(posTags,i+1));
    fv.addFeature(f19, alpha, train);
    Feature f20 = new Feature("pjm2",ssfe.posFeature(posTags,i-2));
    fv.addFeature(f20, alpha, train);
    Feature f21 = new Feature("pjp2",ssfe.posFeature(posTags,i+2));
    fv.addFeature(f21, alpha, train);
    Feature f22 = new Feature("pjm3",ssfe.posFeature(posTags,i-3));
    fv.addFeature(f22, alpha, train);
    Feature f23 = new Feature("pjp3",ssfe.posFeature(posTags,i+3));
    fv.addFeature(f23, alpha, train);
    */
    //System.out.println(posTags.get(i)+" "+posTags.get(i+1));
    //System.out.println(f2+" "+f19);
    Feature f32 = new Feature("pjp1lc", ssfe.isInTheList(pjp1, sets.get(1)));
    fv.addFeature(f32, alpha, train);
    Feature f32b = new Feature("pjp2lc", ssfe.isInTheList(pjp2, sets.get(1)));
    fv.addFeature(f32b, alpha, train);
    Feature f24 = new Feature("wfjm1p", ssfe.containsPunctuation(wfjm1));
    fv.addFeature(f24, alpha, train);
    Feature f25 = new Feature("wfjp1p", ssfe.containsPunctuation(wfjp1));
    fv.addFeature(f25, alpha, train);

    //Feature fm1 = fe.merge2(1, mfn, f19, f2);
    //fv.addFeature(fm1, alpha, train);
    Feature fm2 = fe.merge2(2, mfn, f11, f3);
    fv.addFeature(fm2, alpha, train);
    Feature fm3 = fe.merge2(3, mfn, f25, f7);
    fv.addFeature(fm3, alpha, train);
    //Feature fm4 = fe.merge2(3,mfn,f12,f19);
    //fv.addFeature(fm4, alpha, train);
    Feature fm5 = fe.merge2(4, mfn, f8, f25);
    fv.addFeature(fm5, alpha, train);
    return fv;
  }

}
