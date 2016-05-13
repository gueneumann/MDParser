package de.dfki.lt.parser;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import pi.ParIterator;
import pi.ParIteratorFactory;

import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.dfki.lt.archive.Archivator;
import de.dfki.lt.data.Data;
import de.dfki.lt.data.Sentence;
import de.dfki.lt.eval.Eval;
import de.dfki.lt.features.Alphabet;

public class UsefulnessTest {
	
	public static Data createData(Set<Integer> sentencesIds, Data d) {
		Data data = new Data();
		Sentence[] sentences = new Sentence[sentencesIds.size()];
		Sentence[] dSentences = d.getSentences();
		Iterator<Integer> iter = sentencesIds.iterator();
		int k = 0;
		while (iter.hasNext()) {
			Integer id = iter.next();
			sentences[k] = dSentences[id];
			k++;
		}
		data.setSentences(sentences);
		return data;
	}
	
	public static void addSentence(int n, Data d, Data toAdd) {
		Sentence s = d.getSentences()[n];
		Sentence[] oldSents = toAdd.getSentences();
		Sentence[] newSents = new Sentence[oldSents.length+1];
		for (int i=0; i < oldSents.length;i++) {
			newSents[i] = oldSents[i];
		}
		newSents[oldSents.length] = s;
	}
	
