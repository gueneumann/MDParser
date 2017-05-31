package cases;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import com.schmeier.posTagger.focus.Focus;
import com.schmeier.posTagger.tagger.Tagger;

import de.dfki.lt.mdparser.algorithm.CovingtonAlgorithm;
import de.dfki.lt.mdparser.algorithm.ParsingAlgorithm;
import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.CovingtonFeatureModel;
import de.dfki.lt.mdparser.features.FeatureExtractor;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.outputformat.ConllOutput;
import de.dfki.lt.mdparser.outputformat.ConllXMLOutput;
import de.dfki.lt.mdparser.outputformat.StanfordOutput;
import de.dfki.lt.mdparser.outputformat.TripleOutput;
import de.dfki.lt.mdparser.outputformat.XMLString;
import de.dfki.lt.mdparser.parser.Parser;
import de.dfki.lt.mdparser.parser.Trainer;
import de.dfki.lt.mdparser.sentencesplitter.SSPredictor;
import edu.northwestern.at.morphadorner.corpuslinguistics.sentencesplitter.DefaultSentenceSplitter;
import edu.northwestern.at.morphadorner.corpuslinguistics.sentencesplitter.SentenceSplitter;
import edu.northwestern.at.morphadorner.corpuslinguistics.tokenizer.DefaultWordTokenizer;
import edu.northwestern.at.morphadorner.corpuslinguistics.tokenizer.WordTokenizer;

public class MDParser {

  /**
   * Index of the column for the word form in the output file.
   */
  private int wordFormIndex;

  /**
   * Index of the column for the part of speech in the output file.
   */
  private int posIndex;

  private SentenceSplitter sentenceSplitter;
  private WordTokenizer wordTokenizer;

  /**
   * POS-Tagger for German.
   */
  private Tagger germanTagger;

  /**
   * POS-Tagger for English.
   */
  private Tagger englishTagger;

  /**
   * Path to the English POS-Tagger.
   */
  private String modelFilePosTaggerEnglish;

  /**
   * Path to the German POS-Tagger.
   */
  private String modelFilePosTaggerGerman;

  private Parser parser;

  private Alphabet alphabetParser;

  private int length;
  private String[][][] tokenizedOutput;

  private String language;

  private SSPredictor mySentenceSplitter;


  public MDParser(Properties props)
      throws IOException {
    this.wordFormIndex = 1;
    this.posIndex = 4;
    String mode = props.getProperty("mode");
    if (!mode.equals("train")) {
      this.wordTokenizer = new DefaultWordTokenizer();
      this.sentenceSplitter = new DefaultSentenceSplitter();
      this.language = props.getProperty("language");
      String[] dirs = { "split", "splitA", "splitF", "splitO", "splitC", "splitModels", "temp" };


      Archivator arch = new Archivator(props.getProperty("modelsFile"), dirs);
      arch.extract();
      // GN: select POS-tagger and models
      System.err.println("Load tagging models ...");
      if (this.language.equals("english")) {
        this.modelFilePosTaggerEnglish = props.getProperty("modelFilePOSTaggerEnglish");
        this.englishTagger = new Tagger(this.modelFilePosTaggerEnglish);
        this.englishTagger.init();

      } else if (this.language.equals("german")) {
        this.modelFilePosTaggerGerman = props.getProperty("modelFilePOSTaggerGerman");
        this.germanTagger = new Tagger(this.modelFilePosTaggerGerman);
        this.germanTagger.init();
        // It seems they are not part of the archive

        //TODO GN: I changed this from arch.getInputStream("resources/ss/ssalpha.txt") because calling this
        // creates an error.
        InputStream[] modelFiles = {
            new FileInputStream("resources/ss/ssalpha.txt"),
            new FileInputStream("resources/ss/m.txt"),
            new FileInputStream("resources/ss/lc.txt"),
            new FileInputStream("resources/ss/ne.txt"),
            new FileInputStream("resources/ss/end.txt") };
        this.mySentenceSplitter = new SSPredictor(modelFiles, this.modelFilePosTaggerGerman);
      }
      System.err.println("... DONE!");

      // GN: initializes parser class and load models
      System.err.println("Load parsing models ...");
      this.parser = new Parser();
      this.alphabetParser = new Alphabet(arch.getParserAlphabetInputStream());
      this.parser.setNumberOfClassesParser(this.alphabetParser.getMaxLabelIndex() - 1);
      this.parser.readSplitModels(arch);
      System.err.println("... DONE!");
    }
  }


