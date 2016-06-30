package de.dfki.lt.mdparser.caller;


import java.io.IOException;
import java.lang.reflect.Array;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import cases.ParserTest;
import cases.TrainerTest;
import de.bwaldvogel.liblinear.*;
import de.dfki.lt.mdparser.archive.Archivator;

public class CompleteTest {

	public static void main(String[] args) throws IOException, InvalidInputDataException, NoSuchAlgorithmException {
		
		String trainFile = "resources/input/english-train.conll";
		String goldFile = "resources/input/english-devel.conll";
		String modelFile = "modelEnglish.zip";


		String[] trainArgs = {trainFile};
		String[] parseArgs = {goldFile, modelFile};

		String[] dirs = {"split","splitA","splitF","splitO","splitC","splitModels","temp"};
		Archivator arch = new Archivator(modelFile,dirs);
		long s1 = System.currentTimeMillis();
		TrainerTest.main(trainArgs);
		long s2 = System.currentTimeMillis();

		//	ModelEditorTest.main(null);		
		arch.pack();
		//		arch.delTemp();
		TrainerTest.deleteOld(dirs);

		long s3 = System.currentTimeMillis();
		ParserTest.main(parseArgs);
		long s4 = System.currentTimeMillis();
		System.out.println("Complete Training time: "+((s2-s1)) +" milliseconds.");
		System.out.println("Complete Parsing time: "+((s4-s3))+" milliseconds.");
	}
}
