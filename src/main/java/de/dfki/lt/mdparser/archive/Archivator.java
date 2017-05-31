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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Archivator {

  private String splitModelsDir;
  private String splitAlphabetsDir;
  private String splitModelsLDir;
  private String alphabetParser;
  private String alphabetLabeler;
  private String splitFile;
  private String splitLFile;

  private String archiveName;

  HashMap<String, InputStream> archiveMap;


  public Archivator(String archiveName, String[] dirs)
      throws IOException {
    this.archiveName = archiveName;
    this.archiveMap = new HashMap<String, InputStream>();
    this.checkDirs(dirs);
    setAlphabetParser("temp/alphaParser.txt");
    setSplitFile("temp/split.txt");
    setSplitModelsDir("splitModels");
    setSplitAlphabetsDir("splitA");
  }


  public HashMap<String, InputStream> getArchiveMap() {

    return this.archiveMap;
  }


  public void checkDirs(String[] dirs) {

    for (int i = 0; i < dirs.length; i++) {
      File curDir = new File(dirs[i]);
      if (!curDir.exists()) {
        curDir.mkdir();
      }
    }
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


  @Override
  public String toString() {

    return this.archiveMap.toString();
  }


  public InputStream getInputStream(String key) {

    return this.archiveMap.get(key);
  }


  public InputStream getParserAlphabetInputStream() {

    return this.archiveMap.get("temp/alphaParser.txt");
  }


  public InputStream getLabelerAlphabetInputStream() {

    return this.archiveMap.get("temp/alphaLabeler.txt");
  }


  public InputStream getSplitFileInputStream() {

    return this.archiveMap.get("temp/split.txt");
  }


  public InputStream getSplitLFileInputStream() {

    return this.archiveMap.get("temp/splitL.txt");
  }


  public void setSplitModelsDir(String splitModelsDir) {

    this.splitModelsDir = splitModelsDir;
  }


  public String getSplitModelsDir() {

    return this.splitModelsDir;
  }


  public void setSplitModelsLDir(String splitModelsLDir) {

    this.splitModelsLDir = splitModelsLDir;
  }


  public String getSplitModelsLDir() {

    return this.splitModelsLDir;
  }


  public void setAlphabetParser(String alphabetParser) {

    this.alphabetParser = alphabetParser;
  }


  public String getAlphabetParser() {

    return this.alphabetParser;
  }


  public void setAlphabetLabeler(String alphabetLabeler) {

    this.alphabetLabeler = alphabetLabeler;
  }


  public String getAlphabetLabeler() {

    return this.alphabetLabeler;
  }


  public void setSplitFile(String splitFile) {

    this.splitFile = splitFile;
  }


  public String getSplitFile() {

    return this.splitFile;
  }


  public void setSplitLFile(String splitLFile) {

    this.splitLFile = splitLFile;
  }


  public String getSplitLFile() {

    return this.splitLFile;
  }


  public void setArchiveName(String archiveName) {

    this.archiveName = archiveName;
  }


  public String getArchiveName() {

    return this.archiveName;
  }


  public void setSplitAlphabetsDir(String splitAlphabetsDir) {

    this.splitAlphabetsDir = splitAlphabetsDir;
  }


  public String getSplitAlphabetsDir() {

    return this.splitAlphabetsDir;
  }
}
