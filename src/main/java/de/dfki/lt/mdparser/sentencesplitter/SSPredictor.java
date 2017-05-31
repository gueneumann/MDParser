package de.dfki.lt.mdparser.sentencesplitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import com.schmeier.posTagger.focus.Focus;
import com.schmeier.posTagger.tagger.Tagger;

import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.FeatureVector;
import edu.northwestern.at.morphadorner.corpuslinguistics.tokenizer.DefaultWordTokenizer;

public class SSPredictor {

  private Alphabet alpha;
  private Model model;
  private Tagger tagger;
  private Set<String> lowCaseSet;
  private Set<String> neSet;
  private Set<String> endSet;


  public SSPredictor(String[] modelFiles)
      throws IOException {
    this.alpha = new Alphabet(modelFiles[0]);
    this.model = Linear.loadModel(new File(modelFiles[1]));
    if (modelFiles.length > 2) {
      this.lowCaseSet = readWords(modelFiles[2]);
    }
    if (modelFiles.length > 3) {
      this.neSet = readWords(modelFiles[3]);
    }
    if (modelFiles.length > 4) {
      this.endSet = readWords(modelFiles[4]);
    }
    if (modelFiles.length > 5) {
      this.tagger = new Tagger(modelFiles[5]);
    }
  }


  public SSPredictor(InputStream[] modelFiles, String taggerFile)
      throws IOException {

    this.alpha = new Alphabet(modelFiles[0]);

    this.model = Linear.loadModel(new InputStreamReader(modelFiles[1]));
    if (modelFiles.length > 2) {
      this.lowCaseSet = readWords(modelFiles[2]);
    }
    if (modelFiles.length > 3) {
      this.neSet = readWords(modelFiles[3]);
    }
    if (modelFiles.length > 4) {
      this.endSet = readWords(modelFiles[4]);
    }
    this.tagger = new Tagger(taggerFile);

  }


  public String readInput(String inputFile, String inputFormat) throws IOException {

    FileInputStream in = new FileInputStream(inputFile);
    InputStreamReader ir = new InputStreamReader(in, "UTF8");
    BufferedReader fr = new BufferedReader(ir);
    String line;
    StringBuilder sb = new StringBuilder();
    if (inputFormat.equals("conll")) {
      while ((line = fr.readLine()) != null) {
        if (line.length() > 0) {
          String word = line.split("\t")[1];
          sb.append(word + " ");
        }
      }
    }
    fr.close();
    return sb.toString();
  }


  public List<String> tokenise(String inputString) {

    DefaultWordTokenizer wordTokenizer = new DefaultWordTokenizer();
    //List<String> tok1 =
    return wordTokenizer.extractWords(inputString);
    //return fix(tok1);

  }


  public static String tag(String in, Tagger t) {

    Focus focus = new Focus();
    StringTokenizer str = new StringTokenizer(in);
    while (str.hasMoreTokens()) {
      String word = str.nextToken();
      focus.add(word);
    }
    t.run(focus);
    return focus.toString();
  }


  private List<String> posTag(List<String> tokens) {

    List<String> posTags = new ArrayList<String>(tokens.size());
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < tokens.size(); i++) {
      sb.append(tokens.get(i) + " ");
    }
    String taggedInput = tag(sb.toString(), this.tagger);
    String[] array = taggedInput.split("  ");
    for (int i = 0; i < array.length; i++) {
      String unit = array[i];
      int splitPoint = unit.lastIndexOf(":");
      posTags.add(unit.substring(splitPoint + 1, unit.length()));
    }
    return posTags;
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


  private Set<String> readWords(InputStream inputStream) throws IOException {

    InputStreamReader ir = new InputStreamReader(inputStream, "UTF8");
    BufferedReader fr = new BufferedReader(ir);
    String line;
    Set<String> set = new HashSet<String>();
    while ((line = fr.readLine()) != null) {
      set.add(line);
    }
    return set;
  }


  public List<List<String>> predict(List<String> tokensList) {

    List<List<String>> result = new ArrayList<List<String>>();
    List<String> curSent = new ArrayList<String>();
    SSFeatureModel fm = new SSFeatureModel();
    List<Set<String>> sets = new ArrayList<Set<String>>();
    boolean end = false;
    sets.add(this.lowCaseSet);
    sets.add(this.neSet);
    sets.add(this.endSet);
    //sets.add(nonEndSet);sets.add(abbrSet);sets.add(firstSet);
    List<String> tagsList = posTag(tokensList);
    for (int i = 0; i < tokensList.size(); i++) {
      String curWord = tokensList.get(i);
      curSent.add(curWord);
      if (this.endSet.contains(curWord)) {
        FeatureVector fv = fm.apply(false, i, tokensList, tagsList, sets, this.alpha);
        double[] probs = new double[2];
        int labelInt =
            (int)Linear.predictProbability(this.model, fv.getLiblinearRepresentation(false, false, this.alpha), probs);
        String label = this.alpha.getIndexLabelArray()[labelInt];
        if (label.equals("y")) {
          end = true;
        }
        if (end) {
          result.add(curSent);
          curSent = new ArrayList<String>();
          end = false;
        }
      }
    }

    return result;

  }


  public List<List<String>> predict(String text) {

    return predict(tokenise(text));
  }


}
