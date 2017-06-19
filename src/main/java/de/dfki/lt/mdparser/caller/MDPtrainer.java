package de.dfki.lt.mdparser.caller;


import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.config.GlobalConfig;
import de.dfki.lt.mdparser.parser.Trainer;
import de.dfki.lt.mdparser.parser.TrainerMem;

public final class MDPtrainer {

  private static final String ALGORITHM = "covington";


  private MDPtrainer() {

    // private constructor to enforce noninstantiability
  }


  public static void train(String trainFileParam, String archiveName)
      throws IOException {

    deleteModelBuildeFolder();

    Archivator arch = new Archivator(archiveName);
    Trainer trainer = new Trainer();

    long s1 = System.currentTimeMillis();

    trainer.createAndTrainWithSplittingFromDisk(ALGORITHM, trainFileParam);

    long s2 = System.currentTimeMillis();

    System.out.println("Complete Training time: " + ((s2 - s1)) + " milliseconds.");

    //ModelEditorTest.main(null);
    arch.pack();
  }


  public static void trainMem(String trainFileParam, String archiveName)
      throws IOException {

    deleteModelBuildeFolder();

    Archivator arch = new Archivator(archiveName);
    TrainerMem trainer = new TrainerMem();

    long s1 = System.currentTimeMillis();

    trainer.createAndTrainWithSplittingFromMemory(ALGORITHM, trainFileParam);

    long s2 = System.currentTimeMillis();

    System.out.println("Complete Training time: " + ((s2 - s1)) + " milliseconds.");

    //ModelEditorTest.main(null);
    arch.pack();
  }


  private static void deleteModelBuildeFolder()
      throws IOException {

    if (GlobalConfig.getModelBuildFolder().toString().trim().length() == 0) {
      deleteFolder(GlobalConfig.SPLIT_ALPHA_FOLDER);
      deleteFolder(GlobalConfig.FEATURE_VECTORS_FOLDER);
      deleteFolder(GlobalConfig.SPLIT_INITIAL_FOLDER);
      deleteFolder(GlobalConfig.SPLIT_ADJUST_FOLDER);
      deleteFolder(GlobalConfig.SPLIT_COMPACT_FOLDER);
      deleteFolder(GlobalConfig.SPLIT_MODELS_FOLDER);
      deleteFolder(GlobalConfig.FEATURE_VECTORS_FOLDER);
      try {
        Files.delete(GlobalConfig.ALPHA_FILE);
      } catch (NoSuchFileException e) {
        // nothing to do, file already deleted
      }
      try {
        Files.delete(GlobalConfig.SPLIT_FILE);
      } catch (NoSuchFileException e) {
        // nothing to do, file already deleted
      }
    } else {
      deleteFolder(GlobalConfig.getModelBuildFolder());
    }
  }


  private static void deleteFolder(Path path)
      throws IOException {

    try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException {

          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }


        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            throws IOException {

          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (NoSuchFileException e) {
      // nothing to do, file already deleted
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
