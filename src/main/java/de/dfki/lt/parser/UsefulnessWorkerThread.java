package de.dfki.lt.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.dfki.lt.data.Data;
import de.dfki.lt.data.Sentence;
import de.dfki.lt.eval.Eval;
import de.dfki.lt.features.Alphabet;
import pi.ParIterator;

public class UsefulnessWorkerThread extends Thread {

	private int id;
	private ParIterator<Integer> pi;
	private BufferedWriter bw;
	
	public UsefulnessWorkerThread(int i, ParIterator<Integer> iter, BufferedWriter bw) {
		this.id = i;
		this.pi = iter;
		this.bw = bw;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public void setPi(ParIterator<Integer> pi) {
		this.pi = pi;
	}

	public ParIterator<Integer> getPi() {
		return pi;
	}

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
	
	public void run() {
		String goldFile = "input/english.train";
		String evalFile = "input/english.devel";
		while (pi.hasNext()) {
			Integer n = pi.next();
		//	System.out.println("Hello from Thread "+id);
			System.out.println("Starting with sentence "+n);
			HashSet<Integer> alreadyUsed = new HashSet<Integer>();
			//choose randomly x sentences for tr1 (without n)
			//choose randomly x sentences for tr2 (without n)
			int y = 0;
			int x = 10000;
			Random r = new Random();
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
			Trainer trainer = new Trainer();
			String[] dirs = {"split"+n,"splitA"+n,"splitF"+n,"splitO"+n,"splitC"+n,"splitModels"+n,"temp"+n};
			String alphabetFileParser = "temp"+n+"/alphaParser.txt";
			String resultFile = "temp"+n+"/parsed.txt";
			String splitModelsDir = "splitModels"+n;
			String algorithm = "covington";
			String splitFile = "temp"+n+"/split.txt";
			UsefulnessTest.create(dirs);
			Data dTrain = null;
			try {
				dTrain = new Data(goldFile, true);
			} catch (IOException e9) {
				// TODO Auto-generated catch block
				e9.printStackTrace();
			}
			Data curData = createData(alreadyUsed,dTrain);
			try {
				trainer.createAndTrainWithSplittingFromDisk(algorithm,curData,splitModelsDir, alphabetFileParser,alphabetFileLabeler,splitFile,
						new File("splitA"+n), new File("splitO"+n), new File("splitF"+n), new File("split"+n), new File("splitC"+n));
			} catch (IOException e7) {
				// TODO Auto-generated catch block
				e7.printStackTrace();
			} catch (InvalidInputDataException e7) {
				// TODO Auto-generated catch block
				e7.printStackTrace();
			}
			System.out.println("Finished training for sentence "+n);
		
			//train model for tr1
			//train model for tr2
			//evaluate model for tr1
			Data dTest = null;
			try {
				dTest = new Data(evalFile, false);
			} catch (IOException e8) {
				// TODO Auto-generated catch block
				e8.printStackTrace();
			}
			Parser parser = new Parser();
			Alphabet alphabetParser = null;
			try {
				alphabetParser = new Alphabet(alphabetFileParser);
			} catch (IOException e7) {
				// TODO Auto-generated catch block
				e7.printStackTrace();
			}
			parser.setNumberOfClassesParser(alphabetParser.getMaxLabelIndex()-1);
			try {
				parser.parseCombined(algorithm,dTest,  alphabetParser, false,new File(splitFile),splitModelsDir);
			} catch (IOException e6) {
				// TODO Auto-generated catch block
				e6.printStackTrace();
			}
			System.out.println("Finished evaluation for sentence "+n);
			try {
				dTest.printToFile(resultFile);
			} catch (IOException e5) {
				// TODO Auto-generated catch block
				e5.printStackTrace();
			}
			Eval ev = null;
			try {
				ev = new Eval(evalFile, resultFile,6,6,7,7);
			} catch (IOException e6) {
				// TODO Auto-generated catch block
				e6.printStackTrace();
			}
			try {
				bw.append(n+" "+String.valueOf(ev.getParentsAccuracy())+" "+String.valueOf(ev.getLabelsAccuracy())+" orig\n");
			} catch (IOException e5) {
				// TODO Auto-generated catch block
				e5.printStackTrace();
			}
			try {
				bw.flush();
			} catch (IOException e4) {
				// TODO Auto-generated catch block
				e4.printStackTrace();
			}
			UsefulnessTest.delete(dirs);
		//	TrainerTest.deleteOld(dirs);
		//	TrainerTest.createNew(dirs);
		//	addSentence(n, dTrain, curData);
		//	alreadyUsed.add(n);
			String[] dirs2 = {"splitN"+n,"splitAN"+n,"splitFN"+n,"splitON"+n,"splitCN"+n,"splitModelsN"+n,"tempN"+n};
			alphabetFileParser = "tempN"+n+"/alphaParser.txt";
			resultFile = "tempN"+n+"/parsed.txt";
			splitModelsDir = "splitModelsN"+n;
			splitFile = "tempN"+n+"/split.txt";
			UsefulnessTest.create(dirs2);
			try {
				dTrain = new Data(goldFile, true);
			} catch (IOException e4) {
				// TODO Auto-generated catch block
				e4.printStackTrace();
			}
			alreadyUsed.add(n);
			curData = createData(alreadyUsed, dTrain);			
			try {
				trainer.createAndTrainWithSplittingFromDisk(algorithm,curData,splitModelsDir, alphabetFileParser,alphabetFileLabeler,splitFile,
						new File("splitAN"+n), new File("splitON"+n), new File("splitFN"+n), new File("splitN"+n), new File("splitCN"+n));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidInputDataException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Finished training for sentence "+n+" with sentence");
			parser = new Parser();
			try {
				dTest = new Data(evalFile, false);
			} catch (IOException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			}
			try {
				alphabetParser = new Alphabet(alphabetFileParser);
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			parser.setNumberOfClassesParser(alphabetParser.getMaxLabelIndex()-1);
			try {
				parser.parseCombined(algorithm,dTest, alphabetParser, false,new File(splitFile),splitModelsDir);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Finished evaluation for sentence "+n+" with sentence");
			try {
				dTest.printToFile(resultFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				ev = new Eval(evalFile, resultFile,6,6,7,7);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				bw.append(n+" "+String.valueOf(ev.getParentsAccuracy())+" "+String.valueOf(ev.getLabelsAccuracy())+" use\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				bw.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			UsefulnessTest.delete(dirs2);
			//evaluate model for tr2
			//average performance over two
			//print sentence usefulness of n to a log file
		}
	//	System.out.println("    Thread "+id+" has finished.");
	}
	
}
