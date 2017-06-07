package de.dfki.lt.mdparser.caller;


import java.io.File;
import java.io.IOException;

import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.parser.Trainer;
import de.dfki.lt.mdparser.parser.TrainerMem;

public final class MDPtrainer {

  private static final String[] DIRS = { "split", "splitA", "splitF", "splitO", "splitC", "splitModels", "temp" };
  private static final String ALGORITHM = "covington";
  private static final String SPLIT_MODELS_DIR = "splitModels";
  private static final String SPLIT_FILE = "temp/split.txt";
  private static final String ALPHABET_FILE_PARSER = "temp/alphaParser.txt";


  private MDPtrainer() {

    // private constructor to enforce noninstantiability
  }


  public static void train(String trainFileParam, String archiveName)
      throws IOException {


    deleteOldDirs();
    createNewDirs();

    Archivator arch = new Archivator(archiveName);
    Trainer trainer = new Trainer();

    long s1 = System.currentTimeMillis();

    trainer.createAndTrainWithSplittingFromDisk(
        ALGORITHM, trainFileParam, SPLIT_MODELS_DIR, ALPHABET_FILE_PARSER);

    long s2 = System.currentTimeMillis();

    System.out.println("Complete Training time: " + ((s2 - s1)) + " milliseconds.");

    //ModelEditorTest.main(null);
    arch.pack();
    arch.delTemp();

    deleteOldDirs();
  }


  public static void trainMem(String trainFileParam, String archiveName)
      throws IOException {


    deleteOldDirs();
    createNewDirs();

    Archivator arch = new Archivator(archiveName);
    TrainerMem trainer = new TrainerMem();

    long s1 = System.currentTimeMillis();

    trainer.createAndTrainWithSplittingFromMemory(
        ALGORITHM, trainFileParam, SPLIT_MODELS_DIR, ALPHABET_FILE_PARSER, SPLIT_FILE);

    long s2 = System.currentTimeMillis();

    System.out.println("Complete Training time: " + ((s2 - s1)) + " milliseconds.");

    //ModelEditorTest.main(null);
    arch.pack();
    arch.delTemp();

    deleteOldDirs();
  }


  public static void createNewDirs() {

    for (int i = 0; i < DIRS.length; i++) {
      String dir = DIRS[i];
      File d = new File(dir);
      if (!d.exists()) {
        d.mkdir();
      }
    }
  }


  public static void deleteOldDirs() {

    for (int i = 0; i < DIRS.length; i++) {
      File[] files = new File(DIRS[i]).listFiles();
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


  public static void main(String[] args) {

    try {
      //mdpTrainer.setAlgorithm("stack");
      // Run parallel version of trainier
      train("resources/input/ptb3-std-training.conll", "ptb3-std.zip");
      // Run non-parallel (in memory) version of trainer
      //trainerMem("resources/input/ptb3-std-training.conll", "ptb3-std-nonpara.zip");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
