package cases;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import com.schmeier.posTagger.focus.Focus;
import com.schmeier.posTagger.tagger.Tagger;

public class StringTest {
	
	public static String tag(String in, Tagger t) {
		Focus focus = new Focus();
		StringTokenizer str = new StringTokenizer(in);
		while(str.hasMoreTokens()) {
			String word = str.nextToken();
		/*	if (word.endsWith(".") || word.endsWith("?")
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
				} else {*/
					focus.add(word);
			//	}

		}
		t.run(focus);
		return focus.toString();
	}
	
	public static String readInput(String inputFile, String inputFormat) throws IOException {
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
	
	public static void main(String[] args) throws Exception {
	/*	String ab = "";
		for (int i=0; i < 100000; i++) {
			String a = "a";
			String b = "b";
			ab = a+b;
		}
		long start = System.currentTimeMillis();
		ab = "";
		for (int i=0; i < 10000; i++) {
			String a = "a";
			String b = "b";
			String c = "c";
			ab = a+b+c;
		}
		long end = System.currentTimeMillis();
		System.out.println(end-start+" --> "+ab);
		start = System.currentTimeMillis();
		MutableString msa = new MutableString();
		for (int i=0; i < 10000; i++) {
			msa = new MutableString("a");
			msa.append("b");
			msa.append("c");
		}
		end = System.currentTimeMillis();
		System.out.println(end-start+" --> "+msa);
		start = System.currentTimeMillis();
		StringBuilder abSb = new StringBuilder();
		for (int i=0; i < 10000; i++) {
			abSb.append("a");
			abSb.append("b");
			abSb.append("c");
		}
		end = System.currentTimeMillis();
		System.out.println(end-start+" --> "+abSb);*/
	/*	Properties props = new Properties();	
		FileInputStream in = null;
		if (args.length == 1) {
			in = new FileInputStream(new File(args[0]));
		} else {
			in = new FileInputStream(new File("mdpfull.xml"));
		}
		props.loadFromXML(in);
		MDParser mdp = new MDParser(props);
		in = new FileInputStream(new File("resources/de-sent.bin"));
		SentenceModel model = new SentenceModel(in);
		SentenceDetectorME sd = new SentenceDetectorME(model);
		String[] sents = sd.sentDetect("Am 1. Dezember geht die Welt unter. Frau Mueller kocht heute. Prof. Dr. Mueller geht weg.");
		for (int i=0; i < sents.length;i++) {
			System.out.println("sent number "+i+": "+sents[i]);
		}
		System.out.println(mdp.parseSentence("Am 1. Dezember geht die Welt unter.", "german", "text"));*/
		Properties props = new Properties();	
		FileInputStream in = null;
		if (args.length == 1) {
			in = new FileInputStream(new File(args[0]));
		} else {
			in = new FileInputStream(new File("mdpfull.xml"));
		}
		props.loadFromXML(in);
		String modelFilePosTaggerEnglish = props.getProperty("modelFilePOSTaggerEnglish");
		Tagger tagger = new Tagger(modelFilePosTaggerEnglish);
		tagger.init();
		String input = readInput("input/english.train", "conll");
		FileOutputStream out = new FileOutputStream("temp/asda.txt");
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter bw = new BufferedWriter(or);
	//	System.out.println(input);
		String taggedInput = tag(input,tagger);
	/*	bw.append(input);
		bw.append("\n");
		bw.append(taggedInput);
		bw.append("\n");
		bw.close();*/
		String[] inputArray = input.split(" ");
		System.out.println(inputArray.length);
		String[] taggedArray = taggedInput.split("  ");
		System.out.println(taggedArray.length);
	//	System.out.println(input);
		for (int i=0; i < inputArray.length;i++) {
		//	System.out.print(inputArray[i]);
		//	System.out.println(inputArray[i]);
			bw.append(inputArray[i]+" "+taggedArray[i]+"\n");
		}
	}
	
}
