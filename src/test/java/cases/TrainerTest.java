package cases;

import java.io.File;
import java.io.IOException;

import de.dfki.lt.mdparser.parser.Trainer;

public class TrainerTest {

  public static void main(String[] args) {

    String inputFile = args[0];


    String alphabetFileParser = "temp/alphaParser.txt";
    String alphabetFileLabeler = "temp/alphaLabeler.txt";

    Trainer trainer = new Trainer();
    String[] dirs = { "split", "splitA", "splitF", "splitO", "splitC", "splitModels", "temp" };
    String splitModelsDir = "splitModels";
    String algorithm = "covington";
    String splitFile = "temp/split.txt";
    deleteOld(dirs);
    createNew(dirs);
    try {
      trainer.createAndTrainWithSplittingFromDisk(algorithm, inputFile, splitModelsDir, alphabetFileParser,
          alphabetFileLabeler, splitFile);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }


  static void createNew(String[] dirs) {

    for (int i = 0; i < dirs.length; i++) {
      String dir = dirs[i];
      File d = new File(dir);
      if (!d.exists()) {
        d.mkdir();
      }
    }

  }


  public static void deleteOld(String[] dirs) {

    for (int i = 0; i < dirs.length; i++) {
      File[] files = new File(dirs[i]).listFiles();
      if (files != null) {
        for (int k = 0; k < files.length; k++) {
          boolean b = files[k].delete();
          if (!b) {
            System.out.println("Failed to delete " + files[k]);
          }

        }
      }
    }
  }
}
