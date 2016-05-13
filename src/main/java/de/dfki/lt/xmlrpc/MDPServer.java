package de.dfki.lt.xmlrpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.WebServer;






import com.schmeier.posTagger.focus.Focus;
import com.schmeier.posTagger.tagger.Tagger;

import de.dfki.lt.algorithm.CovingtonAlgorithm;
import de.dfki.lt.algorithm.ParsingAlgorithm;
import de.dfki.lt.algorithm.StackAlgorithm;
import de.dfki.lt.archive.Archivator;
import de.dfki.lt.data.Sentence;
import de.dfki.lt.features.Alphabet;
import de.dfki.lt.features.CovingtonFeatureModel;
import de.dfki.lt.features.FeatureExtractor;
import de.dfki.lt.features.FeatureModel;
import de.dfki.lt.features.StackFeatureModel;
import de.dfki.lt.outputformat.XMLString;
import de.dfki.lt.parser.Parser;
import edu.northwestern.at.morphadorner.corpuslinguistics.sentencesplitter.DefaultSentenceSplitter;
import edu.northwestern.at.morphadorner.corpuslinguistics.sentencesplitter.SentenceSplitter;
import edu.northwestern.at.morphadorner.corpuslinguistics.tokenizer.DefaultWordTokenizer;
import edu.northwestern.at.morphadorner.corpuslinguistics.tokenizer.WordTokenizer;

public class MDPServer {
	
	/**
	 * Index of the column for the word form in the output file.
	 */
	private static int wordFormIndex;

	/**
	 * Index of the column for the part of speech in the output file.
	 */
	private static int posIndex;

	/**
	 * Index of the column for the head in the output file.
	 */
	private static int headIndex;

	/**
	 * Index of the column for the label in the output file.
	 */
	private static int labelIndex;
	
	private static SentenceSplitter sentenceSplitter;
	private static WordTokenizer wordTokenizer;
	
	/**
	 * POS-Tagger for German.
	 */
	private static Tagger germanTagger;

	/**
	 * POS-Tagger for English.
	 */
	private static Tagger englishTagger;
	
	/** 
	* Path to the English POS-Tagger.
	 */
	private static String modelFilePosTaggerEnglish;

	/**
	 * Path to the German POS-Tagger.
	 */
	private static String modelFilePosTaggerGerman;
	

	private static WebServer webServer;

	private static Parser parser;

	private static Alphabet alphabetParser;

	private static Alphabet alphabetLabeler;

	private static FeatureModel fm;

	private static ParsingAlgorithm pa;

	private int length;
	private String[][][] tokenizedOutput;
	
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
	
