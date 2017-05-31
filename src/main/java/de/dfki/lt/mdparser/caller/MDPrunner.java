package de.dfki.lt.mdparser.caller;


import java.io.IOException;

import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.data.Data;
import de.dfki.lt.mdparser.eval.Eval;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.parser.Parser;

public class MDPrunner {

  private String[] dirs = {};
  private String algorithm = "covington";
  private Parser parser = new Parser();
  private Data data = null;
  private Eval evaluator = null;


  // Getters and setters

  // Class instantiation
  public MDPrunner() {
  }


  public Eval getEvaluator() {

    return this.evaluator;
  }


  public void setEvaluator(Eval evaluator) {

    this.evaluator = evaluator;
  }


  public Data getData() {

    return this.data;
  }


  public void setData(Data data) {

    this.data = data;
  }


  public String getAlgorithm() {

    return this.algorithm;
  }


  public void setAlgorithm(String algorithm) {

    this.algorithm = algorithm;
  }


  // Methods

  public void conllFileParsingAndEval(String conllFile, String resultFile, String modelFile)
      throws IOException {

    this.parser = new Parser();
    this.data = new Data(conllFile, false);
    System.out.println("No. of sentences: " + this.data.getSentences().length);

    Archivator arch = new Archivator(modelFile, this.dirs);
    arch.extract();
    Alphabet alphabetParser = new Alphabet(arch.getParserAlphabetInputStream());
    this.parser.setNumberOfClassesParser(alphabetParser.getMaxLabelIndex() - 1);

    this.parser.parseCombined(this.algorithm, this.data, arch, alphabetParser, false);

    this.getData().printToFile(resultFile);
    this.evalParser(conllFile, resultFile);

  }


  public void evalParser(String conllFile, String resultFile) throws IOException {

    this.evaluator = new Eval(conllFile, resultFile, 6, 6, 7, 7);
    System.out.println("Parent accuracy: " + this.evaluator.getParentsAccuracy());
    System.out.println("Label accuracy:  " + this.evaluator.getLabelsAccuracy());
  }


  public void conllFileParsingAndLinearize(String conllFile, String resultFile, String modelFile)
      throws IOException {

    this.parser = new Parser();
    this.data = new Data(conllFile, false);
    System.out.println("No. of sentences: " + this.data.getSentences().length);

    Archivator arch = new Archivator(modelFile, this.dirs);
    arch.extract();
    Alphabet alphabetParser = new Alphabet(arch.getParserAlphabetInputStream());
    this.parser.setNumberOfClassesParser(alphabetParser.getMaxLabelIndex() - 1);

    this.parser.parseCombined(this.algorithm, this.data, arch, alphabetParser, false);

    this.getData().testLinearizedToFile(resultFile);
  }


  public static void main(String[] args)
      throws IOException {

    MDPrunner mdpRunner = new MDPrunner();
    String conllFile = "/Users/gune00/data/UniversalDependencies/conll/German/de-ud-test.conll";
    String resultFile = "/Users/gune00/data/UniversalDependencies/conll/German/de-ud-test-result.conll";
    String modelFile = "/Users/gune00/data/UniversalDependencies/conll/German/de-MDPmodel.zip";

    conllFile = "resources/input/ptb3-std-test.conll";
    resultFile = "resources/input/ptb3-std-test.conll-result.conll";
    modelFile = "ptb3-std.zip";

    mdpRunner.conllFileParsingAndEval(conllFile, resultFile, modelFile);
    //mdpRunner.conllFileParsingAndLinearize(conllFile, resultFile, modelFile);
  }
}
