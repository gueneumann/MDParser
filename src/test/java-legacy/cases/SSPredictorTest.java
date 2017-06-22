package cases;


import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import de.dfki.lt.mdparser.data.Data;
import de.dfki.lt.mdparser.sentencesplitter.SSPredictor;

public class SSPredictorTest {

  public static void main(String[] args) throws IOException {

    String conllInputFile = "";
    String taggerFile = "";
    String language = args[0];
    if (language.equals("english")) {
      taggerFile = "resources/tagger/BROWN_MEDLINE/MEDLINE-BROWN-FINAL";
      conllInputFile = "input/english-devel.conll";
    } else if (language.equals("german")) {
      taggerFile = "resources/tagger/NEGRA/NEGRA";
      conllInputFile = "resources/input/german_tiger_test.conll";
    }
    String[] modelFiles =
        { "temp/ssalpha.txt", "temp/m.txt", "temp/lc.txt", "temp/ne.txt", "temp/end.txt", taggerFile };
    SSPredictor ssp = new SSPredictor(modelFiles);
    String inp = ssp.readInput(conllInputFile, "conll");
    List<String> tokensList = ssp.tokenise(
        //"Meinen Sie, das ist eine Chance, um einen Konsens im Umgang mit Rechtradikalen "
        //+ "zu finden? So gut wie nichts in der Hand."
        inp);

    //System.out.println(tokensList);
    List<List<String>> result = ssp.predict(tokensList);
    FileOutputStream out = new FileOutputStream("temp/res.txt");
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter curBw = new BufferedWriter(or);
    for (int i = 0; i < result.size(); i++) {
      System.out.println(result.get(i));
      for (int k = 0; k < result.get(i).size(); k++) {
        curBw.append(result.get(i).get(k));
        if (k < result.get(i).size() - 1) {
          //curBw.append(" ");
        }
      }
      curBw.append("\n");
    }
    System.out.println(result.size());
    Data d = new Data(conllInputFile, true);
    System.out.println(d.getSentences().length);
    curBw.close();
    //ssp.evaluate(result, inputFile);
  }
}
