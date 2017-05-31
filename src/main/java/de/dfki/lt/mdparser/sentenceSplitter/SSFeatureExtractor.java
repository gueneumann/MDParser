package de.dfki.lt.mdparser.sentenceSplitter;

import java.util.List;
import java.util.Set;

public class SSFeatureExtractor {

  public SSFeatureExtractor() {

  }


  public String wordFormFeature(List<String> tokens, int id) {

    if (id < 0 || id >= tokens.size()) {
      return "null";
    } else {
      return tokens.get(id);
    }
  }


  public String posFeature(List<String> posTags, int id) {

    if (id < 0 || id >= posTags.size()) {
      return "null";
    } else {
      return posTags.get(id);
    }
  }


  public String isUpperCase(String wf) {

    if (Character.isUpperCase(wf.charAt(0))) {
      return "y";
    } else {
      return "n";
    }
  }


  public String isNumber(String wf) {

    for (int i = 0; i < wf.toCharArray().length; i++) {
      if (Character.isDigit(wf.toCharArray()[i])) {
        return "y";
      }
    }
    return "n";
  }


  public String containsPunctuation(String wf) {

    for (int i = 0; i < wf.toCharArray().length; i++) {
      if (Character.getType(wf.toCharArray()[i]) == 24) {
        return "y";
      }
    }
    return "n";
  }


  public String isInTheList(String wf, Set<String> set) {

    if (set.contains(wf)) {
      return "y";
    } else {
      return "n";
    }
  }
}
