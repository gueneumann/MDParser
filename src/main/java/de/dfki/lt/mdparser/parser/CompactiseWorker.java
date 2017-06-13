package de.dfki.lt.mdparser.parser;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import de.dfki.lt.mdparser.features.Alphabet;

public class CompactiseWorker {

  private Alphabet alpha;
  private double bias;
  private Parameter param;
  private String splitModelsDir;


  public CompactiseWorker(Alphabet alpha, String splitModelsDir, double bias) {

    this.alpha = alpha;
    this.bias = bias;
    this.splitModelsDir = splitModelsDir;
    this.param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.1);
  }


  public void processFile(File file) {

    try {
      long threadId = Thread.currentThread().getId();
      System.out.println("Hello from Thread in ComapctWorkerThread " + threadId);

      System.out.println("... Steps: compactifize/sort training file " + file);
      System.out.println("... and store in splitC; read problem and call trainer, "
          + "and finally save models and alphabet, and edit them. ");
      int[][] compactArray =
          Trainer.compactiseTrainingDataFile(file, this.alpha.getNumberOfFeatures());
      //System.out.println("new to old size: "+compactArray[0].length);

      String alphaFileName = "splitA/" + file.getName();
      this.alpha.writeToFile(alphaFileName, compactArray);

      // GN: call the trainer
      Problem prob = Trainer.readProblem("splitC/" + file.getName(), this.bias);
      Linear.disableDebugOutput();
      Model model = Linear.train(prob, this.param);

      // GN: and save the training files
      String modelFileName = this.splitModelsDir + "/" + file.getName();
      System.out.println("Save: " + modelFileName);
      model.save(new File(modelFileName));

      Set<Integer> unusedFeatures = Trainer.getUnusedFeatures(modelFileName);

      Alphabet compactAlpha = new Alphabet(alphaFileName);
      compactAlpha.removeUnusedFeatures(unusedFeatures);
      compactAlpha.writeToFile(alphaFileName);

      Trainer.removeUnusedFeaturesFromModel(modelFileName, unusedFeatures, compactAlpha.getNumberOfFeatures());
    } catch (IOException | InvalidInputDataException e) {
      e.printStackTrace();
    }
  }
}
