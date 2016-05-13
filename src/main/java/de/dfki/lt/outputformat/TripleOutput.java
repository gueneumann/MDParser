package de.dfki.lt.outputformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import de.dfki.lt.data.Sentence;

public class TripleOutput {
	
	private String output;
	
	public TripleOutput(String inputString) throws IOException {
		BufferedReader br = new BufferedReader(new StringReader(inputString));
		StringBuilder sb = new StringBuilder();
		String line;
		List<String> curSent = new ArrayList<String>();
		List<Sentence> sentences = new ArrayList<Sentence>();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<sentences>\n");
		while ((line = br.readLine())!= null) {
			if (line.length() > 0) {
				curSent.add(line);				
			}
			else {
				String[][] sentArray = new String[curSent.size()][8];
				for (int i=0; i < curSent.size();i++) {
					String[] curWord = curSent.get(i).split("\t");
					for (int j=0; j < 8;j++) {
							sentArray[i][j] = curWord[j];
					}
				}
				sentences.add(new Sentence(sentArray));	
				curSent = new ArrayList<String>();
			}
		}
		for (int n = 0; n < sentences.size();n++) {
			sb.append(" <sentence id=\""+(n+1)+"\">\n");
			Sentence curS = sentences.get(n);	
			String[][] parsedSentence = curS.getSentArray();
			for (int i=0; i < parsedSentence.length;i++) {
				String parentWordForm = "null";
				String parentPos = "null";
				String label = parsedSentence[i][7];
				String wf = parsedSentence[i][1];
				XMLString xmlStringWf = new XMLString(wf);
				wf = xmlStringWf.getXmlString();
				String pos = parsedSentence[i][4];
				int parent = Integer.valueOf(parsedSentence[i][6]);
				if (parent != 0) {
					parentWordForm = parsedSentence[parent-1][1];
					xmlStringWf = new XMLString(parentWordForm);
					parentWordForm = xmlStringWf.getXmlString();
					parentPos = parsedSentence[parent-1][4];
				}
		//		sb.append(String.format("    <triple left=\"%d\" right=\"%s\">%s:%s %s %s:%s</triple>\n",
		//				 parent, (i+1), parentWordForm, parentPos, label, wf, pos));
				sb.append(String.format("    <triple id=\"%d\" head=\"%s\">%s:%s %s %s:%s</triple>\n",
										 parent, (i+1), wf, pos, label, parentWordForm, parentPos));
			}
			sb.append(" </sentence>\n");
		}
		sb.append("</sentences>");
		output = sb.toString();
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public String getOutput() {
		return output;
	}
	
}
