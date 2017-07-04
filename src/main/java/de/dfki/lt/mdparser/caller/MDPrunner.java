package de.dfki.lt.mdparser.caller;


import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import de.dfki.lt.mdparser.data.LinearizedSentence;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.eval.Eval;
import de.dfki.lt.mdparser.parser.Parser;

public final class MDPrunner {

  private MDPrunner() {

    // private constructor to enforce noninstantiability
  }


  public static Eval parseAndEvalConllFile(String conllFileName, String resultFileName, String modelFileName)
      throws IOException {

    Parser parser = new Parser(modelFileName);
    List<Sentence> sentencesList = parser.parse(conllFileName);
    writeSentences(sentencesList, resultFileName);
    return new Eval(conllFileName, resultFileName, 6, 6, 7, 7);
  }


  public static void parseConllFileAndLinearize(String conllFileName, String resultFileName, String modelFileName)
      throws IOException {

    Parser parser = new Parser(modelFileName);
    List<Sentence> sentencesList = parser.parse(conllFileName);
    writeLinearizedSentences(sentencesList, resultFileName);
  }


  public static void writeSentences(List<Sentence> sentencesList, String resultFileName)
      throws IOException {

    writeSentences(sentencesList, resultFileName, StandardCharsets.UTF_8);
  }


  public static void writeSentences(List<Sentence> sentencesList, String resultFileName, Charset encoding)
      throws IOException {

    try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(
        Paths.get(resultFileName), encoding))) {
      for (Sentence oneSent : sentencesList) {
        out.println(oneSent);
      }
    }
  }


  public static void writeLinearizedSentences(List<Sentence> sentencesList, String resultFileName)
      throws IOException {

    writeLinearizedSentences(sentencesList, resultFileName, StandardCharsets.UTF_8);
  }


  public static void writeLinearizedSentences(List<Sentence> sentencesList, String resultFileName, Charset encoding)
      throws IOException {

    try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(
        Paths.get(resultFileName), encoding))) {
      for (Sentence oneSentence : sentencesList) {
        LinearizedSentence linearizedSentence = new LinearizedSentence(oneSentence);
        linearizedSentence.linearizedDependencyStructure();
        List<String> linSenString = linearizedSentence.getLinearizedSentence();
        out.println(linSenString);
      }
    }
  }
}
