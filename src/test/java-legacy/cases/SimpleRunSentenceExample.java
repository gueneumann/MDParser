package cases;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class SimpleRunSentenceExample {

  public static void main(String[] args) {

    try {
      Properties props = new Properties();
      // Load properties from props.xml file
      FileInputStream in = new FileInputStream(new File("resources/props/props.xml"));
      props.loadFromXML(in);

      // Initialize parser and pos-tagger with properties
      MDParser mdp = new MDParser(props);

      // Choose an example sentence to parse
      String sentence =
          "Datenschützer warnen vor der Vorratsdatenspeicherung, "
          + "der Türkischen Gemeinde geht der Doppelpass-Kompromiss nicht weit genug.";

      //Call parser with <sentence>, <language>, <input format>

      System.out.println(mdp.parseSentence(sentence, props.getProperty("language"), props.getProperty("inputFormat")));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
