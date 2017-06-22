package de.dfki.lt.mdparser;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;


public final class Utils {

  private Utils() {

    // private constructor to enforce noninstantiability
  }


  public static void deleteFolder(Path path)
      throws IOException {

    try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException {

          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }


        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
            throws IOException {

          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (NoSuchFileException e) {
      // nothing to do, file already deleted
    }
  }


  public static List<Path> getAllFilesFromFolder(Path path, String glob)
      throws IOException {

    List<Path> pathList = new ArrayList<>();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path, glob)) {
      for (Path onePath : directoryStream) {
        pathList.add(onePath);
      }
    }
    return pathList;
  }
}
