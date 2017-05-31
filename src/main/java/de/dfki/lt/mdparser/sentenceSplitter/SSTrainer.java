package de.dfki.lt.mdparser.sentenceSplitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import com.schmeier.posTagger.tagger.Tagger;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import de.dfki.lt.mdparser.data.Data;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.FeatureVector;

public class SSTrainer {

  private double bias = -1;
  private Problem prob = null;
  private Parameter param = new Parameter(SolverType.L1R_LR, 1, 0.01);

  private String conllInputFile;
  private String trainingDataFile;
  private String lowerCaseWordsFile;
  private String neWordsFile;
  private String endFile;
  private String nonEndFile;
  private String abbrFile;
  private String firstFile;
  private String alphabetFile;
  private String modelFile;
  private Tagger tagger;


  public SSTrainer(String conllInputFile, String[] modelFiles) {
    this.conllInputFile = conllInputFile;
    this.trainingDataFile = modelFiles[0];
    this.alphabetFile = modelFiles[1];
    this.modelFile = modelFiles[2];
    this.lowerCaseWordsFile = modelFiles[3];
    this.neWordsFile = modelFiles[4];
    this.endFile = modelFiles[5];
    this.nonEndFile = modelFiles[6];
    this.abbrFile = modelFiles[7];
    this.firstFile = modelFiles[8];

    this.tagger = new Tagger(modelFiles[9]);
    this.tagger.init();
  }


  public void createTrainingData(String language) throws IOException {

    // GN: internalize CONLL data in 2-Dim sentences
    Data d = new Data(this.conllInputFile, true);
    Sentence[] sents = d.getSentences();
    // GN: Initialize model
    SSFeatureModel fm = new SSFeatureModel();
    // GN: Initialize alphabet
    Alphabet alpha = new Alphabet();
    FileOutputStream out = new FileOutputStream(this.trainingDataFile);
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter bw = new BufferedWriter(or);
    // GN: collect information from training file and store in separate files
    // later used as helper for defining features, and are applied in templates
    createLowerCaseWordsList(this.conllInputFile, this.lowerCaseWordsFile);
    createPossibleEndCharsList(this.conllInputFile, this.endFile);
    createPossibleLowCasePosList(this.conllInputFile, this.neWordsFile);

    // /* Uncommented by GN
    createNEWordsList(this.conllInputFile, language, this.neWordsFile);
    createPossibleEndCharsList(this.conllInputFile, this.endFile);
    createPossibleNonEndList(this.conllInputFile, this.nonEndFile);
    createPossibleAbbrList(this.conllInputFile, this.abbrFile);
    createPossibleFirstWords(this.conllInputFile, this.firstFile);
    // */

    //GN: Read in again and store as sets which will be passed to feature model creator
    Set<String> lowCaseSet = readWords(this.lowerCaseWordsFile);
    Set<String> neSet = readWords(this.neWordsFile);
    Set<String> endSet = readWords(this.endFile);
    Set<String> nonEndSet = readWords(this.nonEndFile);
    Set<String> abbrSet = readWords(this.abbrFile);
    Set<String> firstSet = readWords(this.firstFile);
    List<Set<String>> sets = new ArrayList<Set<String>>();
    sets.add(lowCaseSet);
    sets.add(neSet);
    sets.add(endSet);
    sets.add(nonEndSet);
    sets.add(abbrSet);
    sets.add(firstSet);
    int pos = 0;
    int total = 0;
    // GN: compute total token count
    for (int i = 0; i < sents.length; i++) {
      total += sents[i].getSentArray().length - 1;
    }
    List<String> tokens = new ArrayList<String>(total);
    List<String> posTags = new ArrayList<String>(total);
    List<Integer> finalPositions = new ArrayList<Integer>(total);

    // GN: add each token+POS and store whether token is found at sentence end position
    for (int i = 0; i < sents.length; i++) {
      Sentence sent = sents[i];
      String[][] sentArray = sent.getSentArray();
      for (int j = 0; j < sentArray.length; j++) {
        String word = sentArray[j][1];
        String posTag = sentArray[j][4];
        /*
        if (word.length() > 1 && word.charAt(word.length()-1) == '.') {
          tokens.add(word.substring(0,word.length()-1));
          posTags.add(posTag);
          tokens.add(".");
          posTags.add(".");
          pos++;
        }
        else {
        */
        tokens.add(word);
        posTags.add(posTag);
        //}

        if (j + 1 >= sentArray.length) {
          // GN: Collect end positions.
          finalPositions.add(pos);
        }
        pos++;


      }
    }
    //List<String> posTags = posTag(tokens);

    for (int i = 0; i < tokens.size(); i++) {

      // GN: For each token of all sentences
      String token = tokens.get(i);

      // System.out.println(i + ": " + token);
      // GN: set binary classifier label to y or n depending on whether token
      // has been seen at end of a sentence.
      if (endSet.contains(token)) {
        String label = "n";
        if (finalPositions.contains(i)) {
          label = "y";
        }
        // GN: If token is in endSet, create feature vector for token and assign label
        // Using all computed generate feature files collected in sets
        FeatureVector fv = fm.apply(true, i, tokens, posTags, sets, alpha);
        // System.out.println(" ... " + fv.getfList().size() + ": " + fv.toString());
        fv.setLabel(label);
        alpha.addLabel(label);
        // GN: add integer coding of feature vector to trainingDataFile ("temp/ss.txt")
        // It computes a specific index for each feature:value combination !
        // DOES not make sense and leads to a little poorer result
        //if (label.equals("y")) {
        //  //System.out.println(" --- " + fv.getIntegerRepresentation(alpha, false));
        //  bw.append(fv.getIntegerRepresentation(alpha, false)+"\n");
        //}
        //if (label.equals("y")) {
        //  System.out.println(" --- " + fv.getIntegerRepresentation(alpha, false));
        //}
        bw.append(fv.getIntegerRepresentation(alpha, false) + "\n");
      }
    }
    // GN: close training data file and create alphabet file
    bw.close();
    alpha.printToFile(this.alphabetFile);

  }


