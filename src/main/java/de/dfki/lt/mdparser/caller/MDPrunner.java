package de.dfki.lt.mdparser.caller;


import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import de.bwaldvogel.liblinear.*;
import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.data.Data;
import de.dfki.lt.mdparser.eval.Eval;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.parser.Parser;

public class MDPrunner {
	private String[] dirs = {"split","splitA","splitF","splitO","splitC","splitModels","temp"};
	private String algorithm = "covington";
	private String resultFile = "temp/1.conll";
	private Parser parser = new Parser();
	private Data data = null;

	// Getters and setters
	public String getResultFile() {
		return resultFile;
	}
	public void setResultFile(String resultFile) {
		this.resultFile = resultFile;
	}
	public Data getData() {
		return data;
	}
	public void setData(Data data) {
		this.data = data;
	}

	// Class instantiation
	public MDPrunner(){
	}

	public MDPrunner(String resultFile){	
		this.resultFile = resultFile;
	}

	// Methods

	public void conllFileParsing(String conllFile, String resultFile, String modelFile) 
			throws IOException{
		this.parser = new Parser();
		this.data = new Data(conllFile, false);
		this.resultFile = resultFile;
		System.out.println("No. of sentences: "+ data.getSentences().length);

		Archivator arch = new Archivator(modelFile,dirs);
		arch.extract();
		Alphabet alphabetParser = new Alphabet(arch.getParserAlphabetInputStream());
		parser.setNumberOfClassesParser(alphabetParser.getMaxLabelIndex()-1);


		long s3 = System.currentTimeMillis();
		parser.parseCombined(algorithm, data, arch, alphabetParser, false);

		long s4 = System.currentTimeMillis();
		System.out.println("Parsing msec: " + (s4-s3));
		this.getData().printToFile(resultFile);
		this.evalParser(conllFile, resultFile);

	}
	
	public void evalParser(String conllFile, String resultFile) throws IOException{
		Eval ev = new Eval(conllFile, resultFile, 6, 6, 7, 7);
		System.out.println("Parent accuracy: " + ev.getParentsAccuracy());
		System.out.println("Label accuracy:  " + ev.getLabelsAccuracy());
	}


	public static void main(String[] args) throws IOException, InvalidInputDataException, NoSuchAlgorithmException {
		MDPrunner mdpRunner = new MDPrunner();
		String conllFile = "/Users/gune00/data/UniversalDependencies/conll/German/de-ud-test.conll";
		String resultFile = "/Users/gune00/data/UniversalDependencies/conll/German/de-ud-test-result.conll";
		String modelFile = "/Users/gune00/data/UniversalDependencies/conll/German/de-MDPmodel.zip";

		mdpRunner.conllFileParsing(conllFile, resultFile, modelFile);

	}
}
