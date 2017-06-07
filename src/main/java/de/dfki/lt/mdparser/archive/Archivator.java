package de.dfki.lt.mdparser.archive;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Archivator {

  private String splitModelsDir;
  private String splitAlphabetsDir;
  private String alphabetParser;
  private String splitFile;

  private String archiveName;

  private Map<String, InputStream> archiveMap;


  public Archivator(String archiveName) {

    this.archiveName = archiveName;
    this.archiveMap = new HashMap<String, InputStream>();
    this.alphabetParser = "temp/alphaParser.txt";
    this.splitFile = "temp/split.txt";
    this.splitModelsDir = "splitModels";
    this.splitAlphabetsDir = "splitA";
  }


  public void delTemp() {

    File f = new File(this.splitFile);
    f.delete();
    f = new File(this.alphabetParser);
    f.delete();
    File[] files = new File(this.splitModelsDir).listFiles();
    for (int i = 0; i < files.length; i++) {
      files[i].delete();
    }
    files = new File("split").listFiles();
    for (int i = 0; i < files.length; i++) {
      files[i].delete();
    }
    f = new File("temp");
    if (f.listFiles().length == 0) {
      f.delete();
    }

  }


  public void pack() throws IOException {

    FileOutputStream dest = new FileOutputStream(this.archiveName);
    ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(dest));
    List<String> filesToPack = new ArrayList<String>();
    filesToPack.add(this.alphabetParser);
    filesToPack.add(this.splitFile);
    File[] models = new File(this.splitModelsDir).listFiles();
    for (int i = 0; i < models.length; i++) {
      filesToPack.add(models[i].getPath());
    }
    File[] alphabets = new File(this.splitAlphabetsDir).listFiles();
    for (int i = 0; i < alphabets.length; i++) {
      filesToPack.add(alphabets[i].getPath());
    }
    Iterator<String> iter = filesToPack.iterator();
    while (iter.hasNext()) {
      String curFile = iter.next();
      curFile = curFile.replaceAll("\\" + System.getProperty("file.separator"), "/");
      zip.putNextEntry(new ZipEntry(curFile));
      BufferedInputStream origin = new BufferedInputStream(new FileInputStream(curFile));
      int count = 0;
      byte[] data = new byte[20480];
      while ((count = origin.read(data, 0, 20480)) != -1) {
        zip.write(data, 0, count);
      }
      count = 0;
      origin.close();
    }
    zip.close();
  }


  public void extract() throws IOException {

    ZipFile zip = new ZipFile(this.archiveName);
    ZipInputStream zis = new ZipInputStream(new FileInputStream(this.archiveName));
    ZipEntry entry;
    while ((entry = zis.getNextEntry()) != null) {
      this.archiveMap.put(entry.getName(), zip.getInputStream(entry));
    }
  }


  public InputStream getInputStream(String key) {

    return this.archiveMap.get(key);
  }


  public InputStream getParserAlphabetInputStream() {

    return this.archiveMap.get("temp/alphaParser.txt");
  }


  public InputStream getSplitFileInputStream() {

    return this.archiveMap.get("temp/split.txt");
  }
}