  private void createPossibleLowCasePosList(String conllInputFile2,
      String neWordsFile) throws IOException {

    FileOutputStream out = new FileOutputStream(neWordsFile);
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter bw = new BufferedWriter(or);

    Data d = new Data(this.conllInputFile, true);
    Sentence[] sents = d.getSentences();
    Set<String> lowCaseWords = new HashSet<String>();
    for (int i = 0; i < sents.length; i++) {
      Sentence sent = sents[i];
      String[][] sentArray = sent.getSentArray();
      for (int j = 1; j < sentArray.length; j++) {
        String word = sentArray[j][1];
        String pos = sentArray[j][4];
        if (Character.isLowerCase(word.charAt(0))) {
          lowCaseWords.add(pos);
        }
      }
    }
    Iterator<String> iter = lowCaseWords.iterator();
    while (iter.hasNext()) {
      String word = iter.next();
      bw.append(word + "\n");
    }
    bw.close();

  }


  public void train() throws IOException, InvalidInputDataException {

    // GN: Read in the created training data file, whereby alpha is not needed here !
    // GN: NOTE: readProblemm is defined below !
    readProblem(this.trainingDataFile);
    // GN: Create model, i.e., actually do training !
    Model model = Linear.train(this.prob, this.param);
    // GN: Save the model file
    model.save(new File(this.modelFile));

  }


  private void createLowerCaseWordsList(String conllInputFile, String outputFile) throws IOException {

    FileOutputStream out = new FileOutputStream(outputFile);
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter bw = new BufferedWriter(or);

    Data d = new Data(conllInputFile, true);
    Sentence[] sents = d.getSentences();
    Set<String> lowCaseWords = new HashSet<String>();
    for (int i = 0; i < sents.length; i++) {
      Sentence sent = sents[i];
      String[][] sentArray = sent.getSentArray();
      for (int j = 1; j < sentArray.length; j++) {
        String word = sentArray[j][1];
        if (Character.isLowerCase(word.charAt(0))) {
          lowCaseWords.add(word);
        }
      }
    }
    Iterator<String> iter = lowCaseWords.iterator();
    while (iter.hasNext()) {
      String word = iter.next();
      bw.append(word + "\n");
    }
    bw.close();

  }


  private void createNEWordsList(String conllInputFile, String language, String outputFile) throws IOException {

    Set<String> posTagSet = new HashSet<String>();
    if (language.equals("english")) {
      posTagSet.add("NNP");
      posTagSet.add("NNPS");
    } else if (language.equals("german")) {
      posTagSet.add("NE");
    }
    FileOutputStream out = new FileOutputStream(outputFile);
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter bw = new BufferedWriter(or);

    Data d = new Data(conllInputFile, true);
    Sentence[] sents = d.getSentences();
    Set<String> neWords = new HashSet<String>();
    for (int i = 0; i < sents.length; i++) {
      Sentence sent = sents[i];
      String[][] sentArray = sent.getSentArray();
      for (int j = 1; j < sentArray.length; j++) {
        String word = sentArray[j][1];
        String pos = sentArray[j][3];
        if (posTagSet.contains(pos)) {
          neWords.add(word);
        }
      }
    }
    Iterator<String> iter = neWords.iterator();
    while (iter.hasNext()) {
      String word = iter.next();
      bw.append(word + "\n");
    }
    bw.close();

  }


