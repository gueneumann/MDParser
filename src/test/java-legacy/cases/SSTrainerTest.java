package cases;


import java.io.IOException;

import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.dfki.lt.mdparser.sentencesplitter.SSTrainer;

public class SSTrainerTest {

  public static void main(String[] args) {

    String conllInputFile = "";
    String taggerFile = "";
    String language = args[0];
    if (language.equals("english")) {
      taggerFile = "resources/tagger/BROWN_MEDLINE/MEDLINE-BROWN-FINAL";
      conllInputFile = "resources/input/english-train.conll";
    } else if (language.equals("german")) {
      taggerFile = "resources/tagger/NEGRA/NEGRA";
      conllInputFile = "resources/input/german_tiger_train.conll";
    }
    String[] modelFiles = { "temp/ss.txt", "temp/ssalpha0.txt", "temp/m0.txt", "temp/lc.txt", "temp/ne.txt",
        "temp/end.txt", "temp/nonend.txt",
        "temp/abbr.txt", "temp/first.txt", taggerFile };
    // GN: Trainer gets training file and model files, i.e.,
    // files which carry or will carry important information for feature vector representation
    SSTrainer sp = new SSTrainer(conllInputFile, modelFiles);
    try {
      sp.createTrainingData(language);
      sp.train();
    } catch (IOException | InvalidInputDataException e) {
      e.printStackTrace();
    }
  }
}
