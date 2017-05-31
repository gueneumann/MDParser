package cases;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import de.dfki.lt.mdparser.data.Data;

public class DataReaderTest {

  public static void main(String[] args) {

    try {
      Data d = new Data("/Users/gune00/data/MLDP/2009/en-train-2009.conll", true);
      //Data d = new Data("input/english.train", true);
      countDifLabels(d);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  private static void countDifLabels(Data d) {

    Set<String> labels = new HashSet<String>();
    for (int n = 0; n < d.getSentences().length; n++) {
      String[][] stringArray = d.getSentences()[n].getSentArray();
      for (int i = 0; i < stringArray.length; i++) {
        labels.add(stringArray[i][3]);
      }
    }
    System.out.println(labels + " " + labels.size());
  }
}
