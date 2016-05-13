package de.dfki.lt.parser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import de.dfki.lt.data.Sentence;
import de.dfki.lt.features.Alphabet;
import de.dfki.lt.model.ModelEditor;
import pi.ParIterator;

public class CompactiseWorkerThread extends Thread {

	private ParIterator<File> pi;
	private int id;
	private Alphabet alphaParser;
	private Trainer trainer;
	private double    bias             = -1;
	private Problem   prob             = null;
	private Parameter param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.1);
	private String splitModelsDir;
	
	public CompactiseWorkerThread(int i, ParIterator<File> iter, Alphabet alphaParser, String splitModelsDir) {
		this.id = i;
		this.pi = iter;
		this.alphaParser = alphaParser;
		this.prob = prob;
		this.param = param;
		this.trainer = new Trainer();
		this.splitModelsDir = splitModelsDir;
	}

	
	public void run() {
		while (pi.hasNext()) {
			System.out.println("Hello from Thread in CompactiseWorkerThread " + id);
			
			
			File element = pi.next();
			
			System.out.println("... Steps: compactifize/sort training file " + element);
			System.out.println("... and store in splitC; read problem and call trainer, "
					+ "and finally save models and alphabet, and edit them. ");
			List<HashMap<Integer,Integer>> compactList = null;
			int[][] compactArray = null;
			try {
			//	compactList = trainer.compactiseTrainingDataFileInt(element,alphaParser.getMaxIndex(),new File("splitC"));
				compactArray = trainer.compactiseTrainingDataFile(element, alphaParser.getMaxIndex(), new File("splitC"));
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			//	System.out.println("new to old size: "+compactArray[0].length);
			try {
					trainer.readProblem("splitC/"+element.getName());
					prob = trainer.getProblem();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InvalidInputDataException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			
			// GN: call the trainer
				Linear.disableDebugOutput();
				Model m = Linear.train(prob, param);
			//	Linear.saveModel(new File(splitModelsDir+"/"+element.getName()), m);
				
				// GN: and save the training files
				try {
					m.save(new File(splitModelsDir+"/"+element.getName()));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			//	saveModel(m, compactArray, (new File(splitModelsDir+"/"+curentTrainingFile.getName())));
				try {
				//	trainer.saveAlphabetInt(alphaParser, m, compactList, new File("splitA/"+element.getName()));
					trainer.saveAlphabet(alphaParser, m, compactArray, new File("splitA/"+element.getName()));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			//	System.out.println(alphaParser.getMaxIndex());
				ModelEditor me = null;
				try {
					me = new ModelEditor(new File(splitModelsDir+"/"+element.getName()), "splitA/"+element.getName(),true);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
			//		me.editAlphabetAndModelInt("splitA/"+element.getName(), splitModelsDir+"/"+element.getName());
					me.editAlphabetAndModel("splitA/"+element.getName(), splitModelsDir+"/"+element.getName());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		System.out.println("    Thread "+ id +" has finished.");
	}
	
}
