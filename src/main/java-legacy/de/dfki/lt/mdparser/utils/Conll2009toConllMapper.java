package de.dfki.lt.mdparser.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public final class Conll2009toConllMapper {

  private static final String SOURCE_DIR = "/Users/gune00/data/MLDP/2009/";
  private static final String target_DIR = "resources/input/";


  private Conll2009toConllMapper() {

    // private constructor to enforce noninstantiability
  }


  private static void transformConll2009ToConllFile(String sourceFileName, String targetFileName)
      throws IOException {

    String sourceEncoding = "utf-8";
    String targetEncoding = "utf-8";
    // init reader for CONLL style file
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(
            new FileInputStream(sourceFileName),
            sourceEncoding));

    // init writer for line-wise fileßs
    BufferedWriter writer = new BufferedWriter(
        new OutputStreamWriter(
            new FileOutputStream(targetFileName),
            targetEncoding));

    String line = "";
    while ((line = reader.readLine()) != null) {
      if (line.isEmpty()) {
        writer.newLine();
      } else {
        if (!line.startsWith("#")) {
          String[] sourceLine = line.split("\t");
          StringBuilder sb = new StringBuilder();

          // $1, $2, "_", $5, $6, "_", $9, $11, "_", "_"
          sb.append(
              sourceLine[0] + "\t" + sourceLine[1] + "\t" + "_" + "\t"
                  + sourceLine[4] + "\t" + sourceLine[5] + "\t" + "_" + "\t"
                  + sourceLine[8] + "\t" + sourceLine[10] + "\t" + "_" + "\t"
                  + "_" + "\t");
          writer.write(sb.toString());
          writer.newLine();
        }
      }
    }
    reader.close();
    writer.close();
  }


  public static void main(String[] args) throws IOException {

    Conll2009toConllMapper.transformConll2009ToConllFile(
        Conll2009toConllMapper.SOURCE_DIR + "conll09-german/german.train",
        Conll2009toConllMapper.target_DIR + "de-train-2009.conll");
    Conll2009toConllMapper.transformConll2009ToConllFile(
        Conll2009toConllMapper.SOURCE_DIR + "conll09-german/german.in.test.gold",
        Conll2009toConllMapper.target_DIR + "de-test-2009.conll");

    Conll2009toConllMapper.transformConll2009ToConllFile(
        Conll2009toConllMapper.SOURCE_DIR + "conll09-english/english.train",
        Conll2009toConllMapper.target_DIR + "en-train-2009.conll");
    Conll2009toConllMapper.transformConll2009ToConllFile(
        Conll2009toConllMapper.SOURCE_DIR + "conll09-english/english.in.test.gold",
        Conll2009toConllMapper.target_DIR + "en-test-2009.conll");
  }
}
