package de.dfki.lt.xmlrpc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import de.dfki.lt.outputformat.ConllOutput;
import de.dfki.lt.outputformat.ConllXMLOutput;
import de.dfki.lt.outputformat.StanfordOutput;
import de.dfki.lt.outputformat.TripleOutput;

public class MDPClient {
	
	private XmlRpcClient client;
	
	/**
	 * Constructs a client.
	 * @param serverUrl <code>String</code> value for the server address.
	 * @throws MalformedURLException if the URL is malformed.
	 */
	public MDPClient(String serverUrl) throws MalformedURLException  {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();        
        config.setServerURL(new URL(serverUrl));
        config.setEnabledForExceptions(true);
        config.setConnectionTimeout(60*100);
        config.setReplyTimeout(60 * 1000);	        
        client = new XmlRpcClient();
        client.setConfig(config);
	}
	
	public  static String readFileToString(String textFile, String inputFormat) throws IOException {
		FileInputStream in = new FileInputStream(textFile);
		InputStreamReader ir = new InputStreamReader(in, "UTF8");
		BufferedReader fr = new BufferedReader(ir);
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = fr.readLine()) != null) {
			sb.append(line);
			if (inputFormat.equals("conll")) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}
	

	public static void printStringToFile(String string, String fileOutput) throws IOException {
		FileOutputStream out = new FileOutputStream(fileOutput);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter fw = new BufferedWriter(or);		
		fw.write(string);
		fw.close();
	}
	
	public String tagText(Vector<String> v) throws XmlRpcException {
		return (String) client.execute("parser.tagText",v);
	}
	
	public String tagSentence(Vector<String> v) throws XmlRpcException {
		return (String) client.execute("parser.tagSentence",v);
	}
	
	public String parseSentence(Vector<String> v) throws XmlRpcException {
		return (String) client.execute("parser.parseSentence",v);
	}
	public String parseText(Vector<String> v) throws XmlRpcException {
		return (String) client.execute("parser.parseText",v);
	}
	
	
	public static void main(String[] args) throws IOException, XmlRpcException {
		Properties props = new Properties();
		FileInputStream in = null;
		if (args.length == 1) {
			in = new FileInputStream(new File(args[0]));
		}
		else {
			in = new FileInputStream(new File("propsClient.xml"));
		}
		props.loadFromXML(in);
		checkProps(props);
		String server = props.getProperty("server");
		MDPClient mdpClient = new MDPClient(server);
		String fileName = props.getProperty("inputFile");
		String inputType = props.getProperty("inputType");
		String language = props.getProperty("language");
		String inputFormat = props.getProperty("inputFormat");
		File[] allFiles = null;
		if (inputType.equals("file")) {
			allFiles = new File[1];
			allFiles[0] = new File(fileName);
		}
		else if (inputType.equals("dir")) {
			allFiles = new File(fileName).listFiles();
		}
		for (int i=0; i < allFiles.length;i++) {
			fileName = allFiles[i].getPath();
			String outputFile = props.getProperty("outputFile");
			String taggedFile = props.getProperty("taggedFile");
			if (inputType.equals("dir")) {
				outputFile = String.format("%s/%04d.txt",outputFile,i);
				taggedFile = String.format("%s/%04d_morph.txt",taggedFile,i);
			}
			Vector<String> v = new Vector<String>();
			String inputString = readFileToString(fileName,inputFormat);
			v.add(inputString);
			v.add(language);
			v.add(inputFormat);
			String taggedSentence = "";//mdpClient.parseSentence(v);
			String parsed = "";
			String mode = props.getProperty("mode");
			if (mode.equals("parse")) {
				parsed = mdpClient.parseText(v);
				String outputFormat = props.getProperty("outputFormat");
				String output = "";
				if (outputFormat.equals("stanford")) {
					String stanfordMorphString = new StanfordOutput(parsed).getTaggedOutput();
					String stanfordParsedString = new StanfordOutput(parsed).getParsedOutput();
					printStringToFile(stanfordMorphString, taggedFile);
					printStringToFile(stanfordParsedString, outputFile);
				}
				else {
					if (outputFormat.equals("conll")) {
						output = new ConllOutput(parsed).getOutput();
					}
					else if (outputFormat.equals("conllxml")) {
						output = new ConllXMLOutput(parsed).getOutput();
					}
					else if (outputFormat.equals("triple")) {
						output = new TripleOutput(parsed).getOutput();
					}
					printStringToFile(output, outputFile);
				}
			}
			else if (mode.equals("tag")) {
				parsed = mdpClient.tagText(v);
				printStringToFile(parsed, taggedFile);
			}
			
		}
	}

	private static void checkProps(Properties props) {
		//input
		String inputType = props.getProperty("inputType");
		if (!inputType.equals("dir") && !inputType.equals("file"))	{
			System.out.println("Possible values for the property 'inputType': 'dir' or 'file'");
			System.exit(0);
		}
		else {
			String inputFile = props.getProperty("inputFile");
			if (inputType.equals("dir")) {				
				File dir = new File(inputFile);
				if (!dir.isDirectory()) {
					System.out.println("Specified value for the property 'inputFile' is not a directory");
					System.exit(0);
				}
			}
			else if (inputType.equals("file")) {
				File file = new File(inputFile);
				if (!file.exists()) {
					System.out.println("Specified file for the property 'inputFile' does not exist");
					System.exit(0);
				}
				else if (file.isDirectory()) {
					System.out.println("Specified value for the property 'inputFile' is a directory (should be a file)");
					System.exit(0);
				}
			}
		}
		//input format
		String inputFormat = props.getProperty("inputFormat");
		if (!inputFormat.equals("text") && !inputFormat.equals("conll")) {
			System.out.println("Possible values for the property 'inputFormat': 'text' or 'conll'");
			System.exit(0);
		}
		//language
		String language = props.getProperty("language");
		if (!language.equals("german") && !language.equals("english")) {
			System.out.println("Possible values for the property 'language': 'german' or 'english'");
			System.exit(0);
		}
		//outputformat
		String outputFormat = props.getProperty("outputFormat");
		if (!outputFormat.equals("conll") && !outputFormat.equals("conllxml") && !outputFormat.equals("stanford") && ! outputFormat.equals("triple")) {
			System.out.println("Possible values for the property 'outputFormat': 'conll', 'conllxml', 'stanford' or 'triple'");
			System.exit(0);
		}
		//server
		String serverUrl = props.getProperty("server");
		try {
			URL u = new URL(serverUrl);
		} catch (MalformedURLException e) {
			System.out.println("Malformed value for the property 'server'");
			System.exit(0);
		}
		//mode 
		String mode = props.getProperty("mode");
		if (!mode.equals("parse") && !mode.equals("tag")) {
			System.out.println("Possible values for the property 'mode': 'parse' or 'tag'");
			System.exit(0);
		}
		else {
			if (mode.equals("tag")) {
				String taggedFile = props.getProperty("taggedFile");
				File file = new File(taggedFile);
			    if (file.isDirectory()) {
					System.out.println("Specified value for the property 'taggedFile' is a directory (should be a file)");
					System.exit(0);
				}
			}
		}
	}
}
