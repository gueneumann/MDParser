package de.dfki.lt.mdparser.xmlrpc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;

import com.schmeier.posTagger.tagger.Tagger;

import de.dfki.lt.mdparser.algorithm.CovingtonAlgorithm;
import de.dfki.lt.mdparser.algorithm.ParsingAlgorithm;
import de.dfki.lt.mdparser.algorithm.StackAlgorithm;
import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.CovingtonFeatureModel;
import de.dfki.lt.mdparser.features.FeatureExtractor;
import de.dfki.lt.mdparser.features.StackFeatureModel;
import de.dfki.lt.mdparser.parser.Parser;
import edu.northwestern.at.morphadorner.corpuslinguistics.sentencesplitter.DefaultSentenceSplitter;
import edu.northwestern.at.morphadorner.corpuslinguistics.tokenizer.DefaultWordTokenizer;

public final class MDPServer {

  /**
   * POS-Tagger for German.
   */
  private static Tagger germanTagger;

  /**
   * POS-Tagger for English.
   */
  private static Tagger englishTagger;

  /**
   * Path to the English POS-Tagger.
   */
  private static String modelFilePosTaggerEnglish;

  /**
   * Path to the German POS-Tagger.
   */
  private static String modelFilePosTaggerGerman;


  private static WebServer webServer;

  private static Parser parser;

  private static Alphabet alphabetParser;

  private static ParsingAlgorithm pa;


  private MDPServer() {

    // private constructor to enforce noninstantiability
  }


  public static void main(String[] args) throws InvalidPropertiesFormatException, IOException, XmlRpcException {

    Properties props = new Properties();
    FileInputStream in = null;
    if (args.length == 1) {
      in = new FileInputStream(new File(args[0]));
    } else {
      in = new FileInputStream(new File("propsServer.xml"));
    }
    props.loadFromXML(in);
    new DefaultWordTokenizer();
    new DefaultSentenceSplitter();
    String language = props.getProperty("language");
    if (language.equals("english")) {
      modelFilePosTaggerEnglish = props.getProperty("modelFilePOSTaggerEnglish");
      englishTagger = new Tagger(modelFilePosTaggerEnglish);
      englishTagger.init();
    } else if (language.equals("german")) {
      modelFilePosTaggerGerman = props.getProperty("modelFilePOSTaggerGerman");
      germanTagger = new Tagger(modelFilePosTaggerGerman);
      germanTagger.init();
    }
    parser = new Parser();
    String[] dirs = { "split", "splitA", "splitF", "splitO", "splitC", "splitModels", "temp" };
    Archivator arch = new Archivator(props.getProperty("modelsFile"), dirs);
    arch.extract();
    alphabetParser = new Alphabet(arch.getParserAlphabetInputStream());
    parser.setNumberOfClassesParser(alphabetParser.getMaxLabelIndex() - 1);
    parser.readSplitModels(arch);
    String algorithm = props.getProperty("algorithm");
    FeatureExtractor fe = new FeatureExtractor();
    if (algorithm.equals("covington")) {
      new CovingtonFeatureModel(alphabetParser, fe);
      pa = new CovingtonAlgorithm();
    } else if (algorithm.equals("stack")) {
      new StackFeatureModel(alphabetParser, fe);
      pa = new StackAlgorithm();
    }
    pa.setParser(parser);
    webServer = new WebServer(Integer.valueOf(props.getProperty("port")));
    webServer.getXmlRpcServer().setMaxThreads(10);
    PropertyHandlerMapping phm = new PropertyHandlerMapping();
    phm.addHandler("parser", de.dfki.lt.mdparser.xmlrpc.MDPServer.class);
    webServer.getXmlRpcServer().setHandlerMapping(phm);
    XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl)webServer.getXmlRpcServer().getConfig();
    serverConfig.setEnabledForExtensions(true);
    serverConfig.setContentLengthOptional(false);
    webServer.start();
    System.out.println("Server running...");
  }
}
