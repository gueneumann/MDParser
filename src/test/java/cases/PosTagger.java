package cases;


import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.schmeier.posTagger.focus.Focus;
import com.schmeier.posTagger.tagger.Tagger;

import de.dfki.lt.outputformat.XMLString;
import edu.northwestern.at.morphadorner.corpuslinguistics.sentencesplitter.DefaultSentenceSplitter;
import edu.northwestern.at.morphadorner.corpuslinguistics.sentencesplitter.SentenceSplitter;
import edu.northwestern.at.morphadorner.corpuslinguistics.tokenizer.DefaultWordTokenizer;
import edu.northwestern.at.morphadorner.corpuslinguistics.tokenizer.WordTokenizer;


public class PosTagger {
	
	private Tagger tagger;
	private static SentenceSplitter sentenceSplitter;
	private static WordTokenizer wordTokenizer;
	private String[][][] tokenizedOutput;
	
	public PosTagger(String language) {
		if (language.equals("english")) {
			String modelFilePosTaggerEnglish = "resources/tagger/BROWN_MEDLINE/MEDLINE-BROWN-FINAL";
			setTagger(new Tagger(modelFilePosTaggerEnglish));
			tagger.init();
		}
		else if (language.equals("german")) {
			String modelFilePosTaggerGerman = "resources/tagger/NEGRA/NEGRA";
			setTagger(new Tagger(modelFilePosTaggerGerman));
			tagger.init();
		}
		else if (language.equals("englishUpos")) {
			String modelFilePosTaggerEnglish = "resources/UposModels/english/ENGLISH";
			setTagger(new Tagger(modelFilePosTaggerEnglish));
			tagger.init();
		}
		wordTokenizer = new DefaultWordTokenizer();
		sentenceSplitter = new DefaultSentenceSplitter();
	}

	public void setTagger(Tagger tagger) {
		this.tagger = tagger;
	}

	public Tagger getTagger() {
		return tagger;
	}
	
	public String tagSentence(String text, String inputFormat) throws Exception {
		long start = System.currentTimeMillis();
		int length = 0;	
		StringBuilder sb = new StringBuilder();
		String sentenceString = text;
		String taggedSentenceString = null;
		String[] sentenceArray = null;
		if (inputFormat.equals("text")) {
			taggedSentenceString =  tag(sentenceString, tagger);
			return taggedSentenceString;
		//	sentenceArray = taggedSentenceString.split("\\s+");
		}
		else if (inputFormat.equals("conll")) {
			BufferedReader br = new BufferedReader(new StringReader(text));
			String line;
			List<String> sentence = new ArrayList<String>();
			while ((line = br.readLine())!= null) {
				if (line.length() > 0) {
					String[] lineArray = line.split("\t");
					sentence.add(lineArray[1]+":"+lineArray[3]);
				}
			}
			sentenceArray = new String[sentence.size()];
			sentenceArray = sentence.toArray(sentenceArray);
		}
		tokenizedOutput = new String[1][][];				
		tokenizedOutput[0] = new String[sentenceArray.length][10];
		length+=sentenceArray.length;
		for (int j = 0; j < sentenceArray.length; j++) {
			String curEntry = sentenceArray[j];
			int splitPoint = curEntry.lastIndexOf(":");
			String wordForm = curEntry.substring(0, splitPoint);
			String pos = curEntry.substring(splitPoint+1,curEntry.length());
			tokenizedOutput[0][j][0] = String.valueOf(j+1);
			sb.append(tokenizedOutput[0][j][0]+"\t");
			XMLString wfXml = new XMLString(wordForm);
			tokenizedOutput[0][j][1] = wfXml.getXmlString();	
			sb.append(tokenizedOutput[0][j][1]+"\t_\t");
			tokenizedOutput[0][j][3] = pos;
			tokenizedOutput[0][j][4] = pos;
	/*		for (int m=4; m < 10; m++) {
				tokenizedOutput[0][j][m] = "_";
			}*/
			sb.append(tokenizedOutput[0][j][3]+"\t"+tokenizedOutput[0][j][3]+"\t_\t_\t_\n");			
		}
		sb.append("\n");
	/*	System.err.printf("Tagged %d words at %.2f words per second.\n",
				length, (Double.valueOf(length) / (Double.valueOf(System
						.currentTimeMillis()
						- start) / 1000)));*/
		return sb.toString();
	//	return tokenizedOutput[0];
	}
	
