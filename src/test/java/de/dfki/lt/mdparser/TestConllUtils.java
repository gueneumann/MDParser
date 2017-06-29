package de.dfki.lt.mdparser;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.parser.ConllUtils;

public class TestConllUtils {

  public static void main(String[] args) {

    try {
      List<Sentence> sentences = ConllUtils.readConllFile("/Users/gune00/data/MLDP/2009/en-train-2009.conll", true);
      //Data d = new Data("input/english.train", true);
      countDifLabels(sentences);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  private static void countDifLabels(List<Sentence> sentences) {

    Set<String> labels = new HashSet<String>();
    for (Sentence oneSentence : sentences) {
      String[][] stringArray = oneSentence.getSentArray();
      for (int i = 0; i < stringArray.length; i++) {
        labels.add(stringArray[i][3]);
      }
    }
    System.out.println(labels + " " + labels.size());
  }
}
