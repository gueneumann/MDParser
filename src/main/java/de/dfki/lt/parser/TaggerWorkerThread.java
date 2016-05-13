package de.dfki.lt.parser;

import java.util.List;
import java.util.StringTokenizer;

import com.schmeier.posTagger.focus.Focus;
import com.schmeier.posTagger.tagger.Tagger;

import pi.ParIterator;
import de.dfki.lt.data.Sentence;

public class TaggerWorkerThread extends Thread {

	private ParIterator<TagUnit> pi = null;
	private int id = -1;
	private String[] taggedArray;
	private String[][] sentencesArray;
	private Tagger tagger;
	
	public TaggerWorkerThread(int i, ParIterator<TagUnit> iter, String[] taggedArray, String[][] sentencesArray, Tagger[] taggers) {
		this.pi = iter;
		this.id = i;
		this.taggedArray = taggedArray;
		this.sentencesArray = sentencesArray;
		this.tagger = taggers[id];
	}
	
	public static String tag(String in, Tagger t) {
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
	
	public void run() {
		while (pi.hasNext()) {
			TagUnit tu = pi.next();
			List<String> sentence = tu.getSentence();
			int i = tu.getId();
		//	System.out.println("Hello from Thread "+id);
			StringBuilder sbSentence = new StringBuilder();
			
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
		//	System.out.println(sentenceString);
		//	System.out.println(tagger);
			String taggedSentenceString = tag(sentenceString, tagger);
			taggedArray[i] = taggedSentenceString;
			String[] taggedSentenceArray = taggedSentenceString.split("\\s+");
			sentencesArray[i] = new String[taggedSentenceArray.length];	
		}
		System.out.println("    Thread "+id+" has finished.");
	}
	
}
