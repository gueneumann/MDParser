package de.dfki.lt.mdparser.caller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.eval.Eval;
import de.dfki.lt.mdparser.parser.Parser;
import de.dfki.lt.mdparser.parser.Trainer;

/**
 * Top-level class for training, evaluating and parsing with MDParser.
 *
 * @author Jörg Steffen, DFKI
 */
public final class MDParser {

  private static final Logger logger = LoggerFactory.getLogger(MDParser.class);


  private MDParser() {

    // private constructor to enforce noninstantiability
  }


  /**
   * Trains a parser model from an annotated CoNLL corpus.
   *
   * @param modelFileName
   *          model file name
   * @param corpusFileName
   *          CoNLL corpus file name
   */
  public static void train(String modelFileName, String corpusFileName) {

    try {
      Trainer.trainWithSplittingFromDisk(corpusFileName, modelFileName);
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
  }


  /**
   * Evaluates a parser model against an annotated CoNLL corpus.
   *
   * @param modelFileName
   *          model archive, to be loaded from file system or classpath
   * @param corpusFileName
   *          CoNLL corpus file name
   * @param resultFileName
   *          result file name
   */
  public static void eval(String modelFileName, String corpusFileName, String resultFileName) {

    try {
      Eval evaluator = MDPrunner.parseAndEvalConllFile(corpusFileName, resultFileName, modelFileName);
      logger.info("Parent accuracy: " + evaluator.getParentsAccuracy());
      logger.info("Label accuracy:  " + evaluator.getLabelsAccuracy());
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
  }


  /**
   * Creates dependency structures for POS tagged sentences in the given CoNLL corpus.
   *
   * @param modelFileName
   *          model archive, to be loaded from file system or classpath
   * @param corpusFileName
   *          CoNLL corpus file name
   * @param resultFileName
   *          result file name
   */
  public static void parse(String modelFileName, String corpusFileName, String resultFileName) {

    try {
      List<Sentence> sentencesList = Parser.parse(corpusFileName, modelFileName);
      MDPrunner.writeSentences(sentencesList, resultFileName);
    } catch (IOException e) {
      logger.error(e.getLocalizedMessage(), e);
    }
  }


  public static void main(String[] args) {

    List<Options> optionsList = new ArrayList<>();
    optionsList.add(createTrainOptions());
    optionsList.add(createEvalOptions());
    optionsList.add(createParseOptions());

    CommandLine cmd = parseArguments(args, optionsList);
    if (null == cmd) {
      return;
    }

    String mode = cmd.getOptions()[0].getOpt();
    switch (mode) {
      case "train":
        train(
            cmd.getOptionValue("model"),
            cmd.getOptionValue("corpus"));
        break;
      case "eval":
        eval(
            cmd.getOptionValue("model"),
            cmd.getOptionValue("corpus"),
            cmd.getOptionValue("result"));
        break;
      case "parse":
        parse(
            cmd.getOptionValue("model"),
            cmd.getOptionValue("corpus"),
            cmd.getOptionValue("result"));
        break;
      default:
        logger.error(String.format("unkown mode '%s'", mode));
        return;
    }
  }


  private static Options createTrainOptions() {

    Options trainOptions = new Options();
    Option modeOption = new Option("train", false, "run in train mode");
    modeOption.setRequired(true);
    trainOptions.addOption(modeOption);

    Option modelOption = new Option("model", true, "model file to create");
    modelOption.setRequired(true);
    modelOption.setArgName("file");
    trainOptions.addOption(modelOption);

    Option corpusOption = new Option("corpus", true, "CoNLL corpus file");
    corpusOption.setRequired(true);
    corpusOption.setArgName("file");
    trainOptions.addOption(corpusOption);

    return trainOptions;
  }


  private static Options createEvalOptions() {

    Options evalOptions = new Options();
    Option modeOption = new Option("eval", false, "run in evaluation mode");
    modeOption.setRequired(true);
    evalOptions.addOption(modeOption);

    Option modelOption = new Option("model", true, "model, to be loaded from file system or classpath");
    modelOption.setRequired(true);
    modelOption.setArgName("file");
    evalOptions.addOption(modelOption);

    Option corpusOption = new Option("corpus", true, "CoNLL corpus file");
    corpusOption.setRequired(true);
    corpusOption.setArgName("file");
    evalOptions.addOption(corpusOption);

    Option resultOption = new Option("result", true, "result file");
    corpusOption.setRequired(true);
    corpusOption.setArgName("file");
    evalOptions.addOption(resultOption);

    return evalOptions;
  }


  private static Options createParseOptions() {

    Options parseOptions = new Options();
    Option modeOption = new Option("parse", false, "run in parse mode");
    modeOption.setRequired(true);
    parseOptions.addOption(modeOption);

    Option modelOption = new Option("model", true, "model, to be loaded from file system or classpath");
    modelOption.setRequired(true);
    modelOption.setArgName("file");
    parseOptions.addOption(modelOption);

    Option inputOption = new Option("corpus", true, "CoNLL corpus file with POS tags");
    inputOption.setRequired(true);
    inputOption.setArgName("file");
    parseOptions.addOption(inputOption);

    Option resultOption = new Option("result", true, "result file");
    resultOption.setRequired(true);
    resultOption.setArgName("file");
    parseOptions.addOption(resultOption);

    return parseOptions;
  }


  private static CommandLine parseArguments(String[] args, List<Options> optionsList) {

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;

    for (Options oneOptions : optionsList) {
      try {
        cmd = parser.parse(oneOptions, args);
        // arguments successfully parsed
        break;
      } catch (ParseException e) {
        // only log if parse exception is NOT related to mode
        if (args.length > 0 && !e.getMessage().contains(args[0])) {
          logger.error(e.getLocalizedMessage());
          break;
        }
      }
    }

    if (null == cmd) {
      System.out.format(
          "MDParser can%n"
              + "- train a parser model from an annotated CoNLL corpus%n"
              + "- evaluate a parser model against an annotated CoNLL corpus%n"
              + "- parse a POS annotated CoNLL corpus using a parser model%n"
              + "  and create dependency structures for its sentences%n%n");
      HelpFormatter formatter = new HelpFormatter();
      formatter.setOptionComparator(null);
      for (Options oneOptions : optionsList) {
        formatter.setSyntaxPrefix(
            String.format("MDParser options for %s mode", oneOptions.getRequiredOptions().get(0)));
        formatter.printHelp(":", oneOptions);
        System.out.println();
      }
    }

    return cmd;
  }
}
