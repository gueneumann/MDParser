package de.dfki.lt.mdparser.parser;

import java.io.File;
import java.io.IOException;

import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.model.ModelEditor;

public class CompactiseWorker {

  private Alphabet alphaParser;
  private double bias;
  private Parameter param;
  private String splitModelsDir;


  public CompactiseWorker(Alphabet alphaParser, String splitModelsDir, double bias) {

    this.alphaParser = alphaParser;
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
      int[][] compactArray = Trainer.compactiseTrainingDataFile(
          file, this.alphaParser.getNumberOfFeatures(), new File("splitC"));

      //System.out.println("new to old size: "+compactArray[0].length);

      Problem prob = Trainer.readProblem("splitC/" + file.getName(), this.bias);

      // GN: call the trainer
      Linear.disableDebugOutput();
      Model model = Linear.train(prob, this.param);

      // GN: and save the training files
      System.out.println("Save: " + this.splitModelsDir + "/" + file.getName());
      model.save(new File(this.splitModelsDir + "/" + file.getName()));

      this.alphaParser.writeToFile("splitA/" + file.getName(), compactArray);

      //System.out.println(alphaParser.getMaxIndex());
      ModelEditor modelEditor = new ModelEditor(
          new File(this.splitModelsDir + "/" + file.getName()),
          "splitA/" + file.getName(),
          true);
      modelEditor.editAlphabetAndModel("splitA/" + file.getName(), this.splitModelsDir + "/" + file.getName());
    } catch (IOException | InvalidInputDataException e) {
      e.printStackTrace();
    }
  }
}
