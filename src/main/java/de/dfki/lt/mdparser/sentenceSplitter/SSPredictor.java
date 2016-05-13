package de.dfki.lt.mdparser.sentenceSplitter;

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
	
	public SSPredictor(String[] modelFiles) throws IOException {
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
			tagger = new Tagger(modelFiles[5]);
		}
	}

	public SSPredictor(InputStream[] modelFiles,String taggerFile) throws IOException {

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
		tagger = new Tagger(taggerFile);

	}
	
	public String readInput(String inputFile, String inputFormat) throws IOException {
		FileInputStream in = new FileInputStream(inputFile);
		InputStreamReader ir = new InputStreamReader(in, "UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line;
		StringBuilder sb = new StringBuilder(); 
		if (inputFormat.equals("conll")) {
			while ((line = fr.readLine())!= null) {
				if (line.length() > 0) {
					String word = line.split("\t")[1];
					sb.append(word+" ");
				}
			}
		}
		return sb.toString();
	}
	
	public List<String> tokenise(String inputString) {
		DefaultWordTokenizer wordTokenizer = new DefaultWordTokenizer();
	//	List<String> tok1 =
			return wordTokenizer.extractWords(inputString);
	//	return fix(tok1);
		
	}
	
	public static String tag(String in, Tagger t) {
		Focus focus = new Focus();
		StringTokenizer str = new StringTokenizer(in);
		while(str.hasMoreTokens()) {
			String word = str.nextToken();
			focus.add(word);
		}
		t.run(focus);
		return focus.toString();
	}
	
	private List<String> posTag(List<String> tokens) {
		List<String> posTags = new ArrayList<String>(tokens.size());
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < tokens.size();i++) {
			sb.append(tokens.get(i)+" ");
		}
		String taggedInput = tag(sb.toString(), tagger);
		String[] array = taggedInput.split("  ");
		for (int i=0; i < array.length;i++) {
			String unit = array[i];
			int splitPoint = unit.lastIndexOf(":");
			posTags.add(unit.substring(splitPoint+1,unit.length()));
		}
		return posTags;
	}
	
/*	private List<String> fix(List<String> tok1) {
		List<String> newTok = new ArrayList<String>();
		for (int i=0; i < tok1.size();i++) {
			String tok = tok1.get(i);
			String nextTok = "null";
			if (i+1 < tok1.size()) {
				nextTok = tok1.get(i+1);
			}
			if (tok.equals("Sen") && nextTok.equals(".") ) {
				newTok.add(tok+".");
				i++;
			}
			else if (tok.equals("Sept") && nextTok.equals(".")) {
				newTok.add(tok+".");
				i++;
			}
			else if (tok.equals("Nov") && nextTok.equals(".")) {
				newTok.add(tok+".");
				i++;
			}
			else if (tok.equals("No") && nextTok.equals(".")) {
				newTok.add(tok+".");
				i++;
			}
			else if (tok.equals("Dec") && nextTok.equals(".")) {
				newTok.add(tok+".");
				i++;
			}
		/*	else if (tok.length() < 3 && nextTok.equals(".")) {
				newTok.add(tok+".");
				i++;
			}
			else {
				newTok.add(tok);
			}
		}
		return newTok;
	}*/

	private Set<String> readWords(String inputFile) throws IOException {
		FileInputStream in = new FileInputStream(inputFile);
		InputStreamReader ir = new InputStreamReader(in, "UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line;
		Set<String> set = new HashSet<String>();
		while ((line = fr.readLine())!= null) {
			set.add(line);
		}
		return set;
	}
	
	private Set<String> readWords(InputStream inputStream) throws IOException {
		InputStreamReader ir = new InputStreamReader(inputStream, "UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line;
		Set<String> set = new HashSet<String>();
		while ((line = fr.readLine())!= null) {
			set.add(line);
		}
		return set;
	}
	
	public List<List<String>> predict(List<String> tokensList) throws IOException {
		List<List<String>> result = new ArrayList<List<String>>();
		List<String> curSent = new ArrayList<String>();
		SSFeatureModel fm = new SSFeatureModel();
		double prob = 0.0;
		List<Set<String>> sets = new ArrayList<Set<String>>();
		boolean end = false;
		sets.add(lowCaseSet);sets.add(neSet);sets.add(endSet);
		//sets.add(nonEndSet);sets.add(abbrSet);sets.add(firstSet);
		List<String> tagsList = posTag(tokensList);
		for (int i=0; i < tokensList.size();i++) {
			String curWord = tokensList.get(i);
			curSent.add(curWord);
			if (endSet.contains(curWord)) {
				FeatureVector fv = fm.apply(false, i, tokensList, tagsList, sets, alpha);
				double[] probs = new double[2];
				int labelInt = (int) Linear.predictProbability(model, fv.getLiblinearRepresentation(false,false,alpha), probs);
				String label = alpha.getIndexLabelArray()[labelInt];
				if (label.equals("y")) {
					end = true;
				}
				prob = probs[1];	
				if (end) {
					result.add(curSent);
					curSent = new ArrayList<String>();
					end = false;
				}
			}
		}

		return result;
		
	}

	public List<List<String>> predict(String text) throws IOException {
		return predict(tokenise(text));
	}
	
	
}
