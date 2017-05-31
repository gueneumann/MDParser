package cases;


import java.io.IOException;

import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.data.Data;
import de.dfki.lt.mdparser.eval.Eval;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.parser.Parser;

public class ParserTest {

  public static void main(String[] args) throws IOException {

    String goldFile = args[0];

    String resultFile = "temp/1.conll";
    Parser parser = new Parser();
    Data d = new Data(goldFile, false);
    System.out.println("No. of sentences: " + d.getSentences().length);

    String archiveName = args[1];


    String[] dirs = {};
    Archivator arch = new Archivator(archiveName, dirs);
    arch.extract();
    Alphabet alphabetParser = new Alphabet(arch.getParserAlphabetInputStream());
    parser.setNumberOfClassesParser(alphabetParser.getMaxLabelIndex() - 1);

    String algorithm = "covington";
    long s3 = System.currentTimeMillis();
    parser.parseCombined(algorithm, d, arch, alphabetParser, false);

    long s4 = System.currentTimeMillis();
    System.out.println("Parsing time: " + ((s4 - s3)) + " milliseconds.");
    d.printToFile(resultFile);

    Eval ev = new Eval(goldFile, resultFile, 6, 6, 7, 7);
    System.out.println("Parent accuracy: " + ev.getParentsAccuracy());
    System.out.println("Label accuracy:  " + ev.getLabelsAccuracy());

  }
}
