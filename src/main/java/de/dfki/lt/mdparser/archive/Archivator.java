package de.dfki.lt.mdparser.archive;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import de.dfki.lt.mdparser.config.ConfigKeys;
import de.dfki.lt.mdparser.config.GlobalConfig;

public class Archivator {

  private String archiveName;


  public Archivator(String archiveName) {

    this.archiveName = archiveName;
  }


  public InputStream getInputStream(String entry)
      throws IOException {

    entry = entry.replaceAll("\\" + File.separator, "/");
    InputStream in = this.getClass().getClassLoader().getResourceAsStream(this.archiveName);
    // if can't be loaded from classpath, try to load it from the file system
    if (null == in) {
      in = Files.newInputStream(Paths.get(this.archiveName));
    }
    ZipInputStream zin = new ZipInputStream(in);
    for (ZipEntry e; (e = zin.getNextEntry()) != null;) {
      if (e.getName().equals(entry)) {
        return zin;
      }
    }
    throw new IOException(String.format("\"%s\" not found in archive", entry));
  }


  public void pack() throws IOException {

    List<Path> filesToPack = new ArrayList<>();
    filesToPack.add(GlobalConfig.ALPHA_FILE);
    filesToPack.add(GlobalConfig.SPLIT_FILE);
    try (
        DirectoryStream<Path> stream = Files.newDirectoryStream(GlobalConfig.SPLIT_MODELS_FOLDER)) {
      stream.forEach(filesToPack::add);
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(GlobalConfig.SPLIT_ALPHA_FOLDER)) {
      stream.forEach(filesToPack::add);
    }

    OutputStream dest =
        Files.newOutputStream(
            GlobalConfig.getPath(ConfigKeys.MODEL_OUTPUT_FOLDER).resolve(this.archiveName));
    ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(dest));

    for (Path onePath : filesToPack) {
      String zipEntry =
          GlobalConfig.getModelBuildFolder().relativize(onePath).normalize().toString();
      zipEntry = zipEntry.replaceAll("\\" + System.getProperty("file.separator"), "/");
      zipOut.putNextEntry(new ZipEntry(zipEntry));
      BufferedInputStream origin = new BufferedInputStream(Files.newInputStream(onePath));
      int count = 0;
      byte[] data = new byte[20480];
      while ((count = origin.read(data, 0, 20480)) != -1) {
        zipOut.write(data, 0, count);
      }
      count = 0;
      origin.close();
    }
    zipOut.close();
  }
}
