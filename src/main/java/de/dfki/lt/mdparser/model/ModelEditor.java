package de.dfki.lt.mdparser.model;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import de.dfki.lt.mdparser.features.Alphabet;

public class ModelEditor {

  private String solverType;
  private String nrClass;
  private String label;
  private int numberOfClasses;
  private String nrFeature;
  private String bias;
  private HashMap<Integer, Integer> oldIndexToNewIndexMap;
  private double[] weights;
  private Alphabet alpha;
  private HashSet<Integer> neverUsed;
  private HashSet<Integer> lowWeighted;
  private File modelFile;


  public ModelEditor(File modelFile, String alphabetFile, boolean newVer)
      throws IOException {
    this.alpha = new Alphabet(alphabetFile);
    this.modelFile = modelFile;
    FileInputStream in = new FileInputStream(modelFile);
    BufferedInputStream bis = new BufferedInputStream(in, 8000);
    InputStreamReader ir = new InputStreamReader(bis, "UTF8");
    BufferedReader fr = new BufferedReader(ir);
    for (int k = 0; k < 6; k++) {
      fr.readLine();
    }
    String line;
    int wIndex = 1;
    this.neverUsed = new HashSet<Integer>();
    while ((line = fr.readLine()) != null) {
      String[] lineArray = line.split("\\s+");
      boolean zeroLine = true;
      for (int k = 0; k < lineArray.length && zeroLine; k++) {
        if (Math.abs(Double.valueOf(lineArray[k])) > 0.1) {
          //if (Math.abs(Double.valueOf(lineArray[k])) != 0
          //    && (!lineArray[k].contains("e") && !lineArray[k].contains("E"))) {
          zeroLine = false;
        }
      }
      if (zeroLine) {
        this.neverUsed.add(wIndex);
      }
      //else {
      //  System.out.println(line);
      //}
      wIndex++;
    }
    fr.close();

    //System.out.println(alphabetFile + ": " + (this.alpha.getNumberOfFeatures() + 1) + " " + wIndex);
    //System.out.println(this.neverUsed.size());
  }


  public void editAlphabetAndModel(String alphabetFileName, String modelFileParam) throws IOException {

    this.alpha.removeNeverUsedFeatures(this.neverUsed);
    this.alpha.writeToFile(alphabetFileName);
    editModel(this.neverUsed, modelFileParam);
  }


  private void editModel(Set<Integer> neverUsedParam, String modelFileParam) throws IOException {

    FileInputStream in = new FileInputStream(this.modelFile);
    BufferedInputStream bis = new BufferedInputStream(in, 8000);
    InputStreamReader ir = new InputStreamReader(bis, "UTF8");
    BufferedReader fr = new BufferedReader(ir);
    String line;
    this.solverType = fr.readLine();
    this.nrClass = fr.readLine();
    this.label = fr.readLine();
    //nr_feature = fr.readLine();
    fr.readLine();
    this.nrFeature = "nr_feature " + this.alpha.getNumberOfFeatures();
    this.numberOfClasses = Integer.valueOf(this.nrClass.split(" ")[1]);
    this.bias = fr.readLine();
    fr.readLine();
    if (this.numberOfClasses != 2) {
      this.weights = new double[Integer.valueOf(this.nrFeature.split(" ")[1]) * this.numberOfClasses];
    } else {
      this.weights = new double[Integer.valueOf(this.nrFeature.split(" ")[1])];
    }
    int k = 0;
    int l = 1;
    while ((line = fr.readLine()) != null) {
      if (!neverUsedParam.contains(l)) {
        String[] weightsArray = line.split(" ");
        if (this.numberOfClasses != 2) {
          for (int c = 0; c < this.numberOfClasses; c++) {
            this.weights[k * this.numberOfClasses + c] = Double.valueOf(weightsArray[c]);
          }
          k++;
        } else {
          this.weights[k] = Double.valueOf(weightsArray[0]);
          k++;
        }
      }
      l++;
    }
    fr.close();
    printToFile(modelFileParam);
  }


  public void setSolverType(String solverType) {

    this.solverType = solverType;
  }


  public String getSolverType() {

    return this.solverType;
  }


  public void setNr_class(String nr_class) {

    this.nrClass = nr_class;
  }


  public String getNr_class() {

    return this.nrClass;
  }


  public void setNumberOfClasses(int numberOfClasses) {

    this.numberOfClasses = numberOfClasses;
  }


  public int getNumberOfClasses() {

    return this.numberOfClasses;
  }


  public void setNr_feature(String nr_feature) {

    this.nrFeature = nr_feature;
  }


  public String getNr_feature() {

    return this.nrFeature;
  }


  public void setLabel(String label) {

    this.label = label;
  }


  public String getLabel() {

    return this.label;
  }


  public void setOldIndexToNewIndexMap(HashMap<Integer, Integer> oldIndexToNewIndexMap) {

    this.oldIndexToNewIndexMap = oldIndexToNewIndexMap;
  }


  public HashMap<Integer, Integer> getOldIndexToNewIndexMap() {

    return this.oldIndexToNewIndexMap;
  }


  public void setWeights(double[] weights) {

    this.weights = weights;
  }


  public double[] getWeights() {

    return this.weights;
  }


  public void setAlpha(Alphabet alpha) {

    this.alpha = alpha;
  }


  public Alphabet getAlpha() {

    return this.alpha;
  }


  public void printToFile(String outputFile) throws IOException {

    FileOutputStream out = new FileOutputStream(outputFile);
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter fw = new BufferedWriter(or);
    fw.append(this.solverType);
    fw.append("\n");
    fw.append(this.nrClass);
    fw.append("\n");
    fw.append(this.label);
    fw.append("\n");
    fw.append(this.nrFeature);
    fw.append("\n");
    fw.append(this.bias);
    fw.append("\n");
    fw.append("w\n");
    if (this.numberOfClasses != 2) {
      for (int i = 0; i < this.weights.length / this.numberOfClasses; i++) {
        for (int c = 0; c < this.numberOfClasses; c++) {
          if (this.weights[i * this.numberOfClasses + c] == 0) {
            fw.append("0");
          } else {
            fw.append(String.valueOf(this.weights[i * this.numberOfClasses + c]));
          }
          fw.append(" ");
        }

        fw.append("\n");
      }
      fw.close();
    } else {
      for (int i = 0; i < this.weights.length; i++) {
        if (this.weights[i] == 0) {
          fw.append("0");
        } else {
          fw.append(String.valueOf(this.weights[i]));
        }
        fw.append(" ");
        fw.append("\n");
      }
      fw.close();
    }
  }


  public void setNeverUsed(HashSet<Integer> neverUsed) {

    this.neverUsed = neverUsed;
  }


  public HashSet<Integer> getNeverUsed() {

    return this.neverUsed;
  }


  public void secureOldAlphabet(String oldAlphaFile) throws IOException {

    this.alpha.writeToFile(oldAlphaFile);
  }


  public void setLowWeighted(HashSet<Integer> lowWeighted) {

    this.lowWeighted = lowWeighted;
  }


  public HashSet<Integer> getLowWeighted() {

    return this.lowWeighted;
  }


}
