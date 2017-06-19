package de.dfki.lt.mdparser.archive;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import de.dfki.lt.mdparser.config.GlobalConfig;

public class Archivator {

  private String archiveName;

  private Map<String, InputStream> archiveMap;


  public Archivator(String archiveName) {

    this.archiveName = archiveName;
    this.archiveMap = new HashMap<String, InputStream>();
  }


  public void pack() throws IOException {

    FileOutputStream dest = new FileOutputStream(this.archiveName);
    ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(dest));
    List<Path> filesToPack = new ArrayList<>();
    filesToPack.add(GlobalConfig.ALPHA_FILE);
    filesToPack.add(GlobalConfig.SPLIT_FILE);
    Files.newDirectoryStream(GlobalConfig.SPLIT_MODELS_FOLDER).forEach(x -> filesToPack.add(x));
    Files.newDirectoryStream(GlobalConfig.SPLIT_ALPHA_FOLDER).forEach(x -> filesToPack.add(x));
    for (Path onePath : filesToPack) {
      zip.putNextEntry(new ZipEntry(onePath.toString()));
      BufferedInputStream origin = new BufferedInputStream(Files.newInputStream(onePath));
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
      this.archiveMap.put(Paths.get(entry.getName()).toString(), zip.getInputStream(entry));
    }
  }


  public InputStream getInputStream(String key) {

    return this.archiveMap.get(key);
  }


  public InputStream getParserAlphabetInputStream() {

    return this.archiveMap.get(GlobalConfig.ALPHA_FILE.toString());
  }


  public InputStream getSplitFileInputStream() {

    return this.archiveMap.get(GlobalConfig.SPLIT_FILE.toString());
  }
}
