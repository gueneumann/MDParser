package de.dfki.lt.mdparser.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class SplitWorker {

  private Map<Integer, String> posMap;
  private Map<String, PrintWriter> splitMap;


  public SplitWorker(Map<Integer, String> posMap, Map<String, PrintWriter> splitMap) {

    this.posMap = posMap;
    this.splitMap = splitMap;
  }


  public void processFile(File file) {

    long threadId = Thread.currentThread().getId();
    System.out.println("Hello from Thread in SplitWorkerThread " + threadId);
    System.out.println("processing " + file);

    try (BufferedReader in = Files.newBufferedReader(
        file.toPath(), StandardCharsets.UTF_8)) {
      String line;
      while ((line = in.readLine()) != null) {
        String[] lineArray = line.split(" ");
        String splitVal = null;
        Integer splitIndex = 0;
        int fIndex = 1;
        while (splitVal == null) {
          int index = Integer.valueOf(lineArray[fIndex].split(":")[0]);
          String pos = this.posMap.get(index);
          if (pos != null) {
            splitVal = pos;
            splitIndex = index;
            // breaks, because splitVal is no longer null
          } else {
            fIndex++;
          }
        }
        synchronized (this) {
          PrintWriter curBw = this.splitMap.get(splitVal);
          if (curBw == null) {
            curBw = new PrintWriter(Files.newBufferedWriter(
                Paths.get("splitF/" + splitIndex + ".txt"), StandardCharsets.UTF_8));
            this.splitMap.put(splitVal, curBw);
          }
          curBw.println(line);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    System.out.println("    Thread " + threadId + " has finished.");
  }
}
