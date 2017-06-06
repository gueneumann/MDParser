package de.dfki.lt.mdparser.caller;


import java.io.File;
import java.io.IOException;

import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.parser.Trainer;
import de.dfki.lt.mdparser.parser.TrainerMem;

public class MDPtrainer {

  private String trainFile;
  private String modelFile;

  private String[] trainArgs;
  private String[] parseArgs;

  private String[] dirs = { "split", "splitA", "splitF", "splitO", "splitC", "splitModels", "temp" };
  private String splitModelsDir = "splitModels";
  private String algorithm = "covington";
  private String splitFile = "temp/split.txt";
  private String alphabetFileParser = "temp/alphaParser.txt";
  private String alphabetFileLabeler = "temp/alphaLabeler.txt";


  public MDPtrainer() {
  }


  public String getAlgorithm() {

    return this.algorithm;
  }


  public void setAlgorithm(String algorithm) {

    this.algorithm = algorithm;
  }


  public static void createNew(String[] dirs) {

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


  public void trainer(String trainFileParam, String archiveName)
      throws IOException {


    deleteOld(this.dirs);
    createNew(this.dirs);

    Archivator arch = new Archivator(archiveName);
    Trainer trainer = new Trainer();

    long s1 = System.currentTimeMillis();

    trainer.createAndTrainWithSplittingFromDisk(this.algorithm, trainFileParam,
        this.splitModelsDir, this.alphabetFileParser, this.alphabetFileLabeler, this.splitFile);

    long s2 = System.currentTimeMillis();

    System.out.println("Complete Training time: " + ((s2 - s1)) + " milliseconds.");

    //ModelEditorTest.main(null);
    arch.pack();
    arch.delTemp();

    deleteOld(this.dirs);
  }


  public void trainerMem(String trainFileParam, String archiveName)
      throws IOException {


    deleteOld(this.dirs);
    createNew(this.dirs);

    Archivator arch = new Archivator(archiveName);
    TrainerMem trainer = new TrainerMem();

    long s1 = System.currentTimeMillis();

    trainer.createAndTrainWithSplittingFromMemory(this.algorithm, trainFileParam,
        this.splitModelsDir, this.alphabetFileParser, this.alphabetFileLabeler, this.splitFile);

    long s2 = System.currentTimeMillis();

    System.out.println("Complete Training time: " + ((s2 - s1)) + " milliseconds.");

    //ModelEditorTest.main(null);
    arch.pack();
    arch.delTemp();

    deleteOld(this.dirs);
  }


  public static void main(String[] args) {

    try {
      MDPtrainer mdpTrainer = new MDPtrainer();
      //mdpTrainer.setAlgorithm("stack");
      // Run parallel version of trainier
      mdpTrainer.trainer("resources/input/ptb3-std-training.conll", "ptb3-std.zip");
      // Run non-parallel (in memory) version of trainer
      //mdpTrainer.trainerMem("resources/input/ptb3-std-training.conll", "ptb3-std-nonpara.zip");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