  private void createPossibleEndCharsList(String conllInputFile,
      String endFile) throws IOException {

    FileOutputStream out = new FileOutputStream(endFile);
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter bw = new BufferedWriter(or);

    Data d = new Data(conllInputFile, true);
    Sentence[] sents = d.getSentences();
    HashMap<String, Integer> map = new HashMap<String, Integer>();
    for (int i = 0; i < sents.length; i++) {
      Sentence sent = sents[i];
      String[][] sentArray = sent.getSentArray();
      String word = sentArray[sentArray.length - 1][1];
      if (Character.getType(word.charAt(0)) == 24) {
        Integer c = map.get(word);
        if (c == null) {
          map.put(word, 1);
        } else {
          map.put(word, 1 + c);
        }
      }
    }
    Iterator<String> iter = map.keySet().iterator();
    while (iter.hasNext()) {
      String word = iter.next();
      if (map.get(word) > 10) {
        bw.append(word + "\n");
      }
    }
    bw.close();

  }


  private void createPossibleNonEndList(String conllInputFile, String endFile) throws IOException {

    FileOutputStream out = new FileOutputStream(endFile);
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter bw = new BufferedWriter(or);
    Data d = new Data(conllInputFile, true);
    Sentence[] sents = d.getSentences();
    HashMap<String, Integer> map = new HashMap<String, Integer>();
    for (int i = 0; i < sents.length; i++) {
      Sentence sent = sents[i];
      String[][] sentArray = sent.getSentArray();

      for (int j = 1; j < sentArray.length - 1; j++) {
        String word = sentArray[j][1];
        String nextWord = sentArray[j + 1][1];
        if (Character.isUpperCase(nextWord.charAt(0))) {
          Integer c = map.get(word);
          if (c == null) {
            map.put(word, 1);
          } else {
            map.put(word, 1 + c);
          }
        }
      }
    }
    Iterator<String> iter = map.keySet().iterator();
    while (iter.hasNext()) {
      String word = iter.next();
      if (map.get(word) > 10) {
        bw.append(word + "\n");
      }
    }
    bw.close();

  }


  private void createPossibleAbbrList(String conllInputFile, String abbrFile) throws IOException {

    FileOutputStream out = new FileOutputStream(abbrFile);
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter bw = new BufferedWriter(or);
    Data d = new Data(conllInputFile, true);
    Sentence[] sents = d.getSentences();
    HashMap<String, Integer> map = new HashMap<String, Integer>();
    for (int i = 0; i < sents.length; i++) {
      Sentence sent = sents[i];
      String[][] sentArray = sent.getSentArray();

      for (int j = 1; j < sentArray.length - 1; j++) {
        String word = sentArray[j][1];
        if (word.charAt(word.length() - 1) == '.') {
          String newWord = word.substring(0, word.length() - 1);
          Integer c = map.get(newWord);
          if (c == null) {
            map.put(newWord, 1);
          } else {
            map.put(newWord, 1 + c);
          }
        }
      }
    }
    Iterator<String> iter = map.keySet().iterator();
    while (iter.hasNext()) {
      String word = iter.next();
      if (map.get(word) > 5) {
        bw.append(word + "\n");
      }
    }
    bw.close();

  }


  private void createPossibleFirstWords(String conllInputFile,
      String firstFile) throws IOException {

    FileOutputStream out = new FileOutputStream(firstFile);
    OutputStreamWriter or = new OutputStreamWriter(out, "UTF-8");
    BufferedWriter bw = new BufferedWriter(or);
    Data d = new Data(conllInputFile, true);
    Sentence[] sents = d.getSentences();
    HashMap<String, Integer> map = new HashMap<String, Integer>();
    for (int i = 0; i < sents.length; i++) {
      Sentence sent = sents[i];
      String[][] sentArray = sent.getSentArray();
      String word = sentArray[0][1];
      Integer c = map.get(word);
      if (c == null) {
        map.put(word, 1);
      } else {
        map.put(word, 1 + c);
      }
    }
    Iterator<String> iter = map.keySet().iterator();
    while (iter.hasNext()) {
      String word = iter.next();
      if (map.get(word) > 5) {
        bw.append(word + "\n");
      }
    }
    bw.close();

  }


  private Set<String> readWords(String inputFile) throws IOException {

    FileInputStream in = new FileInputStream(inputFile);
    InputStreamReader ir = new InputStreamReader(in, "UTF8");
    BufferedReader fr = new BufferedReader(ir);
    String line;
    Set<String> set = new HashSet<String>();
    while ((line = fr.readLine()) != null) {
      set.add(line);
    }
    fr.close();
    return set;
  }


