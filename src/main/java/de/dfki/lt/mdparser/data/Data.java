package de.dfki.lt.mdparser.data;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class Data {

  private Sentence[] sentences;

  private int infoSize = 12;


  public Data() {

  }


  public Data(String inputFile, boolean train)
      throws IOException {
    FileInputStream in = new FileInputStream(inputFile);
    BufferedInputStream bis = new BufferedInputStream(in, 8000);
    InputStreamReader ir = new InputStreamReader(bis, "UTF8");
    BufferedReader fr = new BufferedReader(ir);
    String line;
    List<String> curSent = new ArrayList<String>(50);
    List<Sentence> sentences = new ArrayList<Sentence>(40000);
    while ((line = fr.readLine()) != null) {
      if (line.length() > 0) {
        // XXX
        // GN: added by GN on 30.06.2015:
        // if first element is not a single number, but contains an interval - as it is the case for Universal grammars
        // ignore such a line
        if (!line.split("\t")[0].contains("-")) {
          curSent.add(line);
        }
        //else System.err.println("Skip: " + line);
      } else {
        String[][] sentArray = new String[curSent.size()][this.infoSize];
        for (int i = 0; i < curSent.size(); i++) {

          String[] curWord = curSent.get(i).split("\\s");


          //System.err.println("Label: " + curWord[7]);

          for (int j = 0; j < curWord.length; j++) {
            if (!train && (j == 6 || j == 7 || j == 8 || j == 9)) {
              sentArray[i][j] = "_";
            } else if (train && (j == 8 || j == 9)) {
              sentArray[i][j] = "_";
            } else {
              sentArray[i][j] = curWord[j];
            }
          }
        }
        sentences.add(new Sentence(sentArray));
        curSent = new ArrayList<String>();
      }
    }
    fr.close();
    this.sentences = new Sentence[sentences.size()];
    for (int i = 0; i < sentences.size(); i++) {
      this.sentences[i] = sentences.get(i);
    }
  }


  public void setSentences(Sentence[] sentences) {

    this.sentences = sentences;
  }


  public Sentence[] getSentences() {

    return this.sentences;
  }


  // TODO:
  // Create a aligned file set of pairs of <sentence, linearizedSentence>
  public void testLinearizedToFile(String resultFile) {

    for (int n = 0; n < this.sentences.length; n++) {
      Sentence s = this.sentences[n];
      LinearizedSentence linearizedSentence = new LinearizedSentence(s);

      linearizedSentence.linearizedDependencyStructure();
      List<String> linSenString = linearizedSentence.getLinearizedSentence();

      System.out.println(linSenString);
    }
  }


  public void printToFile(String resultFile) throws IOException {

    FileOutputStream out = new FileOutputStream(resultFile);
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter fw = new BufferedWriter(or);
    for (int n = 0; n < this.sentences.length; n++) {
      Sentence s = this.sentences[n];
      fw.append(s.toString());
      fw.append("\n");
    }
    fw.close();
  }

}
