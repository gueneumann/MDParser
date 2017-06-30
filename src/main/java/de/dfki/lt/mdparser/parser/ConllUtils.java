package de.dfki.lt.mdparser.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import de.dfki.lt.mdparser.data.Sentence;

public final class ConllUtils {

  private ConllUtils() {

    // private constructor to enforce noninstantiability
  }


  public static List<Sentence> readConllFile(String conllFileName, boolean train)
      throws IOException {

    int infoSize = 12;

    List<Sentence> sentencesList = new ArrayList<Sentence>();

    try (BufferedReader in = Files.newBufferedReader(
        Paths.get(conllFileName), StandardCharsets.UTF_8)) {
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
          String[][] sentArray = new String[curSent.size()][infoSize];
          for (int i = 0; i < curSent.size(); i++) {
            String[] curWord = curSent.get(i).split("\\s");

            //System.err.println("Label: " + curWord[7]);

            for (int j = 0; j < curWord.length; j++) {
              if (!train && (j == 6 || j == 7 || j == 8 || j == 9)) {
                sentArray[i][j] = null;
              } else if (train && (j == 8 || j == 9)) {
                sentArray[i][j] = null;
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

    return sentencesList;
  }
}
