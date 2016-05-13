package de.dfki.lt.mdparser.outputformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class ConllXMLOutput {
	private String output;
	
	public ConllXMLOutput(String inputString) throws IOException {
		BufferedReader br = new BufferedReader(new StringReader(inputString));
		StringBuilder sb = new StringBuilder();
		String line;
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<sentences>\n");
		int sentIndex = 1;
		boolean first = true;
		while ((line = br.readLine())!= null) {
			if (line.length() > 0) {
				if (first) {
					sb.append(" <sentence id=\""+sentIndex+"\">\n");
				}
				first = false;
				String[] lineArray = line.split("\t");
				XMLString xmlS = new XMLString(lineArray[1]);
				String xmlString = xmlS.getXmlString();
				sb.append("  <token id=\""+lineArray[0]+"\" wordForm=\""+xmlString+"\" pos=\""+lineArray[4]+
						"\" head=\""+lineArray[6]+"\" label=\""+lineArray[7]+"\"/>\n");
			}
			else {
				sb.append(" </sentence>\n");
				first = true;
				sentIndex++;
			}
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
