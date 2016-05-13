package cases;


import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import de.dfki.lt.model.ModelAnalyser;

public class ModelAnalyserTest {
	
	public static void printMapToFile(Map map, String outputFile) throws IOException {
		FileOutputStream out = new FileOutputStream(outputFile);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter fw = new BufferedWriter(or);
		Iterator iter = map.keySet().iterator();
		while (iter.hasNext()) {
			Object key = iter.next();
			fw.append(String.format("%s\t%.7f\n",key,map.get(key),Locale.GERMAN));
		}
		fw.close();
	}
	
	public static void main(String[] args) throws IOException {
		//split
	/*	String trainingDataDir = "split";
		String oldAlphabetFile = "temp/alpha_old.txt";
		String newAlphabetFile = "temp/alphaParser.txt";
		ModelAnalyser ma = new ModelAnalyser(trainingDataDir, oldAlphabetFile, newAlphabetFile);
		ma.editTrainingData();
		String modelFileDir = "splitModels";
		HashMap<String,Double> usefulnessMap = ma.computeUsefulness(modelFileDir);
	//	System.out.println(usefulnessMap);
		printMapToFile(usefulnessMap, "temp/fe_individual_features3.txt");
	//	String sortedMap = ma.sortUsefulnessMap(usefulnessMap);
	//	System.out.println(sortedMap);
	//	ma.computeSums("temp/fe_individual_features.txt");
		*/
		
		//no split
		String trainingDataDir = "a";
		String oldAlphabetFile = "temp/alpha_old.txt";
		String newAlphabetFile = "temp/alphaParser.txt";
		ModelAnalyser ma = new ModelAnalyser(trainingDataDir, oldAlphabetFile, newAlphabetFile);
		ma.editTrainingData();
		String modelFileDir = "b";
		HashMap<String,Double> usefulnessMap = ma.computeUsefulness(modelFileDir);
	//	System.out.println(usefulnessMap);
		printMapToFile(usefulnessMap, "temp/fe_individual_features4.txt");
		
	}
}
