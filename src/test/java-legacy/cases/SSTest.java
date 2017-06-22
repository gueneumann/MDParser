package cases;


import java.io.File;
import java.io.IOException;

import de.dfki.lt.mdparser.model.ModelEditor;
import de.dfki.lt.mdparser.sentencesplitter.SSEval;

public class SSTest {

  public static void main(String[] args) {

    try {
      SSTrainerTest.main(args);
      ModelEditor me = new ModelEditor(new File("temp/m0.txt"), "temp/ssalpha0.txt", true);
      me.editAlphabetAndModel("temp/ssalpha.txt", "temp/m.txt");
      SSPredictorTest.main(args);
      SSEval.main(args);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
