package standard;

import java.io.IOException;

import de.dfki.lt.mdparser.caller.MDPrunner;
import de.dfki.lt.mdparser.caller.MDPtrainer;
import de.dfki.lt.mdparser.eval.Eval;

public class MDPtester {

  private void trainLanguage(String trainConllFile, String modelFile)
      throws IOException {

    String trainFile = trainConllFile;
    String modelZipFileName = modelFile;

    MDPtrainer.train(trainFile, modelZipFileName);
  }


  private void testLanguage(String testConllFile, String resultFile, String modelFile) throws IOException {

    String testFile = testConllFile;
    String mdpResultFile = resultFile;
    String modelZipFileName = modelFile;

    Eval evaluator = MDPrunner.conllFileParsingAndEval(testFile, mdpResultFile, modelZipFileName);
    System.out.println("Parent accuracy: " + evaluator.getParentsAccuracy());
    System.out.println("Label accuracy:  " + evaluator.getLabelsAccuracy());
  }


  private void trainAndTest(String trainConllFile, String modelFile, String testConllFile, String resultFile)
      throws IOException {

    System.out.println("Do training with: " + trainConllFile);
    this.trainLanguage(trainConllFile, modelFile);
    System.out.println("\n");

    System.out.println("Do testing with: " + testConllFile);
    this.testLanguage(testConllFile, resultFile, modelFile);
    System.out.println("\n\n");
  }


  public static void main(String[] args) {

    try {
      MDPtester mdpTester = new MDPtester();

      //mdpTester.trainAndTest("resources/input/ptb3-std-training.conll", "ptb3-std.zip",
      //    "resources/input/ptb3-std-test.conll", "resources/input/ptb3-std-result.conll");

      //mdpTester.trainAndTest("resources/input/german_tiger_train.conll", "tiger.zip",
      //    "resources/input/german_tiger_test.conll", "resources/input/german_tiger_result.conll");
      //
      mdpTester.trainAndTest("resources/input/en-train-2009.conll", "en-2009.zip",
          "resources/input/en-test-2009.conll", "resources/input/en-result-2009.conll");

//      mdpTester.trainAndTest("resources/input/de-train-2009.conll", "de-2009.zip",
//          "resources/input/de-test-2009.conll", "resources/input/de-result-2009.conll");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
