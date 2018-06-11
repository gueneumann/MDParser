package de.dfki.lt.mdparser;

import java.io.IOException;

import de.dfki.lt.mdparser.caller.MDPrunner;
import de.dfki.lt.mdparser.config.ConfigKeys;
import de.dfki.lt.mdparser.config.GlobalConfig;
import de.dfki.lt.mdparser.eval.Eval;
import de.dfki.lt.mdparser.parser.TrainerFiles;

public final class MDPtester {

  private MDPtester() {

    // private constructor to enforce noninstantiability
  }


  private static void trainAndEvaluate(
      String trainConllFileName, String modelFileName, String testConllFileName,
      String resultFileName)
      throws IOException {

    // training
    System.out.println("Do training with: " + trainConllFileName);
    TrainerFiles.trainWithSplittingFromDisk(trainConllFileName, modelFileName);
    System.out.println("\n");

    // evaluation
    System.out.println("Do evaluation with: " + testConllFileName);
    Eval evaluator = MDPrunner.parseAndEvalConllFile(testConllFileName, resultFileName,
        // load model from file system and not classpath
        GlobalConfig.getPath(ConfigKeys.MODEL_OUTPUT_FOLDER).resolve(modelFileName).toString());
    System.out.println("Parent accuracy: " + evaluator.getParentsAccuracy());
    System.out.println("Label accuracy:  " + evaluator.getLabelsAccuracy());
    System.out.println("\n\n");
  }

  private static void testAndEvaluate(String modelFileName, String testConllFileName,
      String resultFileName)
      throws IOException {

    // evaluation
    System.out.println("Do evaluation with: " + testConllFileName);
    Eval evaluator = MDPrunner.parseAndEvalConllFile(testConllFileName, resultFileName,
        // load model from file system and not classpath
        GlobalConfig.getPath(ConfigKeys.MODEL_OUTPUT_FOLDER).resolve(modelFileName).toString());
    System.out.println("Parent accuracy: " + evaluator.getParentsAccuracy());
    System.out.println("Label accuracy:  " + evaluator.getLabelsAccuracy());
    System.out.println("\n\n");
  }


  public static void main(String[] args) {

    try {
      /*
      trainAndEvaluate("resources/input/ptb3-std-training.conll", "ptb3-std.zip",
          "resources/input/ptb3-std-test.conll", "resources/input/ptb3-std-result.conll");
      */

      /*
      trainAndEvaluate("resources/input/german_tiger_train.conll", "tiger.zip",
          "resources/input/german_tiger_test.conll", "resources/input/german_tiger_result.conll");
      */

      /*
      trainAndEvaluate("resources/input/en-train-2009.conll", "en-2009.zip",
          "resources/input/en-test-2009.conll", "resources/input/en-result-2009.conll");
      */

//    /*
      testAndEvaluate("en-2009.zip",
          "resources/input/en-test-2009.conll", "resources/input/en-result-2009.conll");
//      */

      /*
      trainAndEvaluate("resources/input/de-train-2009.conll", "de-2009.zip",
          "resources/input/de-test-2009.conll", "resources/input/de-result-2009.conll");
      */

//      // DE
//      trainAndEvaluate("de-train-2009.conll", "de-2009.zip",
//          "de-test-2009.conll", "de-2009-result.conll");
//
//      // EN
//      trainAndEvaluate("en-train-2009.conll", "en-2009.zip",
//          "en-test-2009.conll", "en-2009-result.conll");

      // pure Tiger corpus
      /*
      trainAndEvaluate("resources/input/german_tiger_train.conll", "tiger.zip",
          "resources/input/german_tiger_test.conll", "resources/input/german_tiger_result.conll");
      */

      // train with Tiger(train+test) and evaluate against NAMR gold test files
      /*
      trainAndEvaluate("resources/german_tiger_all.conll", "tiger.zip",
          "resources/test.conll", "resources/result.conll");
      */

      // train with Tiger(train+test) and NAMR gold train files
      // and evaluate against NAMR gold test files
      /*
      trainAndEvaluate(
          "resources/german_tiger_all+train.conll", "tiger.zip",
          "resources/test.conll", "resources/result.conll");
      */

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
