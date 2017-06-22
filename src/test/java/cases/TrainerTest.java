package cases;

import java.io.IOException;

import de.dfki.lt.mdparser.parser.Trainer;

public class TrainerTest {

  public static void main(String[] args) {

    String inputFile = args[0];

    Trainer trainer = new Trainer();
    try {
      trainer.createAndTrainWithSplittingFromDisk(inputFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