	public String tagText(String text, String language, String inputFormat) throws Exception {
		long start = System.currentTimeMillis();
		Tagger tagger = null;
		if (language.equals("german")) {
			tagger = germanTagger;
		} else if (language.equals("english")) {
			tagger = englishTagger;
		}
		System.err.println(tagger);
		length = 0;
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
					sentence.add(lineArray[wordFormIndex]+":"+lineArray[posIndex]);
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
			tokenizedOutput[i] = new String[curSentence.length][10];
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
				tokenizedOutput[i][j][wordFormIndex] = wfXml.getXmlString();	
				tokenizedOutput[i][j][posIndex-1] = pos;
				tokenizedOutput[i][j][posIndex] = pos;
				sb.append(tokenizedOutput[i][j][wordFormIndex]+"\t");
				sb.append("_\t");
				sb.append(tokenizedOutput[i][j][posIndex]+"\t");
				sb.append(tokenizedOutput[i][j][posIndex]+"\t");
				sb.append("_\t_\n");
				length++;
			}
			sb.append("\n");
		}
		System.err.printf("Tagged %d words at %.2f words per second.\n",
				length, (Double.valueOf(length) / (Double.valueOf(System
						.currentTimeMillis()
						- start) / 1000)));
		return sb.toString();
	}
	
	public String tagSentence(String text, String language, String inputFormat) throws Exception {
		long start = System.currentTimeMillis();
		Tagger tagger = null;
		if (language.equals("german")) {
			tagger = germanTagger;
		} else if (language.equals("english")) {
			tagger = englishTagger;
		}
		length = 0;	
		StringBuilder sb = new StringBuilder();
		String sentenceString = text;
		String taggedSentenceString = null;
		String[] sentenceArray = null;
		if (inputFormat.equals("text")) {
			taggedSentenceString =  tag(sentenceString, tagger);
			sentenceArray = taggedSentenceString.split("\\s+");
		}
		else if (inputFormat.equals("conll")) {
			BufferedReader br = new BufferedReader(new StringReader(text));
			String line;
			List<String> sentence = new ArrayList<String>();
			while ((line = br.readLine())!= null) {
				if (line.length() > 0) {
					String[] lineArray = line.split("\t");
					sentence.add(lineArray[wordFormIndex]+":"+lineArray[posIndex]);
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
			tokenizedOutput[0][j][wordFormIndex] = wfXml.getXmlString();	
			sb.append(tokenizedOutput[0][j][wordFormIndex]+"\t_\t");
			tokenizedOutput[0][j][posIndex-1] = pos;
			tokenizedOutput[0][j][posIndex] = pos;
			sb.append(tokenizedOutput[0][j][posIndex]+"\t"+tokenizedOutput[0][j][posIndex]+"\t_\t_\t_\n");			
		}
		sb.append("\n");
		System.err.printf("Tagged %d words at %.2f words per second.\n",
				length, (Double.valueOf(length) / (Double.valueOf(System
						.currentTimeMillis()
						- start) / 1000)));
		return sb.toString();
	}
	
	public String parseSentence(String text, String language, String inputFormat) throws Exception {
		tagSentence(text, language, inputFormat);
		FeatureExtractor fe = new FeatureExtractor();
		FeatureModel fm = new CovingtonFeatureModel(alphabetParser, fe);
		ParsingAlgorithm pa = new CovingtonAlgorithm();
		pa.setParser(parser);
		Sentence sent = new Sentence(tokenizedOutput[0]);
		pa.processCombined(sent, fm, true, parser.getSplitMap());
		return sent.toString();
	}
	
	public String parseText(String text, String language, String inputFormat) throws Exception {
		tagText(text, language, inputFormat);
		length = 0;
		long start = System.currentTimeMillis();
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < tokenizedOutput.length;i++) {
			Sentence sent = new Sentence(tokenizedOutput[i]);
			length += sent.getSentArray().length;
		//	pa.process(sent, fm, true, parser.getSplitMap(), parser.getSplitMapL());
			pa.processCombined(sent, fm, true, parser.getSplitMap());
			sb.append(sent.toString()+"\n");
		}
		Double seconds = Double.valueOf(System.currentTimeMillis()	- start) / 1000;
		System.err.printf("%.2f seconds. Average sentence length %.2f. Parsed %d words at %.2f words per second (%d sentences, %,.2f sentences per second).\n", 
				seconds,
				Double.valueOf(length)/Double.valueOf(tokenizedOutput.length),
				length, 			
				Double.valueOf(length) / seconds,
				tokenizedOutput.length, 
				Double.valueOf(tokenizedOutput.length) / seconds
				);
		return sb.toString();
	}
	
	public static void main(String[] args) throws InvalidPropertiesFormatException, IOException, XmlRpcException {
		Properties props = new Properties();
		FileInputStream in = null;
		if (args.length == 1) {
			in = new FileInputStream(new File(args[0]));
		} else {
			in = new FileInputStream(new File("propsServer.xml"));
		}
		props.loadFromXML(in);	
		wordFormIndex = 1;
		posIndex = 4;
		headIndex = 6;
		labelIndex = 7;
		wordTokenizer = new DefaultWordTokenizer();
		sentenceSplitter = new DefaultSentenceSplitter();
		String language = props.getProperty("language");
		if (language.equals("english")) {
			modelFilePosTaggerEnglish = props.getProperty("modelFilePOSTaggerEnglish");
			englishTagger = new Tagger(modelFilePosTaggerEnglish);
			englishTagger.init();
		}
		else if (language.equals("german")) {
			modelFilePosTaggerGerman = props.getProperty("modelFilePOSTaggerGerman");
			germanTagger = new Tagger(modelFilePosTaggerGerman);
			germanTagger.init();
		}
		parser = new Parser();
		String[] dirs = {"split","splitA","splitF","splitO","splitC","splitModels","temp"};
		Archivator arch = new Archivator(props.getProperty("modelsFile"),dirs);
		arch.extract();
		alphabetParser = new Alphabet(arch.getParserAlphabetInputStream());
	//gds	alphabetLabeler = new Alphabet(arch.getLabelerAlphabetInputStream());
		parser.setNumberOfClassesParser(alphabetParser.getMaxLabelIndex()-1);
	//gds	parser.setNumberOfClassesLabeler(alphabetLabeler.getMaxLabelIndex()-1);
		parser.readSplitModels(arch);
	//gds	parser.readSplitModelsL(arch);
		String algorithm = props.getProperty("algorithm");
		FeatureExtractor fe = new FeatureExtractor();
		if (algorithm.equals("covington")) {
		//gds	fm = new CovingtonFeatureModel(alphabetParser,fe);
			fm = new CovingtonFeatureModel(alphabetParser, fe);
			pa = new CovingtonAlgorithm();
		}
		else if (algorithm.equals("stack")) {
		//gds	fm = new StackFeatureModel(alphabetParser, alphabetLabeler,fe);
			fm = new StackFeatureModel(alphabetParser,fe);
			pa = new StackAlgorithm();		
		}
		pa.setParser(parser);
		webServer = new WebServer(Integer.valueOf(props.getProperty("port")));
		webServer.getXmlRpcServer().setMaxThreads(10);
		PropertyHandlerMapping phm = new PropertyHandlerMapping();
		phm.addHandler("parser", de.dfki.lt.xmlrpc.MDPServer.class);
		webServer.getXmlRpcServer().setHandlerMapping(phm);
		XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) webServer.getXmlRpcServer().getConfig();
		serverConfig.setEnabledForExtensions(true);
		serverConfig.setContentLengthOptional(false);
		webServer.start();
		System.out.println("Server running...");
		
		
	}
}
