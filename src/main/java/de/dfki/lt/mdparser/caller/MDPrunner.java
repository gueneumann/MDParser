package de.dfki.lt.mdparser.caller;


import java.io.IOException;

import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.data.Data;
import de.dfki.lt.mdparser.eval.Eval;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.parser.Parser;

public final class MDPrunner {

  private static final String ALGORITHM = "covington";


  private MDPrunner() {

    // private constructor to enforce noninstantiability
  }


  public static void conllFileParsingAndEval(String conllFile, String resultFile, String modelFile)
      throws IOException {

    Parser parser = new Parser();
    Data data = new Data(conllFile, false);
    System.out.println("No. of sentences: " + data.getSentences().length);

    Archivator arch = new Archivator(modelFile);
    arch.extract();
    Alphabet alphabetParser = new Alphabet(arch.getParserAlphabetInputStream());
    parser.setNumberOfClassesParser(alphabetParser.getMaxLabelIndex() - 1);

    parser.parseCombined(ALGORITHM, data, arch, alphabetParser, false);

    data.printToFile(resultFile);
    Eval evaluator = new Eval(conllFile, resultFile, 6, 6, 7, 7);
    System.out.println("Parent accuracy: " + evaluator.getParentsAccuracy());
    System.out.println("Label accuracy:  " + evaluator.getLabelsAccuracy());
  }


  public static void conllFileParsingAndLinearize(String conllFile, String resultFile, String modelFile)
      throws IOException {

    Parser parser = new Parser();
    Data data = new Data(conllFile, false);
    System.out.println("No. of sentences: " + data.getSentences().length);

    Archivator arch = new Archivator(modelFile);
    arch.extract();
    Alphabet alphabetParser = new Alphabet(arch.getParserAlphabetInputStream());
    parser.setNumberOfClassesParser(alphabetParser.getMaxLabelIndex() - 1);

    parser.parseCombined(ALGORITHM, data, arch, alphabetParser, false);

    data.testLinearizedToFile(resultFile);
  }


  public static void main(String[] args)
      throws IOException {

    String conllFile = "/Users/gune00/data/UniversalDependencies/conll/German/de-ud-test.conll";
    String resultFile = "/Users/gune00/data/UniversalDependencies/conll/German/de-ud-test-result.conll";
    String modelFile = "/Users/gune00/data/UniversalDependencies/conll/German/de-MDPmodel.zip";

    conllFile = "resources/input/ptb3-std-test.conll";
    resultFile = "resources/input/ptb3-std-test.conll-result.conll";
    modelFile = "ptb3-std.zip";

    conllFileParsingAndEval(conllFile, resultFile, modelFile);
    //conllFileParsingAndLinearize(conllFile, resultFile, modelFile);
  }
}
