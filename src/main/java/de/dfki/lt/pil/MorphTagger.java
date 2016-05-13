package de.dfki.lt.pil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.dfki.lt.data.Data;
import de.dfki.lt.data.Sentence;
import de.dfki.lt.features.Alphabet;
import de.dfki.lt.features.FeatureExtractor;
import de.dfki.lt.features.FeatureVector;

public class MorphTagger {
	
	private int correct;
	private int total;
	
	public MorphTagger() {
		
	}
	
	public void test(Data d, String morphFeature) throws IOException, InvalidInputDataException {
		Alphabet alphaMorph = new Alphabet("temp/alphaMorph"+morphFeature+".txt");
		FeatureExtractor fe = new FeatureExtractor();
		MorphModel mm = new MorphModel(alphaMorph, fe);
		Sentence[] sentences = d.getSentences();
		Model m = Linear.loadModel(new File("morphModels/"+morphFeature+".txt"));
		for (int n=0; n < sentences.length;n++) {
			Sentence sent = sentences[n];
			String[][] sentArray = sent.getSentArray();
			for (int i=0; i < sentArray.length;i++) {
				FeatureVector fv = mm.apply(i+1,sent, true);
				MorphFeatureVector mfv = new MorphFeatureVector(fv, sentArray[i][5]);
				String labelGold = mfv.getLabelsMap().get(morphFeature);
				int labelInt = (int) Linear.predict(m, fv.getLiblinearRepresentation(false,false,alphaMorph));	
				String labelPredicted = alphaMorph.getIndexLabelArray()[labelInt];
				if (labelPredicted.equals(labelGold)) {
					correct++;
				}
				total++;
				if (labelPredicted.equals("null")) {
					labelPredicted = "";
				}
				if (sentArray[i][5].equals("_")) {
					sentArray[i][5] = morphFeature+"-"+labelPredicted;
				}
				else {
					sentArray[i][5] += "|"+morphFeature+"-"+labelPredicted; 
				}
			}
		}
		System.out.println(morphFeature+" accuracy: "+Double.valueOf(correct)/Double.valueOf(total));
	}
	
}
