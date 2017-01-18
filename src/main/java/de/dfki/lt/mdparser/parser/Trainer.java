package de.dfki.lt.mdparser.parser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import pi.ParIterator;
import pi.ParIteratorFactory;
import de.bwaldvogel.liblinear.*;
import de.dfki.lt.mdparser.algorithm.CovingtonAlgorithm;
import de.dfki.lt.mdparser.algorithm.ParsingAlgorithm;
import de.dfki.lt.mdparser.algorithm.StackAlgorithm;
import de.dfki.lt.mdparser.data.Data;
import de.dfki.lt.mdparser.data.Sentence;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.features.CovingtonFeatureModel;
import de.dfki.lt.mdparser.features.FeatureExtractor;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.FeatureVector;
import de.dfki.lt.mdparser.features.StackFeatureModel;

public class Trainer {

	private double    bias             = -1;
	private Problem   prob             = null;
	// solver, penalty C, epsilon Eps
	private Parameter param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.3);
	private int totalConfigurations;

	// GN: added 8.7.2014
	public Parameter getParam() {
		return param;
	}

	public void setParam(Parameter param) {
		this.param = param;
	}
	
	//

	static double atof(String s) {
		if (s == null || s.length() < 1) throw new IllegalArgumentException("Can't convert empty string to integer");
		double d = Double.parseDouble(s);
		if (Double.isNaN(d) || Double.isInfinite(d)) {
			throw new IllegalArgumentException("NaN or Infinity in input: " + s);
		}
		return (d);
	}

	static int atoi(String s) throws NumberFormatException {
		if (s == null || s.length() < 1) throw new IllegalArgumentException("Can't convert empty string to integer");
		// Integer.parseInt doesn't accept '+' prefixed strings
		if (s.charAt(0) == '+') s = s.substring(1);
		return Integer.parseInt(s);
	}

	public void readProblem(String filename) throws IOException, InvalidInputDataException {
		BufferedReader fp = new BufferedReader(new FileReader(filename));
		List<Integer> vy = new ArrayList<Integer>();
		List<FeatureNode[]> vx = new ArrayList<FeatureNode[]>();
		int max_index = 0;
		int lineNr = 0;

		try {
			while (true) {
				String line = fp.readLine();
				if (line == null) break;
				lineNr++;

				StringTokenizer st = new StringTokenizer(line, " \t\n\r\f:");
				String token = st.nextToken();

				try {

					vy.add(atoi(token));
				} catch (NumberFormatException e) {
					throw new InvalidInputDataException("invalid label: " + token, filename, lineNr, e);
				}

				int m = st.countTokens() / 2;
				FeatureNode[] x;
				if (bias >= 0) {
					x = new FeatureNode[m + 1];
				} else {
					x = new FeatureNode[m];
				}
				int indexBefore = 0;
				for (int j = 0; j < m; j++) {

					token = st.nextToken();
					int index;
					try {
						index = atoi(token);
					} catch (NumberFormatException e) {
						throw new InvalidInputDataException("invalid index: " + token, filename, lineNr, e);
					}

					// assert that indices are valid and sorted
					if (index < 0) throw new InvalidInputDataException("invalid index: " + index, filename, lineNr);
					if (index <= indexBefore) throw new InvalidInputDataException("indices must be sorted in ascending order", filename, lineNr);
					indexBefore = index;

					token = st.nextToken();
					try {
						double value = atof(token);
						x[j] = new FeatureNode(index, value);
					} catch (NumberFormatException e) {
						throw new InvalidInputDataException("invalid value: " + token, filename, lineNr);
					}
				}
				if (m > 0) {
					max_index = Math.max(max_index, x[m - 1].index);
				}

				vx.add(x);
			}
			prob = constructProblem(vy, vx, max_index);
		}
		finally {
			fp.close();
		}
	}
	
	// XXX GN: this is used for training
	public void createAndTrainWithSplittingFromDisk(String algorithm,
			String inputFile, String splitModelsDir, 
			String alphabetFileParser, String alphabetFileLabeler,
			String splitFile) throws IOException, InvalidInputDataException {
		boolean noLabels = false;
		System.out.println("Start training with createAndTrainWithSplittingFromDisk!");
		
		// GN: internalize CONLL data in 2-Dim sentences; max 0-12 conll columns are considered
		System.out.println("Internalize training data from: " + inputFile);
		Data d = new Data(inputFile, true);
		
		// GN: alphaParser us used for the mapping of integer to feature name
		//		it is incrementally built during training for all features that are added
		//		to the model
		Alphabet alphaParser = new Alphabet();
		// GN: the feature templates functions
		FeatureExtractor fe = new FeatureExtractor();
		Sentence[] sentences = d.getSentences();
		FeatureModel fm = null;
		ParsingAlgorithm pa = null;
		if (algorithm.equals("covington")) {
			fm = new CovingtonFeatureModel(alphaParser, fe);
			pa = new CovingtonAlgorithm();
		}
		else if (algorithm.equals("stack")) {
			fm = new StackFeatureModel(alphaParser,fe);
			pa = new StackAlgorithm();
		}
		setTotalConfigurations(0);
		File splitA = new File("splitA");
		splitA.mkdir();
		File splitO = new File("splitO");
		splitO.mkdir();
		File splitF = new File("splitF");
		splitF.mkdir();
		//print training data
		HashMap<Integer,BufferedWriter> opMap = new HashMap<Integer, BufferedWriter>();
		HashMap<Integer,String> posMap = new HashMap<Integer,String>();
		
		// GN: for each training example do:
		// NOTE: this is a sequential step
		System.out.println("Create feature vectors for data: " + sentences.length);
		System.out.println("Create files in split0 ");
		for (int n=0; n < sentences.length;n++) {
			Sentence sent = sentences[n];
			//	fm.initializeStaticFeaturesCombined(sent, true);
			
			// GN: call the parser on each training example to "re-play" the parser configurations
			//     and to compute the operations in form of a list of feature vectors for each state.
			//     this means that all feature functions are applied on the parsed sentence by applying
			//     the feature model in the training mode.
			//	   the result is then a list of parser states in form of feature vectors whose values are based
			//     one the specific training example
			List<FeatureVector> parserList = pa.processCombined(sent, fm, noLabels);
			totalConfigurations += parserList.size();
			
			// GN: for each feature vector (which represents a parser state) do
			for (int i=0; i < parserList.size(); i++) {
				// GN: for each state/feature vector store them in internal format in a file
				//		buffer whose name depends on the label-value of the feature vector
				//      store in the label-value/buffer in opMap hash.
				//		NOTE: token level, so duplicates
				FeatureVector fv = parserList.get(i);	
				String operation = fv.getLabel();
				// GN: Lookup up label-index and use it to create/extend buffer
				Integer index = alphaParser.getLabelIndexMap().get(operation);
				BufferedWriter curBw = opMap.get(index);
				if (curBw == null) {
					FileOutputStream out = new FileOutputStream(String.format("splitO/%03d.txt",index));
					OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
					curBw = new BufferedWriter(or);
					opMap.put(index,curBw);
	
				}
				curBw.append(fv.getIntegerRepresentation(alphaParser, false)+"\n");
			}
		}
		
		// GN: and finally close all the buffers
		Iterator<Integer> opIter = opMap.keySet().iterator();
		while (opIter.hasNext()) {
			Integer curOp = opIter.next();
			opMap.get(curOp).close();
		}
		
		HashMap<String, BufferedWriter> splitMap = new HashMap<String, BufferedWriter>(); 
		
		// GN: the next code basically creates the split training files
		// 		using a distributed approach based on the available processors
		//		stores and adjust the split files in filder split/
		//		and finally calls the trainer on each file ion parallel
		alphaParser.createIndexToValueArray();		
		String[] valArray = alphaParser.getIndexToValueArray();
		alphaParser.printToFile(alphabetFileParser);
		for (int v=1; v < valArray.length;v++) {
			String val = valArray[v];			
			if (val.split("=")[0].equals("pj")) {
				posMap.put(v, val);
			}
		}
		
		System.out.println("Create splitting training files!");
		
		// GN: for each label-specific feature vector integer encoded file do
		File[] trainingFiles = splitO.listFiles();
		Runtime runtime = Runtime.getRuntime();
		
		// GN: the number of available processor is determined !
		int numberOfProcessors = runtime.availableProcessors();
		int threadCount = numberOfProcessors;		
		//    int threadCount = 10;
		System.out.println("Parallel processing on " + threadCount + " processors !");
		
		List<File> filesList = new ArrayList<File>(trainingFiles.length);
		for (int n=0; n < trainingFiles.length;n++) {
			filesList.add(trainingFiles[n]);
		}
		
		// GN: distribute the split files equally to parallel iterators
		ParIterator<File> iter = ParIteratorFactory.createParIterator(filesList, threadCount);
		Thread[] threadPool = new SplitWorkerThread[threadCount];
		for (int k = 0; k < threadCount; k++) {
			threadPool[k] = new SplitWorkerThread(k, iter, posMap,splitMap);
			threadPool[k].start();
		}
		for (int k = 0; k < threadCount; k++) {
			try {
				threadPool[k].join();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}   
		
		System.out.println("Splitting files created in splitF");
		Iterator<String> splitIter = splitMap.keySet().iterator();
		while (splitIter.hasNext()) {
			String curSplitVal = splitIter.next();
			splitMap.get(curSplitVal).close();
		}
		
		// GN: First version of split files are generated in splitF/
		//		adjust them and store them in split/ 
		//merge into files of acceptable size
		trainingFiles = new File("splitF").listFiles();
		
		System.out.println("Adjust splitting files in splitF and store them in split/ ");
		HashMap<String, String> newSplitMap = new HashMap<String, String>();
		int curSize = 0;
		int splitThreshold = 3000;
		BufferedWriter curBw = null;
		String curFile = null;
		String lastFile = null;
		String curSplitVal = null;
		for (int f=0; f < trainingFiles.length;f++) {
			curSplitVal = trainingFiles[f].getName();
			FileInputStream in = new FileInputStream(trainingFiles[f]);	
			BufferedInputStream bis = new BufferedInputStream(in, 8000);
			InputStreamReader ir = new InputStreamReader(bis,"UTF8");
			BufferedReader fr = new BufferedReader(ir);
			if (curBw == null) {
				curFile = curSplitVal;
				OutputStreamWriter or = new OutputStreamWriter(new FileOutputStream("split/"+curFile),"UTF-8");
				curBw = new BufferedWriter(or);
			}
			String line;
			while ((line = fr.readLine())!= null) {
				curBw.append(line+"\n");
				curSize++;
			}
			fr.close();
			if (curSize > splitThreshold) {
				curBw.close();
				curBw = null;
				curSize = 0;
				lastFile = curFile;
			}
			newSplitMap.put(curSplitVal, curFile);
	
		}
		//if the last file is too small add to the second to last
		
		System.out.println("Eventually adjust last split/ file. ");
		if (curBw != null) {
			curBw.close();
			if (lastFile != null) {
				FileInputStream in = new FileInputStream("split/"+lastFile);
				BufferedInputStream bis = new BufferedInputStream(in, 8000);
				InputStreamReader ir = new InputStreamReader(bis,"UTF8");
				BufferedReader fr = new BufferedReader(ir);
				String line = "";
				StringBuilder sb = new StringBuilder();
				while ((line = fr.readLine())!= null) {
					sb.append(line+"\n");
				}
				in = new FileInputStream("split/"+curFile);
				bis = new BufferedInputStream(in, 8000);
				ir = new InputStreamReader(bis,"UTF8");
				fr = new BufferedReader(ir);
				while ((line = fr.readLine())!= null) {
					sb.append(line+"\n");
				}
				fr.close();
				FileOutputStream out = new FileOutputStream("split/"+lastFile);
				OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
				curBw = new BufferedWriter(or);
				curBw.append(sb.toString());
				curBw.close();
				File f = new File("split/"+curFile);
				f.delete();
	
				newSplitMap.put(curSplitVal, lastFile);
	
				Set<String> toSubstitute = new HashSet<String>();
				Iterator<String> keysIter = newSplitMap.keySet().iterator();
				while (keysIter.hasNext()) {
					String key = keysIter.next();
					String curF = newSplitMap.get(key);
					if (curF.equals(curFile)) {
						toSubstitute.add(key);
					}
				}
				if (!toSubstitute.isEmpty()) {
					keysIter = toSubstitute.iterator();
					while (keysIter.hasNext()) {
						String key = keysIter.next();
						newSplitMap.put(key,lastFile);
					}
				}
			}
			else {
				newSplitMap.put(curFile,curFile);
			}
		}
		//	System.out.println(newSplitMap);
		//printSplitMap
		FileOutputStream out = new FileOutputStream("temp/split.txt");
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		curBw = new BufferedWriter(or);
		Iterator<String> splitValIter = newSplitMap.keySet().iterator();
		alphaParser.printToFile(alphabetFileParser);
		while (splitValIter.hasNext()) {
			curSplitVal = splitValIter.next();
			String newFile = newSplitMap.get(curSplitVal);
			Integer index = Integer.valueOf(curSplitVal.split("\\.")[0]);
			//		String featureString = posMap.get(index);
			String featureString = alphaParser.getIndexToValueArray()[index];
			//	curBw.append(featureString+" split/"+newFile+" "+newFile+"\n");
			curBw.append(featureString+" "+"split/"+newFile+" "+curSplitVal+"\n");
		}
		curBw.close();
		File[] files = new File("split").listFiles();	
		filesList = new ArrayList<File>(files.length);
		for (int n=0; n < files.length;n++) {
			filesList.add(files[n]);
		}
		
		System.out.println("Compute the weights and the final model files in splitModels/ and alphabet files in splitA/ !");
		// XXX GN: Perform parallel training
		// For each file in split create call the trainer on that file -> means a new trainer object is created for each thread
		//			via de.dfki.lt.mdparser.parser.CompactiseWorkerThread.CompactiseWorkerThread(int, ParIterator<File>, Alphabet, String)
		iter = ParIteratorFactory.createParIterator(filesList, threadCount);
		threadPool = new CompactiseWorkerThread[threadCount];
		for (int m = 0; m < threadCount; m++) {
			threadPool[m] = new CompactiseWorkerThread(m, iter,alphaParser, splitModelsDir);
			threadPool[m].start();
		}
		for (int m = 0; m < threadCount; m++) {
			try {
				threadPool[m].join();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Make single alphabet file from splitA files " + alphabetFileParser);
		recreateOneAlphabetAndAdjustModels(alphabetFileParser, "splitA",splitModelsDir);
	
	
	}
	
	private void recreateOneAlphabetAndAdjustModels(String alphabetFile, String splitAlphaDir, String splitModelsDir) throws IOException{
		Alphabet alpha = unionAlphabets(alphabetFile,splitAlphaDir);
		restoreModels(splitModelsDir,alpha, splitAlphaDir);
		alpha.printToFile(alphabetFile);
	}

	private void restoreModels(String splitModelsDir, Alphabet alpha, String splitA) throws IOException {
		File[] models = new File(splitModelsDir).listFiles();
		alpha.createIndexToValueArray();
		for (int i=0; i < models.length; i++) {
			System.out.println("Model file: " + models[i]);
			Model model = Linear.loadModel(models[i]);
			double[] wArray = model.getFeatureWeights();
			String alphaName = splitA+"/"+models[i].getName();
			Alphabet a = new Alphabet(new FileInputStream(new File(alphaName)));
			int numberOfClasses = model.getNrClass();
			FileOutputStream out = new FileOutputStream(models[i]);
			OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
			BufferedWriter bw = new BufferedWriter(or);
			bw.append("solver_type MCSVM_CS\n");
			bw.append("nr_class "+model.getNrClass()+"\nlabel ");

			for (int k=0; k <model.getLabels().length;k++) {
				bw.append(model.getLabels()[k]+" ");
			}
			bw.append("\n");
			String[] features = alpha.getIndexToValueArray();
			//	System.out.println(features);
			boolean notFound = true;
			Set<String> thisAlphabetFeatures = a.getValueToIndexMap().keySet();
			int lastIndex = -1;
			for (int k=features.length-1; k > 1 && notFound;k--) {
				if (thisAlphabetFeatures.contains(features[k])) {
					lastIndex = k;
					notFound = false;
				}
			}
			bw.append("nr_feature "+(lastIndex-1)+"\n");
			bw.append("bias "+model.getBias()+"\nw\n");
			HashMap<String,Integer> valToIndexMap = a.getValueToIndexMap();
			for (int k=1; k < lastIndex;k++) {
				String feature = features[k];
				Integer oldIndex = valToIndexMap.get(feature);
				if (oldIndex == null) {
					for (int m=0; m < numberOfClasses;m++) {
						bw.append("0 ");
					}					
				}
				else {
					for (int l=0; l < numberOfClasses; l++) {
						double curWeight = wArray[(oldIndex-1)*numberOfClasses+l];
						bw.append(String.valueOf(curWeight)+" ");
					}
				}
				bw.append("\n");		

			}
			bw.close();
		}
	}

	private Alphabet unionAlphabets(String alphabetFile, String splitAlphaDir) throws IOException {
		Alphabet alpha = new Alphabet();
		File[] alphabets = new File(splitAlphaDir).listFiles();
		HashMap<String,Integer> map = alpha.getValueToIndexMap();
		for (int i = 0; i < alphabets.length; i++) {
			Alphabet curAlpha = new Alphabet(new FileInputStream(alphabets[i]));		
			if (alpha.getLabelIndexMap().keySet().isEmpty()) {
				alpha.setLabelIndexMap(curAlpha.getLabelIndexMap());
				/*	String[] labels = curAlpha.getIndexLabelArray();
				for (int k=0; k < labels.length;k++) {
					alpha.addLabel(labels[k]);
				}*/
				alpha.setMaxLabelIndex(curAlpha.getLabelIndexMap().size()+1);
			}		
			String[] features = curAlpha.getIndexToValueArray();
			//	System.out.println(curAlpha.getValueToIndexMap());
			for (int k=1; k < features.length;k++) {
				Integer index = map.get(features[k]);
				//	System.out,.println(features)
				if (index == null) {
					alpha.addFeature(features[k]);
					//			System.out.println(features[k]);
				}
			}
		}

		return alpha;

	}

	void saveAlphabet(Alphabet alphaParser, Model model,int[][] compactArray, File file) throws IOException {
		int[] newToOld = compactArray[0];
		FileOutputStream out = new FileOutputStream(file);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter bw = new BufferedWriter(or);
		String[] indexLabelArray = alphaParser.getIndexLabelArray();
		for (int i=1; i < alphaParser.getMaxLabelIndex();i++) {
			bw.append(i+" "+indexLabelArray[i]+"\n");
		}
		// GN: seems to cause problem in training with algorithm=stack
		bw.append("\n");
		String[] indexToValue = alphaParser.getIndexToValueArray();
		boolean notFinished = true;
		for (int i=1; notFinished && i < newToOld.length;i++) {
			int newIndex = i;
			int oldIndex = newToOld[i];
			if (oldIndex == 0) {
				notFinished = false;
			}
			else {
				String stringValue = indexToValue[oldIndex];
				bw.append(newIndex+" "+stringValue+"\n");
			}
		}
		bw.close();
	}

	int[][] compactiseTrainingDataFile(File curentTrainingFile,int absoluteMax, File splitC) throws IOException {
		int[][] compactArray = new int[2][];
		int[] oldToNew = new int[absoluteMax];
		int[] newToOld = new int[absoluteMax];
		compactArray[0] = newToOld;
		compactArray[1] = oldToNew;
		FileInputStream in = new FileInputStream(curentTrainingFile);
		BufferedInputStream bis = new BufferedInputStream(in, 8000);
		InputStreamReader ir = new InputStreamReader(bis,"UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line;
		Integer maxIndex = 1;
		File outputFile = new File(splitC.getName()+"/"+curentTrainingFile.getName());
		FileOutputStream out = new FileOutputStream(outputFile);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter curBw = new BufferedWriter(or);
		Set<Integer> encountered = new HashSet<Integer>(absoluteMax);
		while ((line = fr.readLine())!= null) {
			String[] lineArray = line.split(" ");
			curBw.append(lineArray[0]);
			List<Integer> featureList = new ArrayList<Integer>();
			for (int i=1; i < lineArray.length;i++ ) {
				Integer curFeature = Integer.valueOf(lineArray[i].split(":")[0]);
				Integer newValue = oldToNew[curFeature];
				if (!encountered.contains(curFeature)) {
					newValue = maxIndex;
					oldToNew[curFeature] = newValue;
					newToOld[newValue] = curFeature;
					encountered.add(curFeature);
					maxIndex++;
				}
				if (!featureList.contains(newValue)) {
					featureList.add(newValue);
				}
			}
			Collections.sort(featureList);
			for (int i=0; i < featureList.size();i++) {
				curBw.append(" "+featureList.get(i)+":1");
			}
			curBw.append("\n");
		}
		fr.close();
		curBw.close();
		return compactArray;
	}

	private  Problem constructProblem(List<Integer> vy, List<FeatureNode[]> vx, int max_index) {
		Problem prob = new Problem();
		prob.bias = bias;
		prob.l = vy.size();
		prob.n = max_index;
		if (bias >= 0) {
			prob.n++;
		}
		prob.x = new FeatureNode[prob.l][];
		for (int i = 0; i < prob.l; i++) {
			prob.x[i] = vx.get(i);

			if (bias >= 0) {
				assert prob.x[i][prob.x[i].length - 1] == null;
				prob.x[i][prob.x[i].length - 1] = new FeatureNode(max_index + 1, bias);
			} else {
				assert prob.x[i][prob.x[i].length - 1] != null;
			}
		}

		//GN, May, 2016
		// prob.y = new int[prob.l];
		prob.y = new double[prob.l];
		for (int i = 0; i < prob.l; i++)
			prob.y[i] = vy.get(i);

		return prob;
	}

	public void setTotalConfigurations(int totalConfigurations) {
		this.totalConfigurations = totalConfigurations;
	}

	public int getTotalConfigurations() {
		return totalConfigurations;
	}

	public Problem getProblem() {
		return prob;
	}

}
