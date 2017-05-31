package de.dfki.lt.mdparser.sentenceSplitter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SSEval {

  public SSEval() {

  }


  public List<String> readInput(String inputFile, String inputFormat) throws IOException {

    FileInputStream in = new FileInputStream(inputFile);
    InputStreamReader ir = new InputStreamReader(in, "UTF8");
    BufferedReader fr = new BufferedReader(ir);
    String line;
    StringBuilder sb = new StringBuilder();
    List<String> res = new ArrayList<String>();
    if (inputFormat.equals("conll")) {
      while ((line = fr.readLine()) != null) {
        if (line.length() > 0) {
          String word = line.split("\t")[1];
          sb.append(word);
        } else {
          res.add(sb.toString());
          sb = new StringBuilder();
        }
      }
      fr.close();
    }
    return res;
  }


  public List<String> readResult(String resultFile) throws IOException {

    FileInputStream in = new FileInputStream(resultFile);
    InputStreamReader ir = new InputStreamReader(in, "UTF8");
    BufferedReader fr = new BufferedReader(ir);
    List<String> res = new ArrayList<String>();
    String line;
    while ((line = fr.readLine()) != null) {
      res.add(line);
    }
    fr.close();
    return res;
  }


  public static void main(String[] args) throws IOException {

    SSEval eval = new SSEval();

    String language = args[0];
    String inputFile = "";
    if (language.equals("english")) {
      inputFile = "resources/input/english.devel";
    } else if (language.equals("german")) {
      inputFile = "resources/input/german_tiger_test.conll";
    }
    List<String> input = eval.readInput(inputFile, "conll");
    String resultFile = "temp/res.txt";
    List<String> result = eval.readResult(resultFile);

    eval.eval(input, result);
  }


  private void eval(List<String> input, List<String> result) {

    double correct = 0.0;
    for (int i = 0; i < result.size(); i++) {
      String curSent = result.get(i);
      if (input.contains(curSent)) {
        correct++;
      } else {
        System.out.println(curSent);
      }
    }
    double total = Double.valueOf(result.size());
    System.out.println("Correct: " + correct);
    System.out.println("Total: " + total);
    double rec = correct / input.size();
    double pr = correct / total;
    System.out.println("Precision: " + pr);
    System.out.println("Recall: " + rec);
    System.out.println("F-Measure: " + (2 * pr * rec) / (pr + rec));
  }
}