	public static void main(String[] args) throws IOException, InvalidInputDataException {
		String logFile = "usefulnessFIXED.txt";
		FileOutputStream out = new FileOutputStream(logFile);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter fw = new BufferedWriter(or);
		
		//read data
		
		String goldFile = "input/english.train";
		String evalFile = "input/english.devel";
		Data dTrain;
		Data dTest;
		
		//take sentence n
		int x = 10000;
		Random r = new Random();
		int y;
		Set<Integer> alreadyUsed;
		long start = System.currentTimeMillis();
	//	for (int n=0; n < sents.length;n++) {
		Runtime runtime = Runtime.getRuntime();
	    int numberOfProcessors = runtime.availableProcessors();
	    	//runtime.availableProcessors();
	    System.out.println("Number of processors used: "+numberOfProcessors);
		int threadCount = numberOfProcessors;	
		List<Integer> sentencesList = new ArrayList<Integer>();
		for (int n=0; n < 10000;n++) {
			sentencesList.add(n);
		}
		ParIterator<Integer> iter = ParIteratorFactory.createParIterator(sentencesList, threadCount);
		Thread[] threadPool = new UsefulnessWorkerThread[threadCount];
		for (int i = 0; i < threadCount; i++) {
		    threadPool[i] = new UsefulnessWorkerThread(i, iter,fw);
		    threadPool[i].start();
		}
		for (int i = 0; i < threadCount; i++) {
		    try {
		    	threadPool[i].join();
		    } catch(InterruptedException e) {
			e.printStackTrace();
		    }
		}
	/*	for (int n=1014; n < 1115;n++) {
			System.out.println("Starting with sentence "+n);
			alreadyUsed = new HashSet<Integer>();
			//choose randomly x sentences for tr1 (without n)
			//choose randomly x sentences for tr2 (without n)
			y = 0;
			while (y < x) {		
				Integer currentRandom = -1;
				while (currentRandom < 0) {
					int rand = r.nextInt(39000);
				//	System.out.println(rand+" "+alreadyUsed.contains(rand)+" "+y+" "+x+" "+sents.length+" "+alreadyUsed.size());
					if (rand != n) {
						if (!alreadyUsed.contains(rand)) {
							currentRandom = rand;
						}
					}
				}
				alreadyUsed.add(currentRandom);
				y++;
			}
			System.out.println("Finished composing random training set");
			
			String alphabetFileLabeler = "temp/alphaLabeler.txt";
		//	fw.append(alreadyUsed.toString()+"\n");
		//	fw.flush();
			Trainer trainer = new Trainer();
			String[] dirs = {"split"+n,"splitA"+n,"splitF"+n,"splitO"+n,"splitC"+n,"splitModels"+n,"temp"+n};
			String alphabetFileParser = "temp"+n+"/alphaParser"+n+".txt";
			String resultFile = "temp"+n+"/parsed.txt";
			String splitModelsDir = "splitModels"+n;
			String algorithm = "covington";
			String splitFile = "temp"+n+"/split.txt";
			create(dirs);
		//	TrainerTest.deleteOld(dirs);
		//	TrainerTest.createNew(dirs);
			dTrain = new Data(goldFile, true);
			Data curData = createData(alreadyUsed,dTrain);
			trainer.createAndTrainWithSplittingFromDisk(algorithm,curData,splitModelsDir, alphabetFileParser,alphabetFileLabeler,splitFile,
					new File("splitA"+n), new File("splitO"+n), new File("splitF"+n), new File("split"+n), new File("splitC"+n));
			System.out.println("Finished training for sentence "+n);
		
			//train model for tr1
			//train model for tr2
			//evaluate model for tr1
			dTest = new Data(evalFile, false);
			Parser parser = new Parser();
			Alphabet alphabetParser = new Alphabet(alphabetFileParser);
			parser.setNumberOfClassesParser(alphabetParser.getMaxLabelIndex()-1);
			parser.parseCombined(algorithm,dTest,  alphabetParser, false,new File(splitFile),splitModelsDir);
			System.out.println("Finished evaluation for sentence "+n);
			dTest.printToFile(resultFile);
			Eval ev = new Eval(evalFile, resultFile,6,6,7,7);
			fw.append(n+" "+String.valueOf(ev.getParentsAccuracy())+" "+String.valueOf(ev.getLabelsAccuracy())+" orig\n");
			fw.flush();
			delete(dirs);
		//	TrainerTest.deleteOld(dirs);
		//	TrainerTest.createNew(dirs);
		//	addSentence(n, dTrain, curData);
		//	alreadyUsed.add(n);
			String[] dirs2 = {"splitN"+n,"splitAN"+n,"splitFN"+n,"splitON"+n,"splitCN"+n,"splitModelsN"+n,"tempN"+n};
			alphabetFileParser = "tempN"+n+"/alphaParser"+n+".txt";
			resultFile = "tempN"+n+"/parsed.txt";
			splitModelsDir = "splitModelsN"+n;
			splitFile = "tempN"+n+"/split.txt";
			create(dirs2);
			dTrain = new Data(goldFile, true);
			alreadyUsed.add(n);
			curData = createData(alreadyUsed, dTrain);			
			trainer.createAndTrainWithSplittingFromDisk(algorithm,curData,splitModelsDir, alphabetFileParser,alphabetFileLabeler,splitFile,
					new File("splitAN"+n), new File("splitON"+n), new File("splitFN"+n), new File("splitN"+n), new File("splitCN"+n));
			System.out.println("Finished training for sentence "+n+" with sentence");
			parser = new Parser();
			dTest = new Data(evalFile, false);
			alphabetParser = new Alphabet(alphabetFileParser);
			parser.setNumberOfClassesParser(alphabetParser.getMaxLabelIndex()-1);
			parser.parseCombined(algorithm,dTest, alphabetParser, false,new File(splitFile),splitModelsDir);
			System.out.println("Finished evaluation for sentence "+n+" with sentence");
			dTest.printToFile(resultFile);
			ev = new Eval(evalFile, resultFile,6,6,7,7);
			fw.append(n+" "+String.valueOf(ev.getParentsAccuracy())+" "+String.valueOf(ev.getLabelsAccuracy())+" use\n");
			fw.flush();
			delete(dirs2);
			//evaluate model for tr2
			//average performance over two
			//print sentence usefulness of n to a log file
		}*/
		fw.close();
		long end = System.currentTimeMillis();
		System.out.println(end-start);

		
	
	}

	public static void create(String[] dirs) {
		for (int i=0; i < dirs.length;i++) {
			String dir = dirs[i];
			File d = new File(dir);
			if (!d.exists()) {
				d.mkdir();
			}
		}	
	}

	public static void delete(String[] dirs) {
		for (int i=0; i < dirs.length;i++) {
			File[] files = new File(dirs[i]).listFiles();
			if (files != null) {
				for (int k=0; k < files.length; k++) {
					boolean b = files[k].delete();
				//	System.out.println("Deleted "+files[k]+" "+b);
				}
				new File(dirs[i]).delete();
			}
		}
		
	}
}
