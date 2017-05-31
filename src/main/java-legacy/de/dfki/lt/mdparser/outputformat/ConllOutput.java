package de.dfki.lt.mdparser.outputformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class ConllOutput {

  private String output;


  public ConllOutput(String inputString)
      throws IOException {
    BufferedReader br = new BufferedReader(new StringReader(inputString));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      if (line.length() > 0) {
        String[] lineArray = line.split("\t");
        for (int i = 0; i < 7; i++) {
          sb.append(lineArray[i] + "\t");
        }
        sb.append(lineArray[7] + "\n");
      } else {
        sb.append("\n");
      }
    }
    this.output = sb.toString();
  }


  public void setOutput(String output) {

    this.output = output;
  }


  public String getOutput() {

    return this.output;
  }
}
