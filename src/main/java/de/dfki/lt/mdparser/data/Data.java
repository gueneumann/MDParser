package de.dfki.lt.mdparser.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Data {

  private Sentence[] sentences;

  private int infoSize = 12;


  public Data(String inputFileName, boolean train)
      throws IOException {

    List<Sentence> sentencesList = new ArrayList<Sentence>(40000);

    try (BufferedReader in = Files.newBufferedReader(
        Paths.get(inputFileName), StandardCharsets.UTF_8)) {
      List<String> curSent = new ArrayList<String>(50);
      String line;
      while ((line = in.readLine()) != null) {
        if (line.length() > 0) {
          // XXX
          // GN: added by GN on 30.06.2015:
          // if first element is not a single number, but contains an interval - as it
          // is the case for Universal grammars - ignore such a line
          if (line.split("\t")[0].contains("-")) {
            // System.err.println("Skip: " + line);
            continue;
          }
          curSent.add(line);
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
          sentencesList.add(new Sentence(sentArray));
          curSent = new ArrayList<String>();
        }
      }
    }

    this.sentences = new Sentence[sentencesList.size()];
    for (int i = 0; i < sentencesList.size(); i++) {
      this.sentences[i] = sentencesList.get(i);
    }
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


  public void writeToFile(String resultFileName) throws IOException {

    try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(
        Paths.get(resultFileName), StandardCharsets.UTF_8))) {
      for (Sentence oneSent : this.sentences) {
        out.println(oneSent);
      }
    }
  }
}