  public static String readFileToString(String textFile, String inputFormat) throws IOException {

    FileInputStream in = new FileInputStream(textFile);
    InputStreamReader ir = new InputStreamReader(in, "UTF8");
    BufferedReader fr = new BufferedReader(ir);
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = fr.readLine()) != null) {
      sb.append(line);
      if (inputFormat.equals("conll")) {
        sb.append("\n");
      }
    }
    fr.close();
    return sb.toString();
  }


  public static void printStringToFile(String string, String fileOutput) throws IOException {

    FileOutputStream out = new FileOutputStream(fileOutput);
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter fw = new BufferedWriter(or);
    fw.write(string);
    fw.close();
  }


  public static void checkProps(Properties props) {

    //input
    String inputType = props.getProperty("inputType");
    if (!inputType.equals("dir") && !inputType.equals("file")) {
      System.out.println("Possible values for the property 'inputType': 'dir' or 'file'");
      System.exit(0);
    } else {
      String inputFile = props.getProperty("inputFile");
      if (inputType.equals("dir")) {
        File dir = new File(inputFile);
        if (!dir.isDirectory()) {
          System.out.println("Specified value for the property 'inputFile' is not a directory");
          System.exit(0);
        }
      } else if (inputType.equals("file")) {
        File file = new File(inputFile);
        if (!file.exists()) {
          System.out.println("Specified file for the property 'inputFile' does not exist");
          System.exit(0);
        } else if (file.isDirectory()) {
          System.out.println("Specified value for the property 'inputFile' is a directory (should be a file)");
          System.exit(0);
        }
      }
    }
    //input format
    String inputFormat = props.getProperty("inputFormat");
    if (!inputFormat.equals("text") && !inputFormat.equals("conll")) {
      System.out.println("Possible values for the property 'inputFormat': 'text' or 'conll'");
      System.exit(0);
    }
    //language
    String language = props.getProperty("language");
    if (!language.equals("german") && !language.equals("english")) {
      System.out.println("Possible values for the property 'language': 'german' or 'english'");
      System.exit(0);
    }
    //outputformat
    String outputFormat = props.getProperty("outputFormat");
    if (!outputFormat.equals("conll") && !outputFormat.equals("conllxml") && !outputFormat.equals("stanford")
        && !outputFormat.equals("triple")) {
      System.out
          .println("Possible values for the property 'outputFormat': 'conll', 'conllxml', 'stanford' or 'triple'");
      System.exit(0);
    }
    //mode
    String mode = props.getProperty("mode");
    if (!mode.equals("parse") && !mode.equals("tag") && !mode.equals("train")) {
      System.out.println("Possible values for the property 'mode': 'parse','tag' or 'train'");
      System.exit(0);
    } else {
      if (mode.equals("tag")) {
        String taggedFile = props.getProperty("taggedFile");
        File file = new File(taggedFile);
        if (file.isDirectory()) {
          System.out.println("Specified value for the property 'taggedFile' is a directory (should be a file)");
          System.exit(0);
        }
      }
    }
  }


  public String tag(String in, Tagger t) {

    Focus focus = new Focus();
    StringTokenizer str = new StringTokenizer(in);
    while (str.hasMoreTokens()) {
      String word = str.nextToken();
      if (word.endsWith(".") || word.endsWith("?")
          || word.endsWith(":")
          || word.endsWith(";")
          || word.endsWith("!")
          || word.endsWith(",")
          || word.endsWith(".)")) {
        int wordLength = word.length();
        String pref = "";
        String suff = "";
        if (wordLength != 1) {
          pref = word.substring(0, wordLength - 1);
          suff = word.substring(wordLength - 1, wordLength);
        } else {
          suff = word;
        }
        focus.add(pref);
        focus.add(suff);
      } else {
        focus.add(word);
      }

    }
    t.run(focus);
    return focus.toString();
  }


  public String tagText(String text, String languageParam, String inputFormat)
      throws IOException {

    long start = System.currentTimeMillis();
    Tagger tagger = null;
    if (languageParam.equals("german")) {
      tagger = this.germanTagger;
    } else if (languageParam.equals("english")) {
      tagger = this.englishTagger;
    }
    this.length = 0;
    List<List<String>> sentences = null;
    String[][] sentencesArray = null;
    List<String> taggedList = new ArrayList<String>();
    StringBuilder sb = new StringBuilder();
    if (inputFormat.equals("text")) {
      if (languageParam.equals("english")) {
        sentences = this.sentenceSplitter.extractSentences(text, this.wordTokenizer);
      } else if (languageParam.equals("german")) {
        sentences = this.mySentenceSplitter.predict(text);
      } else {
        System.err.println("unknown language " + this.language);
        return "";
      }
      sentencesArray = new String[sentences.size()][];
      for (int i = 0; i < sentences.size(); i++) {
        StringBuilder sbSentence = new StringBuilder();
        List<String> sentence = sentences.get(i);
        for (int j = 0; j < sentence.size(); j++) {
          sbSentence.append(sentence.get(j));
          if ((j + 1) < sentence.size()) {
            sbSentence.append(" ");
          }
        }

        String sentenceString = sbSentence.toString();
        String taggedSentenceString = tag(sentenceString, tagger);
        taggedList.add(taggedSentenceString);
        String[] taggedSentenceArray = taggedSentenceString.split("\\s+");
        sentencesArray[i] = new String[taggedSentenceArray.length];
      }
    } else if (inputFormat.equals("conll")) {
      BufferedReader br = new BufferedReader(new StringReader(text));
      String line;
      sentences = new ArrayList<List<String>>();
      List<String> sentence = new ArrayList<String>();
      while ((line = br.readLine()) != null) {
        if (line.length() > 0) {
          String[] lineArray = line.split("\t");
          sentence.add(lineArray[this.wordFormIndex] + ":" + lineArray[this.posIndex]);
        } else {
          sentences.add(sentence);
          sentence = new ArrayList<String>();
        }
      }
      if (!sentence.isEmpty()) {
        sentences.add(sentence);
      }
      sentencesArray = new String[sentences.size()][];
      for (int i = 0; i < sentences.size(); i++) {
        List<String> curSentence = sentences.get(i);
        String[] sentenceArray = new String[curSentence.size()];
        sentencesArray[i] = curSentence.toArray(sentenceArray);
      }

    } else {
      System.err.println("unknown format " + inputFormat);
      return "";
    }

    this.tokenizedOutput = new String[sentences.size()][][];
    for (int i = 0; i < sentencesArray.length; i++) {
      String[] curSentence = sentencesArray[i];
      this.tokenizedOutput[i] = new String[curSentence.length][10];
      String taggedSentenceString = null;
      String[] taggedSentenceArray = null;
      if (inputFormat.equals("text")) {
        taggedSentenceString = taggedList.get(i);
        taggedSentenceArray = taggedSentenceString.split("\\s+");

      } else if (inputFormat.equals("conll")) {
        taggedSentenceArray = curSentence;
      } else {
        System.err.println("unkown input format " + inputFormat);
        return "";
      }
      for (int j = 0; j < curSentence.length; j++) {
        String curEntry = taggedSentenceArray[j];
        int splitPoint = curEntry.lastIndexOf(":");
        String wordForm = curEntry.substring(0, splitPoint);
        String pos = curEntry.substring(splitPoint + 1, curEntry.length());
        this.tokenizedOutput[i][j][0] = String.valueOf(j + 1);
        sb.append(this.tokenizedOutput[i][j][0] + "\t");
        XMLString wfXml = new XMLString(wordForm);
        this.tokenizedOutput[i][j][this.wordFormIndex] = wfXml.getXmlString();
        this.tokenizedOutput[i][j][this.posIndex - 1] = pos;
        this.tokenizedOutput[i][j][this.posIndex] = pos;
        sb.append(this.tokenizedOutput[i][j][this.wordFormIndex] + "\t");
        sb.append("_\t");
        sb.append(this.tokenizedOutput[i][j][this.posIndex] + "\t");
        sb.append(this.tokenizedOutput[i][j][this.posIndex] + "\t");
        sb.append("_\t_\n");
        this.length++;
      }
      sb.append("\n");
    }
    System.err.printf("Tagged %d words at %.2f words per second.\n",
        this.length, (Double.valueOf(this.length) / (Double.valueOf(System
            .currentTimeMillis()
            - start) / 1000)));
    return sb.toString();
  }


  public String tagSentence(String text, String languageParam, String inputFormat)
      throws IOException {

    long start = System.currentTimeMillis();
    Tagger tagger = null;
    if (languageParam.equals("german")) {
      tagger = this.germanTagger;
    } else if (languageParam.equals("english")) {
      tagger = this.englishTagger;
    }
    this.length = 0;
    StringBuilder sb = new StringBuilder();
    String sentenceString = text;
    String taggedSentenceString = null;
    String[] sentenceArray = null;
    if (inputFormat.equals("text")) {
      taggedSentenceString = tag(sentenceString, tagger);
      sentenceArray = taggedSentenceString.split("\\s+");
    } else if (inputFormat.equals("conll")) {
      BufferedReader br = new BufferedReader(new StringReader(text));
      String line;
      List<String> sentence = new ArrayList<String>();
      while ((line = br.readLine()) != null) {
        if (line.length() > 0) {
          String[] lineArray = line.split("\t");
          sentence.add(lineArray[this.wordFormIndex] + ":" + lineArray[this.posIndex]);
        }
      }
      sentenceArray = new String[sentence.size()];
      sentenceArray = sentence.toArray(sentenceArray);
    } else {
      System.err.println("unkown format " + inputFormat);
      return "";
    }
    this.tokenizedOutput = new String[1][][];
    this.tokenizedOutput[0] = new String[sentenceArray.length][10];
    this.length += sentenceArray.length;
    for (int j = 0; j < sentenceArray.length; j++) {
      String curEntry = sentenceArray[j];
      int splitPoint = curEntry.lastIndexOf(":");
      String wordForm = curEntry.substring(0, splitPoint);
      String pos = curEntry.substring(splitPoint + 1, curEntry.length());
      this.tokenizedOutput[0][j][0] = String.valueOf(j + 1);
      sb.append(this.tokenizedOutput[0][j][0] + "\t");
      XMLString wfXml = new XMLString(wordForm);
      this.tokenizedOutput[0][j][this.wordFormIndex] = wfXml.getXmlString();
      sb.append(this.tokenizedOutput[0][j][this.wordFormIndex] + "\t_\t");
      this.tokenizedOutput[0][j][this.posIndex - 1] = pos;
      this.tokenizedOutput[0][j][this.posIndex] = pos;
      sb.append(this.tokenizedOutput[0][j][this.posIndex]
          + "\t" + this.tokenizedOutput[0][j][this.posIndex] + "\t_\t_\t_\n");
    }
    sb.append("\n");
    System.err.printf("Tagged %d words at %.2f words per second.\n",
        this.length, (Double.valueOf(this.length) / (Double.valueOf(System
            .currentTimeMillis()
            - start) / 1000)));
    return sb.toString();
  }


  public String parseSentence(String text, String languageParam, String inputFormat)
      throws IOException {

    tagSentence(text, languageParam, inputFormat);
    FeatureExtractor fe = new FeatureExtractor();
    FeatureModel fm = new CovingtonFeatureModel(this.alphabetParser, fe);
    ParsingAlgorithm pa = new CovingtonAlgorithm();
    pa.setParser(this.parser);
    Sentence sent = new Sentence(this.tokenizedOutput[0]);
    pa.processCombined(sent, fm, true, this.parser.getSplitMap());
    return sent.toString();
  }


  public String parseText(String text, String languageParam, String inputFormat)
      throws IOException {

    long startComplete = System.currentTimeMillis();
    tagText(text, languageParam, inputFormat);

    this.length = 0;
    long startParse = System.currentTimeMillis();

    FeatureExtractor fe = new FeatureExtractor();
    FeatureModel fm = new CovingtonFeatureModel(this.alphabetParser, fe);
    ParsingAlgorithm pa = new CovingtonAlgorithm();
    pa.setParser(this.parser);
    StringBuilder sb = new StringBuilder();
    // GN: tokenizedOutput is basically the chart !
    for (int i = 0; i < this.tokenizedOutput.length; i++) {
      Sentence sent = new Sentence(this.tokenizedOutput[i]);
      this.length += sent.getSentArray().length;
      //GN: run parser on selected parser
      pa.processCombined(sent, fm, true, this.parser.getSplitMap());
      sb.append(sent.toString() + "\n");
    }
    Double secondsComplete = Double.valueOf(System.currentTimeMillis() - startComplete) / 1000;
    System.err.printf(
        "Complete pipeline: %.2f seconds. Average sentence length %.2f. "
        + "Parsed %d words at %.2f words per second (%d sentences, %,.2f sentences per second).\n",
        secondsComplete,
        Double.valueOf(this.length) / Double.valueOf(this.tokenizedOutput.length),
        this.length,
        Double.valueOf(this.length) / secondsComplete,
        this.tokenizedOutput.length,
        Double.valueOf(this.tokenizedOutput.length) / secondsComplete);

    Double secondsParse = Double.valueOf(System.currentTimeMillis() - startParse) / 1000;
    System.err.printf(
        "     Only parsing: %.2f seconds. Average sentence length %.2f. "
        + "Parsed %d words at %.2f words per second (%d sentences, %,.2f sentences per second).\n",
        secondsParse,
        Double.valueOf(this.length) / Double.valueOf(this.tokenizedOutput.length),
        this.length,
        Double.valueOf(this.length) / secondsParse,
        this.tokenizedOutput.length,
        Double.valueOf(this.tokenizedOutput.length) / secondsParse);
    return sb.toString();
  }


  public static void main(String[] args) {

    try {
      Properties props = new Properties();
      FileInputStream in = null;
      // GN: read properties
      if (args.length == 1) {
        in = new FileInputStream(new File(args[0]));
      } else {
        in = new FileInputStream(new File("resources/props/props.xml"));
      }
      props.loadFromXML(in);
      checkProps(props);
      MDParser mdp = new MDParser(props);
      String fileName = props.getProperty("inputFile");
      String inputType = props.getProperty("inputType");
      String inputFormat = props.getProperty("inputFormat");
      File[] allFiles = null;
      if (inputType.equals("file")) {
        allFiles = new File[1];
        allFiles[0] = new File(fileName);
      } else if (inputType.equals("dir")) {
        allFiles = new File(fileName).listFiles();
      } else {
        System.err.println("unknown input type " + inputType);
        return;
      }
      for (int i = 0; i < allFiles.length; i++) {
        fileName = allFiles[i].getPath();
        String outputFile = props.getProperty("outputFile");
        String taggedFile = props.getProperty("taggedFile");
        if (inputType.equals("dir")) {
          outputFile = String.format("%s/%04d.txt", outputFile, i);
          taggedFile = String.format("%s/%04d_morph.txt", taggedFile, i);
        }
        String inputString = readFileToString(fileName, inputFormat);
        String parsed = "";
        String mode = props.getProperty("mode");
        System.err.println("------ Tag and Parse input file ...");

        if (mode.equals("parse")) {
          parsed = mdp.parseText(inputString, mdp.language, inputFormat);
          String outputFormat = props.getProperty("outputFormat");
          String output = "";
          if (outputFormat.equals("stanford")) {
            String stanfordMorphString = new StanfordOutput(parsed).getTaggedOutput();
            String stanfordParsedString = new StanfordOutput(parsed).getParsedOutput();
            printStringToFile(stanfordMorphString, taggedFile);
            printStringToFile(stanfordParsedString, outputFile);
          } else {
            if (outputFormat.equals("conll")) {
              output = new ConllOutput(parsed).getOutput();
            } else if (outputFormat.equals("conllxml")) {
              output = new ConllXMLOutput(parsed).getOutput();
            } else if (outputFormat.equals("triple")) {
              output = new TripleOutput(parsed).getOutput();
            }
            printStringToFile(output, outputFile);
            System.err.println("------ ... DONE!");
          }
        } else if (mode.equals("tag")) {
          System.err.println("------ Tag input file ...");

          parsed = mdp.tagText(inputString, mdp.language, inputFormat);
          printStringToFile(parsed, taggedFile);
          System.err.println("------ ... DONE!");
        } else if (mode.equals("train")) {
          Trainer trainer = new Trainer();
          String[] dirs = { "split", "splitA", "splitF", "splitO", "splitC", "splitModels", "temp" };
          String splitModelsDir = "splitModels";
          String algorithm = "covington";
          String splitFile = "temp/split.txt";
          TrainerTest.deleteOld(dirs);
          TrainerTest.createNew(dirs);
          String inputFile = props.getProperty("inputFile");
          String alphabetFileParser = "temp/alphaParser.txt";
          String alphabetFileLabeler = "temp/alphaLabeler.txt";
          trainer.createAndTrainWithSplittingFromDisk(algorithm, inputFile, splitModelsDir, alphabetFileParser,
              alphabetFileLabeler, splitFile);
          Archivator arch = new Archivator(props.getProperty("modelsFile"), dirs);
          arch.pack();
          arch.delTemp();
          TrainerTest.deleteOld(dirs);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