  static double atof(String s) {

    if (s == null || s.length() < 1) {
      throw new IllegalArgumentException("Can't convert empty string to integer");
    }
    double d = Double.parseDouble(s);
    if (Double.isNaN(d) || Double.isInfinite(d)) {
      throw new IllegalArgumentException("NaN or Infinity in input: " + s);
    }
    return (d);
  }


  static int atoi(String s) throws NumberFormatException {

    if (s == null || s.length() < 1) {
      throw new IllegalArgumentException("Can't convert empty string to integer");
    }
    // Integer.parseInt doesn't accept '+' prefixed strings
    if (s.charAt(0) == '+') {
      s = s.substring(1);
    }
    return Integer.parseInt(s);
  }


  public void readProblem(String filename) throws IOException, InvalidInputDataException {

    BufferedReader fp = new BufferedReader(new FileReader(filename));
    List<Integer> vy = new ArrayList<Integer>();
    List<FeatureNode[]> vx = new ArrayList<FeatureNode[]>();
    int max_index = 0;
    int lineNr = 0;

    try {
      while (true) {
        // GN: read training data file line by line and stop if no more lines
        // GN: line is actually an integer representation of a feature vector, e.g.,
        // 1 8:1 9:1 10:1 11:1 ...  134:1 135:1 136:1
        // First element is class label (1=N, 2=y), and then N feature-value-index:value
        // It seems that here, value is always 1 (probably just a dummy)
        // Feature-value-index is just the individual index of a feature:value combination
        String line = fp.readLine();
        if (line == null) {
          break;
        }
        lineNr++;
        // GN: tokenize integer string
        StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
        String token = st.nextToken();

        try {

          vy.add(atoi(token));
        } catch (NumberFormatException e) {
          throw new InvalidInputDataException("invalid label: " + token, filename, lineNr, e);
        }

        int m = st.countTokens() / 2;
        // m just seems to be the feature vector size
        // bias seems to be set to -1
        FeatureNode[] x;
        if (this.bias >= 0) {
          x = new FeatureNode[m + 1];
        } else {
          x = new FeatureNode[m];
        }

        int indexBefore = 0;

        // GN: now loop through the feature:value pairs
        for (int j = 0; j < m; j++) {

          // GN: the feature integer code
          token = st.nextToken();
          int index;
          try {
            index = atoi(token);
          } catch (NumberFormatException e) {
            throw new InvalidInputDataException("invalid index: " + token, filename, lineNr, e);
          }

          // assert that indices are valid and sorted
          if (index < 0) {
            throw new InvalidInputDataException("invalid index: " + index, filename, lineNr);
          }
          if (index <= indexBefore) {
            throw new InvalidInputDataException("indices must be sorted in ascending order", filename, lineNr);
          }
          indexBefore = index;

          // GN: the feature value
          token = st.nextToken();
          try {
            double value = atof(token);

            // Add a feature/value node at j-position in the x
            x[j] = new FeatureNode(index, value);
          } catch (NumberFormatException e) {
            throw new InvalidInputDataException("invalid value: " + token, filename, lineNr);
          }
        }
        if (m > 0) {
          max_index = Math.max(max_index, x[m - 1].index);
        }

        vx.add(x);
      }
      // GN: vy = a vector of all labels for all tokens
      // GN: vx = a vector of all feature vectors for all tokens
      // GN: max_index = the maximum feature-value-index
      this.prob = constructProblem(vy, vx, max_index);
    } finally {
      fp.close();
    }
  }


  public Problem constructProblem(List<Integer> vy, List<FeatureNode[]> vx, int max_index) {

    Problem prob = new Problem();
    prob.bias = this.bias;
    prob.l = vy.size();
    prob.n = max_index;
    if (this.bias >= 0) {
      prob.n++;
    }
    prob.x = new FeatureNode[prob.l][];
    for (int i = 0; i < prob.l; i++) {
      prob.x[i] = vx.get(i);


      // GN: bias is set to -1 !

      if (this.bias >= 0) {
        // GN: assert should be gone !
        assert prob.x[i][prob.x[i].length - 1] == null;

        prob.x[i][prob.x[i].length - 1] = new FeatureNode(max_index + 1, this.bias);

      } else {
        // GN: assert should be gone !
        assert prob.x[i][prob.x[i].length - 1] != null;
      }
    }

    // GN: looks like that vy and vx are just copied
    prob.y = new double[prob.l];
    for (int i = 0; i < prob.l; i++) {
      prob.y[i] = vy.get(i);
    }
    return prob;
  }


}
