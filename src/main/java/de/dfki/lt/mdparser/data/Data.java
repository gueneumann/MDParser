package de.dfki.lt.mdparser.data;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class Data {

	private Sentence[] sentences;

	private int infoSize = 12;
	
	public Data() {
		
	}

	public Data(String inputFile, boolean train) throws IOException {
		FileInputStream in = new FileInputStream(inputFile);
		BufferedInputStream bis = new BufferedInputStream(in, 8000);
		InputStreamReader ir = new InputStreamReader(bis,"UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line;
		List<String> curSent = new ArrayList<String>(50);
		List<Sentence> sentences = new ArrayList<Sentence>(40000);
		while ((line = fr.readLine()) != null) {
			if (line.length() > 0) {
				curSent.add(line);		
			}
			else {
				String[][] sentArray = new String[curSent.size()][infoSize];
				for (int i=0; i < curSent.size();i++) {

					String[] curWord = curSent.get(i).split("\\s");
				//	if (curWord[7].isEmpty() ) System.out.println(curWord[6]);
				//	if (!train) {
				//		sentArray[i][8] = "_";
				//		sentArray[i][9] = "_";
				//	}
					for (int j=0; j < curWord.length;j++) {
						if (!train && (j== 6 || j==7 || j==8 || j==9)) {
							sentArray[i][j] = "_";							
						}
						else if (train && (j==8 || j==9)) {
							sentArray[i][j] = "_";
						}
						else {
							sentArray[i][j] = curWord[j];
						}
					}
			//		sentArray[i][5] = "_";
				}
				sentences.add(new Sentence(sentArray));	
				curSent = new ArrayList<String>();
			}
		}
		fr.close();
		this.sentences = new Sentence[sentences.size()];
		for (int i = 0; i < sentences.size();i++) {
			this.sentences[i] = sentences.get(i);	
			
		}
	}
	
	// GN: not used
	/*public Data(String inputFile, boolean train, int col) throws IOException {
		FileInputStream in = new FileInputStream(inputFile);
		BufferedInputStream bis = new BufferedInputStream(in, 8000);
		InputStreamReader ir = new InputStreamReader(bis,"UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line;
		List<String> curSent = new ArrayList<String>(50);
		List<Sentence> sentences = new ArrayList<Sentence>(40000);
		while ((line = fr.readLine()) != null) {
			if (line.length() > 0) {
				curSent.add(line);		
			}
			else {
				String[][] sentArray = new String[curSent.size()][infoSize];
				for (int i=0; i < curSent.size();i++) {

					String[] curWord = curSent.get(i).split("\\s");
				//	if (curWord[7].isEmpty() ) System.out.println(curWord[6]);
					for (int j=0; j < curWord.length;j++) {
						if (!train && (j == col)) {
							sentArray[i][j]	= "_";
						}
						else {
							sentArray[i][j]	= curWord[j];
						}

					}
			//		sentArray[i][5] = "_";
				}
				sentences.add(new Sentence(sentArray));	
				curSent = new ArrayList<String>();
			}
		}
		fr.close();
		this.sentences = new Sentence[sentences.size()];
		for (int i = 0; i < sentences.size();i++) {
			this.sentences[i] = sentences.get(i);	
			
		}
	}*/
		
	public void setSentences(Sentence[] sentences) {
		this.sentences = sentences;
	}

	public Sentence[] getSentences() {
		return sentences;
	}


	public void printToFile(String resultFile) throws IOException {
		FileOutputStream out = new FileOutputStream(resultFile);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter fw = new BufferedWriter(or);
		for (int n=0; n < sentences.length;n++) {
			Sentence s = sentences[n];
			fw.append(s.toString());
			fw.append("\n");
		}	
		fw.close();
	}
	
}
