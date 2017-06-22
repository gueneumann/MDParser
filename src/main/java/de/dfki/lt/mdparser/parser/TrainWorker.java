package de.dfki.lt.mdparser.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import de.dfki.lt.mdparser.config.GlobalConfig;
import de.dfki.lt.mdparser.features.Alphabet;

public class TrainWorker {

  private Alphabet alpha;
  private double bias;
  private Parameter param;


  public TrainWorker(Alphabet alpha, double bias)
      throws IOException {

    this.alpha = alpha;
    this.bias = bias;
    this.param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.1);

    Files.createDirectories(GlobalConfig.SPLIT_COMPACT_FOLDER);
    Files.createDirectories(GlobalConfig.SPLIT_MODELS_FOLDER);
  }


  public void processFile(Path file) {

    try {
      long threadId = Thread.currentThread().getId();
      System.out.println("Hello from TrainWorker in thread " + threadId);

      System.out.println("... Steps: compactifize/sort training file " + file);
      System.out.println("... and store in " + GlobalConfig.SPLIT_COMPACT_FOLDER
          + "; read problem and call trainer, and finally save models and alphabet, and edit them. ");
      int[][] compactArray =
          Trainer.compactiseTrainingDataFile(file, this.alpha.getNumberOfFeatures());
      //System.out.println("new to old size: "+compactArray[0].length);

      String alphaFileName = GlobalConfig.SPLIT_ALPHA_FOLDER.resolve(file.getFileName()).toString();
      this.alpha.writeToFile(Paths.get(alphaFileName), compactArray);

      // GN: call the trainer
      Problem prob = Trainer.readProblem(GlobalConfig.SPLIT_COMPACT_FOLDER.resolve(file.getFileName()), this.bias);
      Linear.disableDebugOutput();
      Model model = Linear.train(prob, this.param);

      // GN: and save the training files
      Path modelPath = GlobalConfig.SPLIT_MODELS_FOLDER.resolve(file.getFileName());
      System.out.println("Save: " + modelPath);
      model.save(modelPath.toFile());

      Set<Integer> unusedFeatures = Trainer.getUnusedFeatures(modelPath);

      Alphabet compactAlpha = new Alphabet(Paths.get(alphaFileName));
      compactAlpha.removeUnusedFeatures(unusedFeatures);
      compactAlpha.writeToFile(Paths.get(alphaFileName));

      Trainer.removeUnusedFeaturesFromModel(modelPath, unusedFeatures, compactAlpha.getNumberOfFeatures());
    } catch (IOException | InvalidInputDataException e) {
      e.printStackTrace();
    }
  }
}
