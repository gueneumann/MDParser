package de.dfki.lt.mdparser.features;

import java.util.List;

import de.dfki.lt.mdparser.algorithm.ParserState;
import de.dfki.lt.mdparser.data.Sentence;


public abstract class FeatureModel {

  private Alphabet parserAlphabet;
  private Alphabet labelerAlphabet;


  public FeatureModel(Alphabet parserAlphabet, Alphabet labelerAlphabet) {

    this.parserAlphabet = parserAlphabet;
    this.labelerAlphabet = labelerAlphabet;
  }


  public FeatureModel(Alphabet parserAlphabet) {

    this.parserAlphabet = parserAlphabet;
  }


  public Alphabet getParserAlphabet() {

    return this.parserAlphabet;
  }


  public Alphabet getLabelerAlphabet() {

    return this.labelerAlphabet;
  }


  public abstract List<FeatureVector> apply(ParserState state, boolean train, boolean noLabels);


  public abstract FeatureVector applyCombined(ParserState state, boolean train, boolean noLabels);


  public abstract void initializeStaticFeaturesCombined(Sentence sent, boolean train);
}
