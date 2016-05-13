package cases;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import de.dfki.lt.data.Data;

public class DataReaderTest {
	public static void main(String[] args) throws IOException {
		Data d = new Data("PIL/train/train-htb-ver0.5.auto.utf8.conll", true); 
	//	Data d = new Data("input/english.train", true);
		countDifLabels(d);
	}

	private static void countDifLabels(Data d) {
		Set<String> labels = new HashSet<String>();
		for (int n=0; n < d.getSentences().length;n++) {
			String[][] stringArray = d.getSentences()[n].getSentArray();
			for (int i=0; i < stringArray.length;i++) {
				labels.add(stringArray[i][3]);
			}
		}
		System.out.println(labels+" "+labels.size());
	}
}
