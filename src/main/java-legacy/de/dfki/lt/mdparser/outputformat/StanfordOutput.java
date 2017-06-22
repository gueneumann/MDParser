package de.dfki.lt.mdparser.outputformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import de.dfki.lt.mdparser.data.Sentence;

public class StanfordOutput {

  private String taggedOutput;
  private String parsedOutput;


  public StanfordOutput(String inputString)
      throws IOException {
    BufferedReader br = new BufferedReader(new StringReader(inputString));
    StringBuilder sbt = new StringBuilder();
    StringBuilder sbp = new StringBuilder();
    String line;
    List<String> curSent = new ArrayList<String>();
    List<Sentence> sentences = new ArrayList<Sentence>();
    while ((line = br.readLine()) != null) {
      if (line.length() > 0) {
        curSent.add(line);
      } else {
        String[][] sentArray = new String[curSent.size()][8];
        for (int i = 0; i < curSent.size(); i++) {
          String[] curWord = curSent.get(i).split("\t");
          for (int j = 0; j < 8; j++) {
            sentArray[i][j] = curWord[j];
          }
        }
        sentences.add(new Sentence(sentArray));
        curSent = new ArrayList<String>();
      }
    }
    for (int i = 0; i < sentences.size(); i++) {
      Sentence curS = sentences.get(i);
      String[][] parsedSentence = curS.getSentArray();
      if (parsedSentence.length > 2) {
        int n = 0;
        StringBuilder sb2 = new StringBuilder();
        StringBuilder sb3 = new StringBuilder();
        for (int k = 0; k < parsedSentence.length; k++) {
          String label = parsedSentence[k][7];
          String parentIndex = parsedSentence[k][6];
          String childWord = parsedSentence[k][1];
          String pos = parsedSentence[k][3];
          if (Integer.valueOf(parentIndex) != 0 && !label.equals("ROOT") && !label.equals("Unk")
              && !label.equals("PUNCT")) {
            String parentWord = parsedSentence[Integer.valueOf(parentIndex) - 1][1];
            sb2.append(label + "(" + parentWord + "-" + parentIndex + ", " + childWord + "-" + i + ")\n");
            sb3.append(childWord + "_" + pos + "\n");
            n++;
          }
        }

        if (n > 0) {
          sbp.append(sb2.toString() + "\n");
          sbt.append(sb3.toString() + "\n");
        }
      }
    }
    this.parsedOutput = sbp.toString();
    this.taggedOutput = sbt.toString();
  }


  public void setTaggedOutput(String taggedOutput) {

    this.taggedOutput = taggedOutput;
  }


  public String getTaggedOutput() {

    return this.taggedOutput;
  }


  public void setParsedOutput(String parsedOutput) {

    this.parsedOutput = parsedOutput;
  }


  public String getParsedOutput() {

    return this.parsedOutput;
  }
}
