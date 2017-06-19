package cases;

import java.io.IOException;

import de.dfki.lt.mdparser.parser.Trainer;

public class TrainerTest {

  public static void main(String[] args) {

    String inputFile = args[0];

    Trainer trainer = new Trainer();
    String algorithm = "covington";
    try {
      trainer.createAndTrainWithSplittingFromDisk(algorithm, inputFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