	public String[][][] tagText(String text, String language, String inputFormat) throws Exception {
		long start = System.currentTimeMillis();
		int length = 0;
		List<List<String>> sentences = null;
		String[][] sentencesArray = null;
		List<String> taggedList = null;
		StringBuilder sb = new StringBuilder();
		if (inputFormat.equals("text")) {
			sentences = sentenceSplitter.extractSentences(text, wordTokenizer);		
			sentencesArray = new String[sentences.size()][];
			taggedList = new ArrayList<String>();
			for (int i = 0; i < sentences.size(); i++) {
				StringBuilder sbSentence = new StringBuilder();
				List<String> sentence = sentences.get(i);
				
				if (sentence.size() > 2) {
					for (int j = 0; j < sentence.size()-2; j++) {								
						sbSentence.append(sentence.get(j)+" ");
					}
				}
				if (sentence.size() > 2) {
					sbSentence.append(sentence.get(sentence.size()-2));
				}
				if (sentence.size() > 1) {
					sbSentence.append(sentence.get(sentence.size()-1));
				}
				if (sentence.size() == 1) {
					sbSentence.append(sentence.get(0));
				}
				String sentenceString = sbSentence.toString();
				
				String taggedSentenceString = tag(sentenceString, tagger);
				taggedList.add(taggedSentenceString);
				String[] taggedSentenceArray = taggedSentenceString.split("\\s+");
				sentencesArray[i] = new String[taggedSentenceArray.length];			
			}	
		}
		else if (inputFormat.equals("conll")) {
			BufferedReader br = new BufferedReader(new StringReader(text));
			String line;
			sentences = new ArrayList<List<String>>();
			List<String> sentence = new ArrayList<String>();
			while ((line = br.readLine())!= null) {
				if (line.length() > 0) {
					String[] lineArray = line.split("\t");
					sentence.add(lineArray[1]+":"+lineArray[3]);
				}
				else {
					sentences.add(sentence);
					sentence = new ArrayList<String>();				
				}
			}
			if (!sentence.isEmpty()) {
				sentences.add(sentence);
			}
			sentencesArray = new String[sentences.size()][];
			for (int i=0; i < sentences.size();i++) {
				List<String> curSentence = sentences.get(i);
				String[] sentenceArray = new String[curSentence.size()];
				sentencesArray[i] =curSentence.toArray(sentenceArray);
			}			
		}
		tokenizedOutput = new String[sentences.size()][][];
		for (int i = 0; i < sentencesArray.length; i++) {
			String[] curSentence = sentencesArray[i];
			tokenizedOutput[i] = new String[curSentence.length][12];
			String taggedSentenceString = null;		
			String[] taggedSentenceArray = null;
			if (inputFormat.equals("text")) {
				taggedSentenceString = taggedList.get(i);
				taggedSentenceArray = taggedSentenceString.split("\\s+");
				
			}
			else if (inputFormat.equals("conll")) {
				taggedSentenceArray = curSentence;
			}
			for (int j = 0; j < curSentence.length; j++) {
				String curEntry = taggedSentenceArray[j];
				String wordForm = "";
				String pos = "";
				int splitPoint = curEntry.indexOf(":");
				wordForm = curEntry.substring(0, splitPoint);
				pos = curEntry.substring(splitPoint+1,curEntry.length());
				if (pos.equals("::")) {
					wordForm = ":";
					pos = ":";
				}
				tokenizedOutput[i][j][0] = String.valueOf(j+1);
				sb.append(tokenizedOutput[i][j][0]+"\t");
				XMLString wfXml = new XMLString(wordForm);
				tokenizedOutput[i][j][1] = wordForm;
			//	tokenizedOutput[i][j][1] = wfXml.getXmlString();	
				tokenizedOutput[i][j][2] = "_";
				tokenizedOutput[i][j][3] = pos;
				tokenizedOutput[i][j][4] = pos;
				sb.append(tokenizedOutput[i][j][1]+"\t");
				sb.append("_\t");
				sb.append(tokenizedOutput[i][j][3]+"\t");
				sb.append(tokenizedOutput[i][j][3]+"\t");
				sb.append("_\t_\n");
				length++;
			}
			sb.append("\n");
		}
	/*	System.err.printf("Tagged %d words at %.2f words per second.\n",
				length, (Double.valueOf(length) / (Double.valueOf(System
						.currentTimeMillis()
						- start) / 1000)));*/
	//	return sb.toString();
		return tokenizedOutput;
	}
	
	private static String tag(String in, Tagger t) {
		Focus focus = new Focus();
		StringTokenizer str = new StringTokenizer(in);
		while(str.hasMoreTokens()) {
			String word = str.nextToken();
			if (word.endsWith(".") || word.endsWith("?")
						|| word.endsWith(":")
						|| word.endsWith(";")
						|| word.endsWith("!")
						|| word.endsWith(",")
						|| word.endsWith(".)")
						) {
					int length = word.length();
					String pref = "";
					String suff = "";
					if (length != 1) {
						pref = word.substring(0,length-1);
						suff = word.substring(length-1,length);
					}
					else {
						suff = word;
					}
					focus.add(pref);
					focus.add(suff);
				} else {
					focus.add(word);
				}

		}
		t.run(focus);
		return focus.toString();
	}
	
	public String transformTaggedOutputToStringChain(String[][] taggedOutput) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < taggedOutput.length-1;i++) {
			sb.append(taggedOutput[i][3]);
			sb.append(" ");
		}
		sb.append(taggedOutput[taggedOutput.length-1][3]);
		return sb.toString();
	}
	
	public String toString(String[][] taggedOutput) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < taggedOutput.length-1;i++) {
			sb.append(taggedOutput[i][1]+"/"+taggedOutput[i][3]);
			sb.append(" ");
		}
		sb.append(taggedOutput[taggedOutput.length-1][1]+"/"+taggedOutput[taggedOutput.length-1][3]);
		return sb.toString();
	}
	
	
}
