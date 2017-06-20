package de.dfki.lt.mdparser.caller;


import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import de.bwaldvogel.liblinear.*;
import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.parser.Trainer;
import de.dfki.lt.mdparser.parser.TrainerMem;

public class MDPtrainer {

	String trainFile;
	String modelFile;

	String[] trainArgs;
	String[] parseArgs;

	String[] dirs = {"split","splitA","splitF","splitO","splitC","splitModels","temp"};
	String splitModelsDir = "splitModels";
	String algorithm = "covington";
	String splitFile = "temp/split.txt";
	String alphabetFileParser = "temp/alphaParser.txt";
	String alphabetFileLabeler = "temp/alphaLabeler.txt";
	
	public String getAlgorithm() {
		return this.algorithm;
	}
	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}


	public MDPtrainer(){
	}
	
	public MDPtrainer(String algorithm){
		this.algorithm = algorithm;
	}


	public static void createNew(String[] dirs) {
		for (int i=0; i < dirs.length;i++) {
			String dir = dirs[i];
			File d = new File(dir);
			if (!d.exists()) {
				d.mkdir();
			}
		}
	}

	public static void deleteOld(String[] dirs) {
		for (int i=0; i < dirs.length;i++) {
			File[] files = new File(dirs[i]).listFiles();
			if (files != null) {
				for (int k=0; k < files.length; k++) {
					boolean b = files[k].delete();
					if (!b)
						System.out.println("Failed to delete "+files[k]);
				}
			}
		}
	}

	public void trainer(String trainFile, String archiveName) throws IOException, InvalidInputDataException, NoSuchAlgorithmException {


		deleteOld(dirs);
		createNew(dirs);

		Archivator arch = new Archivator(archiveName,dirs);
		Trainer trainer = new Trainer();

		long s1 = System.currentTimeMillis();

		trainer.createAndTrainWithSplittingFromDisk(algorithm,trainFile,
				splitModelsDir, alphabetFileParser,alphabetFileLabeler,splitFile);

		long s2 = System.currentTimeMillis();

		System.out.println("Complete Training time: "+((s2-s1)) +" milliseconds.");

		//	ModelEditorTest.main(null);		
		arch.pack();
		arch.delTemp();

		deleteOld(dirs);
	}

	public void trainerMem(String trainFile, String archiveName) throws IOException, InvalidInputDataException, NoSuchAlgorithmException {


		deleteOld(dirs);
		createNew(dirs);

		Archivator arch = new Archivator(archiveName,dirs);
		TrainerMem trainer = new TrainerMem();

		long s1 = System.currentTimeMillis();

		trainer.createAndTrainWithSplittingFromMemory(algorithm,trainFile,
				splitModelsDir, alphabetFileParser,alphabetFileLabeler,splitFile);

		long s2 = System.currentTimeMillis();

		System.out.println("Complete Training time: "+((s2-s1)) +" milliseconds.");

		//	ModelEditorTest.main(null);		
		arch.pack();
		arch.delTemp();

		deleteOld(dirs);
	}

	public static void main(String[] args) throws IOException, InvalidInputDataException, NoSuchAlgorithmException {
		MDPtrainer mdpTrainer = new MDPtrainer();
		//mdpTrainer.setAlgorithm("stack");
		// Run parallel version of trainier
		mdpTrainer.trainer("resources/input/ptb3-std-training.conll", "ptb3-std.zip");
		// Run non-parallel (in memory) version of trainer
		//mdpTrainer.trainerMem("resources/input/ptb3-std-training.conll", "ptb3-std-nonpara.zip");
	}
}
