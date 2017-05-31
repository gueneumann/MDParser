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
import pi.ParIterator;

public class CompactiseWorkerThread extends Thread {

  private ParIterator<File> pi;
  private int id;
  private Alphabet alphaParser;
  private Trainer trainer = new Trainer();
  private Problem prob = null;
  private Parameter param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.1);
  private String splitModelsDir;


  public CompactiseWorkerThread(int i, ParIterator<File> iter, Alphabet alphaParser, String splitModelsDir) {
    this.id = i;
    this.pi = iter;
    this.alphaParser = alphaParser;
    this.splitModelsDir = splitModelsDir;
  }


  @Override
  public void run() {

    while (this.pi.hasNext()) {
      System.out.println("Hello from Thread in CompactiseWorkerThread " + this.id);


      File element = this.pi.next();

      System.out.println("... Steps: compactifize/sort training file " + element);
      System.out.println("... and store in splitC; read problem and call trainer, "
          + "and finally save models and alphabet, and edit them. ");
      int[][] compactArray = null;
      try {
        compactArray = this.trainer.compactiseTrainingDataFile(
            element, this.alphaParser.getMaxIndex(), new File("splitC"));
      } catch (IOException e) {
        e.printStackTrace();
      }
      //System.out.println("new to old size: "+compactArray[0].length);
      try {
        this.trainer.readProblem("splitC/" + element.getName());
        this.prob = this.trainer.getProblem();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (InvalidInputDataException e) {
        e.printStackTrace();
      }

      // GN: call the trainer
      Linear.disableDebugOutput();
      Model m = Linear.train(this.prob, this.param);

      // GN: and save the training files
      try {
        System.out.println("Save: " + this.splitModelsDir + "/" + element.getName());
        m.save(new File(this.splitModelsDir + "/" + element.getName()));
      } catch (IOException e) {
        e.printStackTrace();
      }
      try {
        this.trainer.saveAlphabet(this.alphaParser, m, compactArray, new File("splitA/" + element.getName()));
      } catch (IOException e) {
        e.printStackTrace();
      }
      //System.out.println(alphaParser.getMaxIndex());
      ModelEditor me = null;
      try {
        me = new ModelEditor(
            new File(this.splitModelsDir + "/" + element.getName()), "splitA/" + element.getName(), true);
      } catch (IOException e) {
        e.printStackTrace();
      }
      try {
        me.editAlphabetAndModel("splitA/" + element.getName(), this.splitModelsDir + "/" + element.getName());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    System.out.println("    Thread " + this.id + " has finished.");
  }

}
