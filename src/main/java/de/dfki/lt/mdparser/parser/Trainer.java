package de.dfki.lt.mdparser.parser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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
import de.dfki.lt.mdparser.features.Feature;
import de.dfki.lt.mdparser.features.FeatureExtractor;
import de.dfki.lt.mdparser.features.FeatureModel;
import de.dfki.lt.mdparser.features.FeatureVector;
import de.dfki.lt.mdparser.features.StackFeatureModel;
import de.dfki.lt.mdparser.model.ModelEditor;

public class Trainer {

	private double    bias             = -1;
	private Problem   prob             = null;
	private Parameter param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.3);
	//private Parameter param = new Parameter(SolverType.L1R_LR, 1, 0.01);	
	private int numberOfFeatures;
	private int maxLabelParser;
	private int maxLabelLabeler;
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

	public void myReadProblem(Alphabet alpha, boolean labels,List<FeatureVector> fvList) throws IOException, InvalidInputDataException {

		int max_index = Integer.MIN_VALUE;
		List<Integer> yList = new ArrayList<Integer>();	
		List<FeatureNode[]> xList = new ArrayList<FeatureNode[]>();
		for (int i=0; i < fvList.size();i++) {
			FeatureVector fv = fvList.get(i);
			FeatureNode[] fnArray = fv.getLiblinearRepresentation(true, labels, alpha);
			Integer y = Integer.valueOf(alpha.getLabelIndexMap().get(fv.getLabel()));
			Integer x = 0;
			yList.add(y);
			xList.add(fnArray);
			max_index = Math.max(max_index, fnArray[fnArray.length-1].index);
		}
		this.prob = constructProblem(yList,xList,max_index);
	}

	public void myReadProblem(int[][] compactArray, Alphabet alpha, boolean labels,List<FeatureVector> fvList) throws IOException, InvalidInputDataException {

		int max_index = Integer.MIN_VALUE;
		List<Integer> yList = new ArrayList<Integer>();	
		List<FeatureNode[]> xList = new ArrayList<FeatureNode[]>();
		for (int i=0; i < fvList.size();i++) {
			FeatureVector fv = fvList.get(i);
			List<Feature> fList = fv.getfList();

			FeatureNode[] fnArray = new FeatureNode[fList.size()];
			List<Integer> indexes = new ArrayList<Integer>();
			for (int k=0; k < fList.size();k++) {
				indexes.add(fList.get(k).getIndexParser());
			}
			Collections.sort(indexes);
			for (int k=0; k < indexes.size();k++) {
				FeatureNode fn = new FeatureNode(indexes.get(k),1);
				fnArray[k] = fn;
			}
			int[] oldToNewL = compactArray[3];
			Integer oldLabelIndex = Integer.valueOf(alpha.getLabelIndexMap().get(fv.getLabel()));
			//	Integer y = oldToNewL[oldLabelIndex];
			Integer y = oldLabelIndex;
			yList.add(y);
			xList.add(fnArray);
			max_index = Math.max(max_index, fnArray[fnArray.length-1].index);
		}
		this.prob = constructProblem(yList,xList,max_index);
	}

	public void createAndTrainWithSplittingFromMemory(String algorithm,
			String inputFile, String splitModelsDir, 
			String alphabetFileParser, String alphabetFileLabeler,
			String splitFile) throws IOException, InvalidInputDataException {
		boolean noLabels = false;
		HashMap<String,List<FeatureVector>> splitMap = new HashMap<String,List<FeatureVector>>();
		long st = System.currentTimeMillis();

		System.out.println("Start training with createAndTrainWithSplittingFromMemory!");
		
		// GN: internalize CONLL data in 2-Dim sentences
		System.out.println("Internalize training data from: " + inputFile);
		
		Data d = new Data(inputFile, true);
		Alphabet alphaParser = new Alphabet();
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
		int totalConfigurations = 0;
		File splitA = new File("splitA");
		splitA.mkdir();
		System.out.println("Create feature vectors for data: " + sentences.length);
		// For each training example x_i = n-th sentence do:
		for (int n=0; n < sentences.length;n++) {
			Sentence sent = sentences[n];
			// GN: initialize static features (i.e., concrete values for feature+value instance)
			//	   check static features for current word j and left words i
			//	NOTE: the static and dynamic feature-values are added to the alphabet class as a side-effect
			fm.initializeStaticFeaturesCombined(sent, true);
			
			// GN: call parsing control strategy
			// GN: call the parser on each training example to "re-play" the parser configurations
			//     and to compute the operations in form of a list of feature vectors for each state.
			//     this means that all feature functions are applied on the parsed sentence by applying
			//     the feature model in the training mode.
			//	   the result is then a list of parser states in form of feature vectors whose values are based
			//     one the specific training example
			List<FeatureVector> parserList = pa.processCombined(sent, fm, noLabels);
			totalConfigurations += parserList.size();
			
			// GN: for each feature vector (which represents ONE parser configuration) do
			//     group them into 
			for (int i=0; i < parserList.size(); i++) {
				
				// the feature vector of the ith configuration of the ith token of the "parsed" sentence:
				// which is a list starting with a label and followed by a list of feature values
				
				FeatureVector fv = parserList.get(i);
				// System.out.println("Sentence " + n + " ... configuration fv-parse " + fv.toString());
				// Sentence 1778 ... fv-parse j#O pj=NNS pjp1=IN pjp2=DT pjp3=NN wfj=results cpj=NNS wfjp1=for m0=IN_DT_NN pjp4=TO pi=null pip1=" wfi=null cpi=null m1=null_" m2=null_null pip2=IN dist=6 wfhi=null phi=null depi=none depldj=null depldi=none deprdi=O m3=null_NNS m4=null_O_none m5=none_null m6=null_"_IN m7=results_null_DT m8=6_NNS_for

				
				Feature splitFeature = fv.getfList().get(0);	// reference to the first feature-value which is the POS of current token j
				
				// Basically the POS-feature value for the jth element, e.g., pj=ART
				// It is used for creating splitVal different hashes, which serve as parallel split training files
				String splitVal = splitFeature.getFeatureString();
				
				// NOTE: difference to function createAndTrainWithSplittingFromDisk():
				//       there the label is used, not the pos-class

				// System.out.println("Sentence " + n + " ... Split value " + splitVal);

				List<FeatureVector> listForThisSplitVal = splitMap.get(splitVal);
				if (listForThisSplitVal == null) {
					listForThisSplitVal = new ArrayList<FeatureVector>();
					splitMap.put(splitVal, listForThisSplitVal);
				}
				listForThisSplitVal.add(fv);
			}
		}
		System.out.println("Total configurations: "+totalConfigurations);
		//	NOTE: the static and dynamic feature-values are added to the alphabet class as a side-effect
		//  via the selected model (CovingtonFeatureModel()) and are now saved in a file.
		//  HIERIX
		alphaParser.printToFile(alphabetFileParser);
		setMaxLabelParser(alphaParser.getMaxLabelIndex());	
		
		//merging split maps parser
		this.numberOfFeatures = alphaParser.getMaxIndex();
		HashMap<String,String> newSplitMap = new HashMap<String,String>();
		Iterator<String> keyIter = splitMap.keySet().iterator();
		int curCount = 0;
		int t = 10000;
		int n=1;
		List<FeatureVector> curList = new ArrayList<FeatureVector>();
		HashMap<String,List<FeatureVector>> mergedMap = new HashMap<String, List<FeatureVector>>();
		//find all that are > t 
		Set<String> toRemove = new HashSet<String>();
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			List<FeatureVector> listForThisSplitVal = splitMap.get(key);
			int count = splitMap.get(key).size();	
			if (count > t) {
				newSplitMap.put(key,String.valueOf(n));
				mergedMap.put(String.valueOf(n),listForThisSplitVal);
				n++;
				toRemove.add(key);
			}
		}
		keyIter = splitMap.keySet().iterator();
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			if (!toRemove.contains(key)) {
				List<FeatureVector> listForThisSplitVal = splitMap.get(key);
				curList.addAll(listForThisSplitVal);
				newSplitMap.put(key,String.valueOf(n));
				int count = splitMap.get(key).size();		
				curCount += count;
				if (curCount > t) {
					//			mergedMap.put(String.valueOf(n),curList);
					//			n++;
					//			curCount = 0;			
					//		curList = new ArrayList<FeatureVector>();	
				}	
			}
		}
		//	if (!curList.isEmpty() && curList.size() > (t/2)) {
		mergedMap.put(String.valueOf(n),curList);
		//	}
		/*	else {
			List<FeatureVector> previousList = mergedMap.get(String.valueOf(n-1));
			previousList.addAll(curList);
			mergedMap.put(String.valueOf(n), previousList);
		}*/
		guaranteeOrder(mergedMap,alphaParser);
		long end = System.currentTimeMillis();
		System.out.println("Training data creating time: "+(end-st)/1000+" seconds.");

		//smaller indexes for each model
		HashMap<String,int[][]> compactMap = compactiseTrainingDataFiles(alphaParser,mergedMap);

		//train parser
		FileOutputStream out = new FileOutputStream(splitFile);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter bw = new BufferedWriter(or);
		Iterator<String> iter = splitMap.keySet().iterator();
		boolean[] b = new boolean[100];
		System.out.println("Do training with liblinear ... ");
		st = System.currentTimeMillis();
		while (iter.hasNext()) {
			String curFeature = iter.next();
			//	System.out.println(curFeature);
			String nValForCurFeature = newSplitMap.get(curFeature);
			n = Integer.valueOf(nValForCurFeature);
			bw.append(curFeature+" "+"split/"+nValForCurFeature+".txt "+nValForCurFeature+".txt\n");		
			if (!b[n]) {
				curList = mergedMap.get(nValForCurFeature);
				//		System.out.println(nValForCurFeature+" "+mergedMap.get(nValForCurFeature).size());
				myReadProblem(alphaParser, false, curList);
				//		myReadProblem(compactMap.get(nValForCurFeature),alphaParser, false, curList);
				//		System.out.println(curList.get(0));System.out.println(curList.get(1));System.out.println(curList.get(2));System.out.println(curList.get(3));
				Linear.disableDebugOutput();
				// DO THE TRAINING
				Model model = Linear.train(this.prob, this.param);
				//		System.out.println("mmodel "+nValForCurFeature+".txt: " +Double.valueOf(MemoryUtil.deepMemoryUsageOf(model))/1024/1024+" MB");
				//use weights but with old indexes
				//			saveModel(model,compactMap.get(nValForCurFeature), new File(splitModelsDir+"/"+nValForCurFeature+".txt"));
				saveAlphabet(alphaParser,model,compactMap.get(nValForCurFeature), new File("splitA/"+nValForCurFeature+".txt"));
				//	old		
				model.save(new File(splitModelsDir+"/"+nValForCurFeature+".txt"));
				//	edit immediately	
				ModelEditor me = new ModelEditor(new File(splitModelsDir+"/"+nValForCurFeature+".txt"), "splitA/"+nValForCurFeature+".txt",true);
				me.editAlphabetAndModel("splitA/"+nValForCurFeature+".txt", splitModelsDir+"/"+nValForCurFeature+".txt");
				b[n] = true;
			}
		}
		bw.close();
		// recreate alphabet and models 
		recreateOneAlphabetAndAdjustModels(alphabetFileParser, "splitA",splitModelsDir);
		long end2 = System.currentTimeMillis();
		System.out.println("... done : "+(end2 - st)/1000+" seconds.");

	}

	// XXX GN: this is used for training
	public void createAndTrainWithSplittingFromDisk(String algorithm,
			String inputFile, String splitModelsDir, 
			String alphabetFileParser, String alphabetFileLabeler,
			String splitFile) throws IOException, InvalidInputDataException {
		boolean noLabels = false;
		long st = System.currentTimeMillis();	
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
		long t1 = System.currentTimeMillis();
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
		
		long t2 = System.currentTimeMillis();
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
		long t3 = System.currentTimeMillis();
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
				FileOutputStream out = new FileOutputStream("split/"+curFile);
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
		long t4 = System.currentTimeMillis();
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
		long t5 = System.currentTimeMillis();
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
		long t6 = System.currentTimeMillis();
		recreateOneAlphabetAndAdjustModels(alphabetFileParser, "splitA",splitModelsDir);
		long t7 = System.currentTimeMillis();
		//	System.out.println((t2-t1)+" "+(t3-t2)+" "+(t4-t3)+" "+(t5-t4)+" "+(t6-t5)+" "+(t7-t6));
	
	
	}

	// GN: NOT USED
	public void createAndTrainWithSplittingFromDisk(String algorithm,
			Data d, String splitModelsDir, 
			String alphabetFileParser, String alphabetFileLabeler,
			String splitFile) throws IOException, InvalidInputDataException {
		boolean noLabels = false;
		long st = System.currentTimeMillis();	
		Alphabet alphaParser = new Alphabet();
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
		int totalConfigurations = 0;
		File splitA = new File("splitA");
		splitA.mkdir();
		File splitO = new File("splitO");
		splitO.mkdir();
		File splitF = new File("splitF");
		splitF.mkdir();
		//print training data
		HashMap<Integer,BufferedWriter> opMap = new HashMap<Integer, BufferedWriter>();
		for (int n=0; n < sentences.length;n++) {
			Sentence sent = sentences[n];
			fm.initializeStaticFeaturesCombined(sent, true);
			// Call parser 
			List<FeatureVector> parserList = pa.processCombined(sent, fm, noLabels);
			totalConfigurations += parserList.size();
			for (int i=0; i < parserList.size(); i++) {
				FeatureVector fv = parserList.get(i);	
				String operation = fv.getLabel();
				Integer index = alphaParser.getLabelIndexMap().get(operation);
				BufferedWriter curBw = opMap.get(index);
				if (curBw == null) {
					FileOutputStream out = new FileOutputStream(String.format("splitO/%03d.txt",index));
					OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
					curBw = new BufferedWriter(or);
					opMap.put(index,curBw);

				}
				curBw.append(fv.getIntegerRepresentation(alphaParser, false)+"\n");
				//		curBw.append(fv+"\n");
			}
		}
		Iterator<Integer> opIter = opMap.keySet().iterator();
		while (opIter.hasNext()) {
			Integer curOp = opIter.next();
			opMap.get(curOp).close();
		}
		HashMap<String, BufferedWriter> splitMap = new HashMap<String, BufferedWriter>(); 
		//split
		alphaParser.createIndexToValueArray();
		HashMap<Integer,String> posMap = new HashMap<Integer,String>();
		String[] valArray = alphaParser.getIndexToValueArray();
		for (int v=1; v < valArray.length;v++) {
			String val = valArray[v];
			if (val.split("=")[0].equals("pj")) {
				posMap.put(v, val);
			}
		}
		File[] trainingFiles = splitO.listFiles();
		List<Integer> indexes = new ArrayList<Integer>(trainingFiles.length);
		for (int i=0; i < trainingFiles.length;i++) {
			indexes.add(Integer.valueOf(trainingFiles[i].getName().split("\\.")[0]));
		}
		Collections.sort(indexes);
		for (int f=0; f < indexes.size();f++) {
			FileInputStream in = new FileInputStream(String.format("splitO/%03d.txt",indexes.get(f)));
			BufferedInputStream bis = new BufferedInputStream(in, 8000);
			InputStreamReader ir = new InputStreamReader(bis,"UTF8");
			BufferedReader fr = new BufferedReader(ir);
			String line;
			while ((line = fr.readLine())!= null) {
				String[] lineArray = line.split(" ");
				String splitVal = "";
				Integer splitIndex = 0;
				int fIndex = 1;
				while (splitVal.equals("")) {				
					int index = Integer.valueOf(lineArray[fIndex].split(":")[0]);
					String pos = posMap.get(index);
					if (pos != null) {
						splitVal = pos;
						splitIndex = index;
					}
					else {
						fIndex++;
					}
				}
				BufferedWriter curBw = splitMap.get(splitVal);
				if (curBw == null) {
					FileOutputStream out = new FileOutputStream("splitF/"+splitIndex+".txt");
					OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
					curBw = new BufferedWriter(or);
					splitMap.put(splitVal,curBw);
				}
				curBw.append(line+"\n");
			}
			fr.close();
		}
		Iterator<String> splitIter = splitMap.keySet().iterator();
		while (splitIter.hasNext()) {
			String curSplitVal = splitIter.next();
			splitMap.get(curSplitVal).close();
		}
		//merge into files of acceptable size
		trainingFiles = new File("splitF").listFiles();
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
				FileOutputStream out = new FileOutputStream("split/"+curFile);
				OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
				curBw = new BufferedWriter(or);
			}
			String line;
			while ((line = fr.readLine())!= null) {
				curBw.append(line+"\n");
				curSize++;
			}
			if (curSize > splitThreshold) {
				curBw.close();
				curBw = null;
				curSize = 0;
				lastFile = curFile;
			}
			newSplitMap.put(curSplitVal, curFile);
			fr.close();
		}	
		//if the last file is too small add to the second to last
		if (curBw != null) {
			curBw.close();
			FileInputStream in = new FileInputStream("split/"+lastFile);
			BufferedInputStream bis = new BufferedInputStream(in, 8000);
			InputStreamReader ir = new InputStreamReader(bis,"UTF8");
			BufferedReader fr = new BufferedReader(ir);
			String line = "";
			StringBuilder sb = new StringBuilder();
			while ((line = fr.readLine())!= null) {
				sb.append(line+"\n");
			}
			fr.close();
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
		}
		System.out.println(newSplitMap);
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
			String featureString = alphaParser.getIndexToValueArray()[index];
			//	curBw.append(featureString+" split/"+newFile+" "+newFile+"\n");	
			curBw.append(featureString+" "+"split/"+newFile+" "+curSplitVal+"\n");
		}
		curBw.close();
		File[] files = new File("split").listFiles();
		/*	for (int i=0; i < files.length;i++) {
			System.out.print(files[i]+" ");
		}
		System.out.println();*/
		for (int i=0; i < files.length;i++) {
			File curentTrainingFile = files[i];
			int[][] compactArray = compactiseTrainingDataFile(curentTrainingFile,alphaParser.getMaxIndex(),new File("splitC"));
			readProblem("splitC/"+curentTrainingFile.getName());
			Linear.disableDebugOutput();
			Model m = Linear.train(prob, param);
			m.save(new File(splitModelsDir+"/"+curentTrainingFile.getName()));
			saveAlphabet(alphaParser, m, compactArray, new File("splitA/"+curentTrainingFile.getName()));		
			ModelEditor me = new ModelEditor(new File(splitModelsDir+"/"+curentTrainingFile.getName()), "splitA/"+curentTrainingFile.getName(),true);
			me.editAlphabetAndModel("splitA/"+curentTrainingFile.getName(), splitModelsDir+"/"+curentTrainingFile.getName());

		}
		recreateOneAlphabetAndAdjustModels(alphabetFileParser, "splitA",splitModelsDir);



	}

	// ONly used on UseFulltest something
	public void createAndTrainWithSplittingFromDisk(String algorithm,
			Data d, String splitModelsDir, 
			String alphabetFileParser, String alphabetFileLabeler,
			String splitFile, File splitA, File splitO, File splitF, File split, File splitC) throws IOException, InvalidInputDataException {
		boolean noLabels = false;
		long st = System.currentTimeMillis();	
		Alphabet alphaParser = new Alphabet();
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
		int totalConfigurations = 0;
		/*	File splitA = new File("splitA");
		splitA.mkdir();
		File splitO = new File("splitO");
		splitO.mkdir();
		File splitF = new File("splitF");
		splitF.mkdir();*/
		//print training data
		HashMap<Integer,BufferedWriter> opMap = new HashMap<Integer, BufferedWriter>();
		for (int n=0; n < sentences.length;n++) {
			Sentence sent = sentences[n];
			fm.initializeStaticFeaturesCombined(sent, true);
			List<FeatureVector> parserList = pa.processCombined(sent, fm, noLabels);
			totalConfigurations += parserList.size();
			for (int i=0; i < parserList.size(); i++) {
				FeatureVector fv = parserList.get(i);	
				String operation = fv.getLabel();
				Integer index = alphaParser.getLabelIndexMap().get(operation);
				BufferedWriter curBw = opMap.get(index);
				if (curBw == null) {
					FileOutputStream out = new FileOutputStream(String.format(splitO.getName()+"/%03d.txt",index));
					OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
					curBw = new BufferedWriter(or);
					opMap.put(index,curBw);

				}
				curBw.append(fv.getIntegerRepresentation(alphaParser, false)+"\n");
				//		curBw.append(fv+"\n");
			}
		}
		Iterator<Integer> opIter = opMap.keySet().iterator();
		while (opIter.hasNext()) {
			Integer curOp = opIter.next();
			opMap.get(curOp).close();
		}
		HashMap<String, BufferedWriter> splitMap = new HashMap<String, BufferedWriter>(); 
		//split
		alphaParser.createIndexToValueArray();
		HashMap<Integer,String> posMap = new HashMap<Integer,String>();
		String[] valArray = alphaParser.getIndexToValueArray();
		for (int v=1; v < valArray.length;v++) {
			String val = valArray[v];
			if (val.split("=")[0].equals("pj")) {
				posMap.put(v, val);
			}
		}
		File[] trainingFiles = splitO.listFiles();
		List<Integer> indexes = new ArrayList<Integer>(trainingFiles.length);
		for (int i=0; i < trainingFiles.length;i++) {
			indexes.add(Integer.valueOf(trainingFiles[i].getName().split("\\.")[0]));
		}
		Collections.sort(indexes);
		for (int f=0; f < indexes.size();f++) {
			FileInputStream in = new FileInputStream(String.format(splitO.getName()+"/%03d.txt",indexes.get(f)));
			BufferedInputStream bis = new BufferedInputStream(in, 8000);
			InputStreamReader ir = new InputStreamReader(bis,"UTF8");
			BufferedReader fr = new BufferedReader(ir);
			String line;
			while ((line = fr.readLine())!= null) {
				String[] lineArray = line.split(" ");
				String splitVal = "";
				Integer splitIndex = 0;
				int fIndex = 1;
				while (splitVal.equals("")) {		
					//		System.out.println(String.format(splitO.getName()+"/%03d.txt",indexes.get(f))+" "+line);
					/*		if (fIndex == lineArray.length) {
						System.out.println(String.format(splitO.getName()+"/%03d.txt",indexes.get(f))+" "+line);
					}*/
					int index = Integer.valueOf(lineArray[fIndex].split(":")[0]);
					String pos = posMap.get(index);
					if (pos != null) {
						splitVal = pos;
						splitIndex = index;
					}
					else {
						fIndex++;
					}
				}
				BufferedWriter curBw = splitMap.get(splitVal);
				if (curBw == null) {
					FileOutputStream out = new FileOutputStream(splitF.getName()+"/"+splitIndex+".txt");
					OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
					curBw = new BufferedWriter(or);
					splitMap.put(splitVal,curBw);
				}
				curBw.append(line+"\n");
			}
			fr.close();
		}
		Iterator<String> splitIter = splitMap.keySet().iterator();
		while (splitIter.hasNext()) {
			String curSplitVal = splitIter.next();
			splitMap.get(curSplitVal).close();
		}
		//merge into files of acceptable size
		trainingFiles = splitF.listFiles();
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
				FileOutputStream out = new FileOutputStream(split.getName()+"/"+curFile);
				OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
				curBw = new BufferedWriter(or);
			}
			String line;
			while ((line = fr.readLine())!= null) {
				curBw.append(line+"\n");
				curSize++;
			}
			if (curSize > splitThreshold) {
				curBw.close();
				curBw = null;
				curSize = 0;
				lastFile = curFile;
			}
			newSplitMap.put(curSplitVal, curFile);
			fr.close();
		}	
		//if the last file is too small add to the second to last
		if (curBw != null) {
			curBw.close();
			FileInputStream in = new FileInputStream(split.getName()+"/"+lastFile);
			BufferedInputStream bis = new BufferedInputStream(in, 8000);
			InputStreamReader ir = new InputStreamReader(bis,"UTF8");
			BufferedReader fr = new BufferedReader(ir);
			String line = "";
			StringBuilder sb = new StringBuilder();
			while ((line = fr.readLine())!= null) {
				sb.append(line+"\n");
			}
			fr.close();
			in = new FileInputStream(split.getName()+"/"+curFile);
			bis = new BufferedInputStream(in, 8000);
			ir = new InputStreamReader(bis,"UTF8");
			fr = new BufferedReader(ir);
			while ((line = fr.readLine())!= null) {
				sb.append(line+"\n");
			}
			fr.close();
			FileOutputStream out = new FileOutputStream(split.getName()+"/"+lastFile);
			OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
			curBw = new BufferedWriter(or);
			curBw.append(sb.toString());
			curBw.close();
			File f = new File(split+"/"+curFile);
			f.delete();
			newSplitMap.put(curSplitVal, lastFile);
		}
		//printSplitMap
		FileOutputStream out = new FileOutputStream(splitFile);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		curBw = new BufferedWriter(or);
		Iterator<String> splitValIter = newSplitMap.keySet().iterator();
		alphaParser.printToFile(alphabetFileParser);
		while (splitValIter.hasNext()) {
			curSplitVal = splitValIter.next();
			String newFile = newSplitMap.get(curSplitVal);
			Integer index = Integer.valueOf(curSplitVal.split("\\.")[0]);
			String featureString = alphaParser.getIndexToValueArray()[index];
			//	curBw.append(featureString+" split/"+newFile+" "+newFile+"\n");	
			curBw.append(featureString+" "+split.getName()+"/"+newFile+" "+curSplitVal+"\n");
		}
		curBw.close();
		File[] files = split.listFiles();
		/*	for (int i=0; i < files.length;i++) {
			System.out.print(files[i]+" ");
		}
		System.out.println();*/
		for (int i=0; i < files.length;i++) {
			File curentTrainingFile = files[i];
			int[][] compactArray = compactiseTrainingDataFile(curentTrainingFile,alphaParser.getMaxIndex(), splitC);
			readProblem(splitC.getName()+"/"+curentTrainingFile.getName());
			Linear.disableDebugOutput();
			Model m = Linear.train(prob, param);
			m.save(new File(splitModelsDir+"/"+curentTrainingFile.getName()));
			saveAlphabet(alphaParser, m, compactArray, new File(splitA.getName()+"/"+curentTrainingFile.getName()));		
			ModelEditor me = new ModelEditor(new File(splitModelsDir+"/"+curentTrainingFile.getName()), splitA.getName()+"/"+curentTrainingFile.getName(),true);
			me.editAlphabetAndModel(splitA.getName()+"/"+curentTrainingFile.getName(), splitModelsDir+"/"+curentTrainingFile.getName());

		}
		recreateOneAlphabetAndAdjustModels(alphabetFileParser, splitA.getName(),splitModelsDir);



	}

	// GN: not used
	public void createAndTrainWithSplittingFromDiskInt(String algorithm,
			String inputFile, String splitModelsDir, 
			String alphabetFileParser, String alphabetFileLabeler,
			String splitFile) throws IOException, InvalidInputDataException, NoSuchAlgorithmException {
		boolean noLabels = false;
		long st = System.currentTimeMillis();	
		Data d = new Data(inputFile, true);
		Alphabet alphaParser = new Alphabet();
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
		HashMap<Integer,String> overlapMap = new HashMap<Integer,String>();
		//print training data
		HashMap<Integer,BufferedWriter> opMap = new HashMap<Integer, BufferedWriter>();
		long t1 = System.currentTimeMillis();
		HashMap<Integer,String> posMap = new HashMap<Integer,String>();
		for (int n=0; n < sentences.length;n++) {
			Sentence sent = sentences[n];
			//	fm.initializeStaticFeaturesCombined(sent, true);
			List<FeatureVector> parserList = pa.processCombined(sent, fm, noLabels);
			totalConfigurations += parserList.size();
			for (int i=0; i < parserList.size(); i++) {
				FeatureVector fv = parserList.get(i);	
				String operation = fv.getLabel();
				Integer index = alphaParser.getLabelIndexMap().get(operation);
				BufferedWriter curBw = opMap.get(index);
				if (curBw == null) {
					FileOutputStream out = new FileOutputStream(String.format("splitO/%03d.txt",index));
					OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
					curBw = new BufferedWriter(or);
					opMap.put(index,curBw);

				}
				curBw.append(fv.getIntegerRepresentationInt(overlapMap,posMap,alphaParser, false)+"\n");
			}
		}

		Iterator<Integer> opIter = opMap.keySet().iterator();
		while (opIter.hasNext()) {
			Integer curOp = opIter.next();
			opMap.get(curOp).close();
		}
		long t2 = System.currentTimeMillis();
		HashMap<String, BufferedWriter> splitMap = new HashMap<String, BufferedWriter>(); 
		//split
		alphaParser.createIndexToValueArray();		
		String[] valArray = alphaParser.getIndexToValueArray();
		alphaParser.printToFile(alphabetFileParser);
		File[] trainingFiles = splitO.listFiles();
		Runtime runtime = Runtime.getRuntime();
		int numberOfProcessors = runtime.availableProcessors();
		int threadCount = numberOfProcessors;		
		//    int threadCount = 1;
		List<File> filesList = new ArrayList<File>(trainingFiles.length);
		for (int n=0; n < trainingFiles.length;n++) {
			filesList.add(trainingFiles[n]);
		}
		Collections.sort(filesList);
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
		long t3 = System.currentTimeMillis();
		Iterator<String> splitIter = splitMap.keySet().iterator();
		while (splitIter.hasNext()) {
			String curSplitVal = splitIter.next();
			splitMap.get(curSplitVal).close();
		}
		//merge into files of acceptable size
		trainingFiles = new File("splitF").listFiles();
		Arrays.sort(trainingFiles);
		HashMap<String, String> newSplitMap = new HashMap<String, String>();
		int curSize = 0;
		int splitThreshold = 15000;
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
				FileOutputStream out = new FileOutputStream("split/"+curFile);
				OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
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
		long t4 = System.currentTimeMillis();
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
			String featureString = posMap.get(index);
			//	String featureString = alphaParser.getIndexToValueArray()[index];
			//	curBw.append(featureString+" split/"+newFile+" "+newFile+"\n");
			curBw.append(featureString+" "+"split/"+newFile+" "+curSplitVal+"\n");
		}
		curBw.close();
		long t5 = System.currentTimeMillis();
		File[] files = new File("split").listFiles();	
		filesList = new ArrayList<File>(files.length);
		for (int n=0; n < files.length;n++) {
			filesList.add(files[n]);
		}
		Collections.sort(filesList);
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
		long t6 = System.currentTimeMillis();
		recreateOneAlphabetAndAdjustModels(alphabetFileParser, "splitA",splitModelsDir);
		long t7 = System.currentTimeMillis();
		//	System.out.println((t2-t1)+" "+(t3-t2)+" "+(t4-t3)+" "+(t5-t4)+" "+(t6-t5)+" "+(t7-t6));


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
			System.out.println(models[i]);
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

	void saveAlphabetInt(Alphabet alphaParser, Model model,List<HashMap<Integer,Integer>> compactList, File file) throws IOException {
		HashMap<Integer,Integer> newToOld = compactList.get(0);
		HashMap<Integer,Integer> oldToNew = compactList.get(1);
		FileOutputStream out = new FileOutputStream(file);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter bw = new BufferedWriter(or);
		String[] indexLabelArray = alphaParser.getIndexLabelArray();
		for (int i=1; i < alphaParser.getMaxLabelIndex();i++) {
			bw.append(i+" "+indexLabelArray[i]+"\n");
		}
		bw.append("\n");
		boolean notFinished = true;
		for (int i=1; notFinished && i < newToOld.size();i++) {
			int newIndex = i;
			Integer oldIndex = newToOld.get(i);
			if (oldIndex == null) {
				notFinished = false;
			}
			else {
				Integer value = oldToNew.get(oldIndex);
				bw.append(newIndex+" "+oldIndex+"\n");
			}
		}
		bw.close();
	}

	private void saveAlphabet2(Alphabet alphaParser, Model model,int[][] compactArray, File file) throws IOException {
		int[] newToOld = compactArray[0];
		FileOutputStream out = new FileOutputStream(file);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter bw = new BufferedWriter(or);
		String[] indexLabelArray = alphaParser.getIndexLabelArray();
		for (int i=1; i < alphaParser.getMaxLabelIndex();i++) {
			bw.append(i+" "+indexLabelArray[i]+"\n");
		}
		bw.append("\n");
		String[] indexToValue = alphaParser.getIndexToValueArray();
		boolean notFinished = true;
		for (int i=1; i < indexToValue.length;i++) {
			int newIndex = i;
			int oldIndex = newToOld[i];
			String stringValue = indexToValue[oldIndex];
			//	System.out.println(newIndex+" "+stringValue+" "+indexToValue.length);
			bw.append(newIndex+" "+stringValue+"\n");

		}
		bw.close();
	}

	private void saveModel(Model model, int[][] compactArray, File file) throws FileNotFoundException, IOException {		
		double[] wArray = model.getFeatureWeights();
		int[] newToOld = compactArray[0];
		int[] oldToNew = compactArray[1];
		int numberOfClasses = model.getNrClass();
		FileOutputStream out = new FileOutputStream(file);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter bw = new BufferedWriter(or);
		bw.append("solver_type MCSVM_CS\n");
		bw.append("nr_class "+model.getNrClass()+"\nlabel ");

		for (int k=0; k <model.getLabels().length;k++) {
			bw.append(model.getLabels()[k]+" ");
		}
		bw.append("\n");
		int lastIndex = -1;
		for (int k=1; k < oldToNew.length;k++) {
			int curOldIndex = k;
			int curNewIndex = oldToNew[curOldIndex];
			if (curNewIndex != 0) {
				for (int l=0; l < numberOfClasses; l++) {
					double curWeight = wArray[(curNewIndex-1)*numberOfClasses+l];
					if (curWeight != 0) {
						lastIndex = k;
					}
				}
			}
		}
		bw.append("nr_feature "+lastIndex+"\n");
		bw.append("bias "+model.getBias()+"\nw\n");
		for (int k=1; k < lastIndex;k++) {
			int curOldIndex = k;
			int curNewIndex = oldToNew[curOldIndex];
			//	System.out.println(curOldIndex+" "+curNewIndex);
			if (curNewIndex == 0) {
				for (int l=0; l < numberOfClasses; l++) {
					bw.append("0 ");
				}
			}
			else {
				for (int l=0; l < numberOfClasses; l++) {
					double curWeight = wArray[(curNewIndex-1)*numberOfClasses+l];
					bw.append(String.valueOf(curWeight)+" ");
				}
			}
			bw.append("\n");
		}
		bw.close();
	}

	private void saveModel2(Model model, int[][] compactArray, File file) throws FileNotFoundException, IOException {		
		double[] wArray = model.getFeatureWeights();
		int[] newToOld = compactArray[0];
		int[] oldToNew = compactArray[1];
		int numberOfClasses = model.getNrClass();
		FileOutputStream out = new FileOutputStream(file);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter bw = new BufferedWriter(or);
		bw.append("solver_type MCSVM_CS\n");
		bw.append("nr_class "+model.getNrClass()+"\nlabel ");

		for (int k=0; k <model.getLabels().length;k++) {
			bw.append(model.getLabels()[k]+" ");
		}
		bw.append("\n");
		int lastIndex = -1;
		for (int k=1; k < oldToNew.length;k++) {
			int curOldIndex = k;
			int curNewIndex = oldToNew[curOldIndex];
			if (curNewIndex != 0) {
				for (int l=0; l < numberOfClasses; l++) {
					double curWeight = wArray[(curNewIndex-1)*numberOfClasses+l];
					if (curWeight != 0) {
						lastIndex = k;
					}
				}
			}
		}
		bw.append("nr_feature "+lastIndex+"\n");
		bw.append("bias "+model.getBias()+"\nw\n");
		for (int k=1; k < lastIndex;k++) {
			int curNewIndex = k;
			for (int l=0; l < numberOfClasses; l++) {
				double curWeight = wArray[(curNewIndex-1)*numberOfClasses+l];
				bw.append(String.valueOf(curWeight)+" ");
			}
			bw.append("\n");
		}
		bw.close();
	}

	private HashMap<String,int[][]> compactiseTrainingDataFiles(Alphabet alphaParser, HashMap<String, List<FeatureVector>> mergedMap) {
		int maxIndex = alphaParser.getMaxIndex();
		alphaParser.createIndexToValueArray();
		HashMap<String,int[][]> compactMap = new HashMap<String, int[][]>();
		Iterator<String> iter = mergedMap.keySet().iterator();
		while (iter.hasNext()) {
			String curFeature = iter.next();
			List<FeatureVector> curTrainingData = mergedMap.get(curFeature);
			int[][] compactArray = new int[4][];
			int[] newToOld = new int[maxIndex+1];
			int[] oldToNew = new int[maxIndex+1];
			int[] newToOldL = new int[alphaParser.getMaxLabelIndex()+1];
			int[] oldToNewL = new int[alphaParser.getMaxLabelIndex()+1];
			compactArray[0] = newToOld;
			compactArray[1] = oldToNew;
			compactArray[2] = newToOldL;
			compactArray[3] = oldToNewL;
			compactMap.put(curFeature,compactArray);
			int curMaxIndex = 1;
			int curLabelMaxIndex = 1;
			Set<Integer> alreadyProcessed = new HashSet<Integer>();
			Set<Integer> alreadyProcessedLabels = new HashSet<Integer>();
			List<FeatureVector> compactisedTrainingData = new ArrayList<FeatureVector>(curTrainingData.size());
			for (int i=0; i < curTrainingData.size();i++) {
				FeatureVector fv = curTrainingData.get(i);
				FeatureVector newFv = new FeatureVector(true);
				String label = fv.getLabel();
				//	System.out.println(label);
				Integer labelOld = alphaParser.getLabelIndexMap().get(label);
				/*	Integer labelNew = -1;
				if (!alreadyProcessedLabels.contains(labelOld)) {
					labelNew = curLabelMaxIndex;
					oldToNewL[labelOld] = labelNew;
					newToOldL[labelNew] = labelOld;
					alreadyProcessedLabels.add(labelOld);
					curLabelMaxIndex++;
				}*/
				newFv.setLabel(label);
				List<Feature> fList = fv.getfList();
				List<Feature> newFList = new ArrayList<Feature>();
				for (int k=0; k < fList.size();k++) {
					Feature f = fList.get(k);
					Integer oldIndex = alphaParser.getFeatureIndex(f.getFeatureString());
					if (!alreadyProcessed.contains(oldIndex)) {
						alreadyProcessed.add(oldIndex);
						oldToNew[oldIndex] = curMaxIndex;
						newToOld[curMaxIndex] = oldIndex;
						curMaxIndex++;
					}
					Feature newF = f.clone();
					newF.setIndexParser(oldToNew[oldIndex]);
					newFList.add(newF);
				}
				newFv.setfList(newFList);
				compactisedTrainingData.add(newFv);
			}
			mergedMap.put(curFeature, compactisedTrainingData);
		}
		return compactMap;


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

	List<HashMap<Integer,Integer>> compactiseTrainingDataFileInt(File curentTrainingFile,int size, File splitC) throws IOException {
		List<HashMap<Integer,Integer>> compactList = new ArrayList<HashMap<Integer,Integer>>();
		HashMap<Integer,Integer> oldToNew = new HashMap<Integer,Integer>(size);
		HashMap<Integer,Integer> newToOld = new HashMap<Integer,Integer>(size);
		compactList.add(newToOld);
		compactList.add(oldToNew);
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
		while ((line = fr.readLine())!= null) {
			String[] lineArray = line.split(" ");
			curBw.append(lineArray[0]);
			List<Integer> featureList = new ArrayList<Integer>();
			for (int i=1; i < lineArray.length;i++ ) {
				Integer curFeature = Integer.valueOf(lineArray[i].split(":")[0]);
				Integer newValue = oldToNew.get(curFeature);
				if (newValue == null) {
					newValue = maxIndex;
					oldToNew.put(curFeature,newValue);
					newToOld.put(newValue,curFeature);
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
		return compactList;
	}


	private void guaranteeOrder(HashMap<String, List<FeatureVector>> splitMap, Alphabet alpha) {
		Iterator<String> iter = splitMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			List<FeatureVector> curList = splitMap.get(key);
			for (int i=0; i < alpha.getMaxLabelIndex();i++) {
				curList.add(curList.get(i));
			}
			boolean[] b = new boolean[alpha.getMaxLabelIndex()];
			int curIndex = 1;
			for (int i=alpha.getMaxLabelIndex(); i < curList.size() && curIndex < alpha.getMaxLabelIndex();i++) {
				FeatureVector fv = curList.get(i);
				String label = fv.getLabel();
				int labelIndex = alpha.getLabelIndexMap().get(label);
				if (!b[labelIndex-1]) {
					curList.set(labelIndex-1, fv);
					curIndex++;
					b[labelIndex-1] = true;
				}
			}

		}

	}

	private Problem myConstructProblem(List<List<Integer>> xList, List<Integer> yList, int max_index) {
		Problem prob = new Problem();
		prob.bias = bias;
		prob.l = yList.size();
		prob.n = max_index;
		prob.x = new FeatureNode[prob.l][];
		for (int i = 0; i < prob.l; i++) {
			List<Integer> curList = xList.get(i);
			FeatureNode[] fnArray = new FeatureNode[curList.size()];
			for (int k=0; k < curList.size();k++) {
				FeatureNode fn = new FeatureNode(curList.get(k),1);
				fnArray[k] = fn;
			}
			prob.x[i] = fnArray;
			assert prob.x[i][prob.x[i].length - 1] != null;     
		}
		//GN: May, 2016
		// prob.y = new int[prob.l];
		prob.y = new double[prob.l];
		for (int i = 0; i < prob.l; i++)
			prob.y[i] = yList.get(i);

		return prob;

	}

	private void addAtRightPosition(int index, Alphabet alpha,List<FeatureVector> listForThisSplitVal, FeatureVector fv,boolean[] bArray ) {
		if (index >= listForThisSplitVal.size()) {
			listForThisSplitVal.add(fv);
		}
		else {
			if (bArray[index]) {
				listForThisSplitVal.add(fv);
			}
			else {
				boolean found = false;
				int ind = -1;
				for (int i=0; i < listForThisSplitVal.size() && !found;i++) {
					String curFvLabel = listForThisSplitVal.get(i).getLabel();
					int curFvIndex = alpha.getLabelIndexMap().get(curFvLabel);
					if (index < curFvIndex) {
						found = true;
						ind = i;
					}
				}
				if (ind != -1) {
					listForThisSplitVal.add(ind,fv);
					bArray[index] = true;
				}
				else {
					listForThisSplitVal.add(fv);
				}
			}
		}
	}

	public void readProblem(String filename, int threshold, int[] counts) throws IOException, InvalidInputDataException {
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
				Set<Integer> featureSet ;
				if (bias >= 0) {
					//      x = new FeatureNode[m + 1];
					featureSet = new HashSet<Integer>(m+1);
				}    
				else {
					//          x = new FeatureNode[m];
					featureSet = new HashSet<Integer>(m);
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
						if (counts[index] > threshold) {
							featureSet.add(index);	                     
						}
					} catch (NumberFormatException e) {
						throw new InvalidInputDataException("invalid value: " + token, filename, lineNr);
					}
				}
				x = new FeatureNode[featureSet.size()];
				int j=0;
				ArrayList<Integer> sortedFeatures = new ArrayList<Integer>(featureSet);
				Collections.sort(sortedFeatures);
				for (Integer fn : sortedFeatures) {
					x[j] = new FeatureNode(fn,1);
					j++;
				}
				if (featureSet.size() > 0) {
					max_index = Math.max(max_index, x[featureSet.size() - 1].index);
				}
				vx.add(x);
			}






			prob = constructProblem(vy, vx, max_index);
		}
		finally {
			fp.close();
		}
	}

	private int[] countFeatures(String fileName) throws IOException{
		int[] counts = new int[numberOfFeatures+1];
		FileInputStream in = new FileInputStream(fileName);
		BufferedInputStream bis = new BufferedInputStream(in, 8000);
		InputStreamReader ir = new InputStreamReader(bis,"UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line;
		while ((line = fr.readLine())!= null) {
			String[] lineArray = line.split(" ");
			for (int i = 1 ; i < lineArray.length; i++) {
				int featureIndex = Integer.valueOf(lineArray[i].split(":")[0]);
				counts[featureIndex]++;
			}
		}
		return counts;
	}

	public  Problem constructProblem(List<Integer> vy, List<FeatureNode[]> vx, int max_index) {
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

	public void createTrainingDataWithSplitting(String algorithm,String inputFile, String outputDir, String outputDirL, String alphabetFileParser,String alphabetFileLabeler) throws IOException {
		boolean noLabels = true;
		HashMap<String,StringBuilder> splitMap = new HashMap<String,StringBuilder>();
		HashMap<String,StringBuilder> splitMapFirst = new HashMap<String,StringBuilder>();
		HashMap<String,Integer> splitMapFirstIndex = new HashMap<String,Integer>();
		HashMap<String,Integer> splitMapCounts = new HashMap<String,Integer>();
		HashMap<String,String> splitMapMerged = new HashMap<String,String>();

		HashMap<String,StringBuilder> splitMapL = new HashMap<String,StringBuilder>();
		HashMap<String,Integer> splitMapCountsL = new HashMap<String,Integer>();
		HashMap<String,String> splitMapMergedL = new HashMap<String,String>();

		Data d = new Data(inputFile, true);
		Alphabet alphaParser = new Alphabet();
		Alphabet alphaLabeler = new Alphabet();
		//	Alphabet alphaParser = new Alphabet(alphabetFileParser);
		//	Alphabet alphaLabeler = new Alphabet(alphabetFileLabeler);
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
		for (int n=0; n < sentences.length;n++) {
			Sentence sent = sentences[n];
			fm.initializeStaticFeaturesCombined(sent, true);
			List<FeatureVector> parserList = pa.processCombined(sent, fm, noLabels);
			for (int i=0; i < parserList.size(); i++) {
				FeatureVector fv = parserList.get(i);
				int curLabel = Integer.valueOf(alphaParser.getLabelIndexMap().get(fv.getLabel()));
				Feature splitFeature = fv.getFeature("pj");
				String splitVal = splitFeature.getFeatureString();
				StringBuilder sb = splitMap.get(splitVal);
				if (sb == null) {
					sb = new StringBuilder();
					splitMap.put(splitVal, sb);
					splitMapCounts.put(splitVal, 1);
				}
				else {
					int c = splitMapCounts.get(splitVal);
					splitMapCounts.put(splitVal, c+1);
				}
				Integer curIndexParser = splitMapFirstIndex.get(splitVal);
				if (curIndexParser == null) {
					curIndexParser = 1;
				}
				if (curLabel == curIndexParser) {
					StringBuilder sbFirst = splitMapFirst.get(splitVal);
					if (sbFirst == null) {
						sbFirst = new StringBuilder();
						splitMapFirst.put(splitVal,sbFirst);
					}
					sbFirst.append(fv.getIntegerRepresentation(alphaParser, false));
					sbFirst.append("\n");
					curIndexParser++;
					splitMapFirstIndex.put(splitVal, curIndexParser);
				}
				else {
					sb.append(fv.getIntegerRepresentation(alphaParser, false));
					sb.append("\n");
				}
			}
		}
		alphaParser.printToFile(alphabetFileParser);
		alphaLabeler.printToFile(alphabetFileLabeler);
		setMaxLabelParser(alphaParser.getMaxLabelIndex());
		setMaxLabelLabeler(alphaLabeler.getMaxLabelIndex());				
		//merging split maps parser
		this.numberOfFeatures = alphaParser.getMaxIndex();
		HashMap<String,String> newSplitMap = codeSplitMap(splitMap);
		Iterator<String> keyIter = splitMap.keySet().iterator();
		int curCount = 0;
		BufferedWriter curBw = null;
		StringBuilder sb = null;
		String outputFile = "";
		int t = 1000;
		boolean wellformed = false;
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			String newKey = newSplitMap.get(key);
			int count = splitMapCounts.get(key);		
			if (curBw == null) {
				outputFile = outputDir+"/"+newKey+".txt";
				FileOutputStream out = new FileOutputStream(outputFile);
				OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
				curBw = new BufferedWriter(or);
				wellformed = false;
				sb = new StringBuilder();
			}
			if (!wellformed) {
				Integer curNumbInFirst = splitMapFirstIndex.get(key);
				if (curNumbInFirst != null) {
					if (curNumbInFirst == alphaParser.getMaxLabelIndex()) {		
						curBw.append(splitMapFirst.get(key).toString());
						wellformed = true;
					}
				}
			}
			sb.append(splitMap.get(key).toString());
			curCount += count;
			if (curCount > t) {
				curBw.append(sb.toString());
				sb = null;
				wellformed = false;
				curBw.close();
				curBw = null;
				curCount = 0;				
			}
			splitMapMerged.put(key,outputFile);
		}
		if (curBw != null) {
			curBw.append(sb.toString());
			curBw.close();
		}
		printSplitMap(splitMapMerged, "temp/split.txt", newSplitMap);

		//mergins plit maps labeler
		HashMap<String,String> newSplitMapL = codeSplitMap(splitMapL);
		Iterator<String> keyIterL = splitMapL.keySet().iterator();
		curCount = 0;
		curBw = null;
		sb = null;
		HashMap<String,String> outputMap = new HashMap<String,String>();
		outputFile = "";
		t = 10000;
		/*				FileOutputStream out = new FileOutputStream(outputFile);
				OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
				curBw = new BufferedWriter(or);*/
		while (keyIterL.hasNext()) {
			String key = keyIterL.next();
			String newKey = newSplitMapL.get(key);
			int count = splitMapCountsL.get(key);
			if (sb == null) {
				outputFile = outputDirL+"/"+newKey+".txt";
				sb = new StringBuilder();
			}
			sb.append(splitMapL.get(key).toString());
			//	curBw.append(splitMapL.get(key).toString());
			curCount += count;
			if (curCount > t) {
				outputMap.put(outputFile, sb.toString());			
				sb = null;
				curCount = 0;				
			}
			splitMapMergedL.put(key,outputFile);
		}
		if (sb != null) {
			outputMap.put(outputFile, sb.toString());	
		}
		String line;
		Iterator<String> iter = outputMap.keySet().iterator();
		String[] classArray = new String[alphaLabeler.getMaxLabelIndex()];
		boolean[] classArrayBoolean = new boolean[alphaLabeler.getMaxLabelIndex()];
		while (iter.hasNext()) {
			String curOutputFile = iter.next();
			String content = outputMap.get(curOutputFile);
			BufferedReader br = new BufferedReader(new StringReader(content));
			FileOutputStream out = new FileOutputStream(curOutputFile);
			OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
			curBw = new BufferedWriter(or);
			sb = new StringBuilder();
			while ((line = br.readLine())!= null) {
				int curIndex = Integer.valueOf(line.split(" ")[0]);
				if (!classArrayBoolean[curIndex]) {
					classArray[curIndex] = line+"\n";
					classArrayBoolean[curIndex] = true;
				}
				else {
					sb.append(line+"\n");
				}
			}
			for (int i=1; i < classArray.length;i++) {
				if (classArrayBoolean[i])
					curBw.append(classArray[i]);
			}
			curBw.append(sb.toString());
			curBw.close();
		}
		printSplitMap(splitMapMergedL, "temp/splitL.txt", newSplitMapL);

	}

	public void trainModelsWithSplitting(String trainingFilesDir, String splitFile) throws IOException, InvalidInputDataException {
		Set<String> trained = new HashSet<String>();
		HashMap<String,String> splitMap = readSplitFile(splitFile);
		Iterator<String> keyIter = splitMap.keySet().iterator();
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			String curInputFile = splitMap.get(key);
			if (!trained.contains(curInputFile)) {
				readProblem(curInputFile);
				File f = new File(curInputFile);			
				Model model = Linear.train(this.prob, this.param);
				model.save(new File(trainingFilesDir+"/"+f.getName()));
				trained.add(curInputFile);
			}

		}

	}

	private HashMap<String,String> readSplitFile(String splitFile) throws IOException {
		HashMap<String,String> splitMap = new HashMap<String,String>();
		BufferedReader fp = new BufferedReader(new FileReader(splitFile));
		String line;
		while ((line = fp.readLine())!= null) {
			String[] lineArray = line.split(" ");
			splitMap.put(lineArray[0], lineArray[1]);
		}
		return splitMap;
	}

	private void printSplitMap(HashMap<String, String> splitMapMerged, String splitMapFile, HashMap<String,String> newSplitMap) throws IOException {
		FileOutputStream out = new FileOutputStream(splitMapFile);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter bw = new BufferedWriter(or);
		Iterator<String> keyIter = splitMapMerged.keySet().iterator();
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			bw.append(key+" "+splitMapMerged.get(key)+" "+newSplitMap.get(key)+"\n");
		}
		bw.close();	
	}

	public void trainModel(String inputFile, String modelFile) throws IOException, InvalidInputDataException {
		readProblem(inputFile);
		Linear.disableDebugOutput();
		Model model = Linear.train(this.prob, this.param);

		model.save(new File(modelFile));
	}

	public void removeRareFeatures(String outputFile, String alphabetFile, int threshold) throws IOException {
		Alphabet alpha = new Alphabet(alphabetFile);
		this.numberOfFeatures = alpha.getMaxIndex();
		int[] counts = countFeatures(outputFile);
		HashMap<Integer,Integer> oldIndexToNewIndex = new HashMap<Integer,Integer>(counts.length);
		int curNewIndex = 1;
		for (int i = 1; i < counts.length; i++) {
			if (counts[i] > threshold) {
				oldIndexToNewIndex.put(i, curNewIndex);
				curNewIndex++;
			}
		}
		StringBuilder sb = new StringBuilder();
		FileInputStream in = new FileInputStream(outputFile);
		BufferedInputStream bis = new BufferedInputStream(in, 8000);
		InputStreamReader ir = new InputStreamReader(bis,"UTF8");
		BufferedReader fr = new BufferedReader(ir);
		String line;
		while ((line = fr.readLine())!= null) {
			String[] lineArray = line.split(" ");
			List<Integer> l = new ArrayList<Integer>();
			for (int i = 1 ; i < lineArray.length; i++) {
				int featureIndex = Integer.valueOf(lineArray[i].split(":")[0]);
				Integer newFeatureIndex = oldIndexToNewIndex.get(featureIndex);
				if (newFeatureIndex != null) {
					l.add(newFeatureIndex);
				}
			}
			if (l.size() > 0) {
				sb.append(lineArray[0]);
				for (int k=0; k < l.size();k++) {
					sb.append(" ");
					sb.append(l.get(k));
					sb.append(":1");
				}
				sb.append("\n");
			}
		}
		FileOutputStream out = new FileOutputStream(outputFile);
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter fw = new BufferedWriter(or);
		fw.append(sb.toString());
		fw.close();
		String[] newIndexToValueArray = new String[oldIndexToNewIndex.size()+1];
		String[] oldIndexToValueArray = alpha.getIndexToValueArray();
		for (int i = 1; i < oldIndexToValueArray.length; i++) {
			String val = oldIndexToValueArray[i];
			Integer newIndex = oldIndexToNewIndex.get(i);
			if (newIndex != null) {
				newIndexToValueArray[newIndex] = val;
			}
		}

		HashMap<String,Integer> newValueToIndexMap = new HashMap<String,Integer>(oldIndexToNewIndex.size()+1);
		HashMap<String,Integer> oldValueToIndexMap = alpha.getValueToIndexMap();
		Iterator<String> iter = oldValueToIndexMap.keySet().iterator();
		while (iter.hasNext()) {
			String val = iter.next();
			int oldIndex = oldValueToIndexMap.get(val);
			Integer newIndex = oldIndexToNewIndex.get(oldIndex);
			if (newIndex != null) {
				newValueToIndexMap.put(val,newIndex);
			}
		}	
		alpha.setIndexToValueArray(newIndexToValueArray);
		alpha.setValueToIndexMap(newValueToIndexMap);
		alpha.printToFile(alphabetFile);

	}

	public void removeRareFeaturesWithSplitting(String splitTrainingDir,String alphabetFileParser, int tMin, int tMax) throws IOException {
		Alphabet alpha = new Alphabet(alphabetFileParser);
		this.numberOfFeatures = alpha.getMaxIndex();
		int minFeatureNumber = Integer.MAX_VALUE;
		File[] models = new File(splitTrainingDir).listFiles();
		int[][] countsArray = new int[models.length][];
		for (int i=0; i < models.length;i++) {
			int[] curCounts = countFeatures(models[i].getAbsolutePath());
			countsArray[i] = curCounts;
			if (curCounts.length < minFeatureNumber) {
				minFeatureNumber = curCounts.length;
			}
		}
		boolean[] toRemove = new boolean[minFeatureNumber];
		for (int f = 0; f < minFeatureNumber; f++) {
			int minFreqForThisFeature = Integer.MAX_VALUE;
			int maxFreqForThisFeature = 0;
			for (int i=0; i < countsArray.length;i++) {
				if (countsArray[i][f] > maxFreqForThisFeature) {
					maxFreqForThisFeature = countsArray[i][f];					
				}
				if (countsArray[i][f] < minFreqForThisFeature) {
					minFreqForThisFeature = countsArray[i][f];
				}
			}
			if (minFreqForThisFeature > tMin || maxFreqForThisFeature > tMax) {
				toRemove[f] = false;
			}
			else {
				toRemove[f] = true;
			}
		}
		HashMap<Integer,Integer> oldIndexToNewIndex = new HashMap<Integer,Integer>(minFeatureNumber);
		int c = 1;
		for (int i=0; i < models.length;i++) {
			StringBuilder sb = new StringBuilder();
			BufferedReader fp = new BufferedReader(new FileReader(models[i]));
			String line;
			while ((line = fp.readLine())!= null) {
				String[] lineArray = line.split(" ");
				ArrayList<Integer> fs = new ArrayList<Integer>();
				for (int k=1; k < lineArray.length;k++) {
					int fIndex = Integer.valueOf(lineArray[k].split(":")[0]);
					if (!toRemove[fIndex]) {						
						Integer newIndex = oldIndexToNewIndex.get(fIndex);
						if (newIndex == null) {
							newIndex = c;
							oldIndexToNewIndex.put(fIndex, c);
							c++;
						}
						fs.add(newIndex);
					}
				}
				if (!fs.isEmpty()) {
					Collections.sort(fs);
					sb.append(lineArray[0]);
					for (int k=0; k < fs.size();k++) {
						sb.append(" ");
						sb.append(fs.get(k));
						sb.append(":1");
					}
					sb.append("\n");
				}
			}
			FileOutputStream out = new FileOutputStream(models[i]);
			OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
			BufferedWriter bw = new BufferedWriter(or);
			bw.append(sb.toString());
			bw.close();
		}
		String[] newIndexToValueArray = new String[oldIndexToNewIndex.size()+1];
		String[] oldIndexToValueArray = alpha.getIndexToValueArray();
		for (int i = 1; i < oldIndexToValueArray.length; i++) {
			String val = oldIndexToValueArray[i];
			Integer newIndex = oldIndexToNewIndex.get(i);
			if (newIndex != null) {
				newIndexToValueArray[newIndex] = val;
			}
		}

		HashMap<String,Integer> newValueToIndexMap = new HashMap<String,Integer>(oldIndexToNewIndex.size()+1);
		HashMap<String,Integer> oldValueToIndexMap = alpha.getValueToIndexMap();
		Iterator<String> iter = oldValueToIndexMap.keySet().iterator();
		while (iter.hasNext()) {
			String val = iter.next();
			int oldIndex = oldValueToIndexMap.get(val);
			Integer newIndex = oldIndexToNewIndex.get(oldIndex);
			if (newIndex != null) {
				newValueToIndexMap.put(val,newIndex);
			}
		}	
		alpha.setIndexToValueArray(newIndexToValueArray);
		alpha.setValueToIndexMap(newValueToIndexMap);
		alpha.printToFile(alphabetFileParser);

	}

	public void reorderWeights(String outputDir, int numberOfClasses) throws IOException {
		File[] models = new File(outputDir).listFiles();
		for (int i=0; i < models.length;i++) {
			FileInputStream in = new FileInputStream(models[i]);
			InputStreamReader ir = new InputStreamReader(in,"UTF8");
			BufferedReader fr = new BufferedReader(ir);
			fr.readLine();
			fr.readLine();
			String labString = fr.readLine();
			String[] labArray = labString.split(" ");
			int start = 0;
			boolean needed = false;
			int cc = 0;
			for (int k=1; k < labArray.length;k++) {
				Integer curLab = Integer.valueOf(labArray[k]);
				if (curLab > start) {
					start = curLab;
					cc++;
				}
				else {
					needed = true;
				}
			}
			if (cc != (numberOfClasses)) {
				needed = true;
			}
			if (needed) {
				Model curModel = Linear.loadModel(models[i]);
				StringBuilder curModelSb = new StringBuilder();
				int nrFeature = curModel.getNrFeature();		

				double[] oldWeights = curModel.getFeatureWeights();
				int[] labels = curModel.getLabels();	
				Set<Integer> labelsSet = new HashSet<Integer>();
				Set<Integer> missingSet = new HashSet<Integer>();
				for (int k=0; k < labels.length;k++) {
					labelsSet.add(labels[k]);
				}
				for (int k=1; k < numberOfClasses;k++) {
					if (!labelsSet.contains(k)) {
						missingSet.add(k);
					}
				}
				double[] newWeights;
				if (!missingSet.isEmpty()) {
					newWeights = new double[nrFeature*(labels.length+missingSet.size())];
				}
				else {
					newWeights	= new double[nrFeature*labels.length];
				}			
				for (int fIndex=0; fIndex < nrFeature; fIndex++) {
					int c = 0;
					for (int l=0; l < labels.length+missingSet.size();l++) {				
						if (missingSet.contains(l+1)) {
							newWeights[fIndex*(labels.length+missingSet.size())+l] = 0;
							c++;
						}
						else {
							double curWeight = oldWeights[fIndex*labels.length+(l-c)];		
							int curWeightPosition = labels[l-c]-1;
							newWeights[fIndex*(labels.length+missingSet.size())+curWeightPosition] = curWeight;
						}						
					}
				}
				curModelSb.append("solver_type MCSVM_CS\n");
				curModelSb.append("nr_class "+(labels.length+missingSet.size())+"\n");
				curModelSb.append("label");
				for (int l=1; l <= labels.length+missingSet.size();l++) {
					curModelSb.append(" "+l);
				}
				curModelSb.append("\n");
				curModelSb.append("nr_feature "+nrFeature+"\n");
				curModelSb.append("bias -1.000000000000000\n");
				curModelSb.append("w\n");
				for (int fIndex=0; fIndex < nrFeature; fIndex++) {
					for (int l=0; l < labels.length+missingSet.size();l++) {
						double curWeight = newWeights[fIndex*(labels.length+missingSet.size())+l];
						if (curWeight == 0) {
							curModelSb.append("0 ");
						}
						else {
							curModelSb.append(curWeight+" ");
						}
					}
					curModelSb.append("\n");			
				}
				FileOutputStream out = new FileOutputStream(models[i].getAbsolutePath());
				OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
				BufferedWriter bw = new BufferedWriter(or);
				bw.append(curModelSb.toString());
				bw.close();
			}
		}
	}

	public HashMap<String,String>  codeSplitMap(HashMap<String,StringBuilder> splitMap) throws IOException {
		HashMap<String,String> newSplitMap = new HashMap<String,String>();
		int n=1;
		Iterator<String> keyIter = splitMap.keySet().iterator();
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			//	bw.append(n+" "+key);
			newSplitMap.put(key, String.valueOf(n));
			n++;
		}
		return newSplitMap;	
	}


	public void setMaxLabelLabeler(int maxLabelLabeler) {
		this.maxLabelLabeler = maxLabelLabeler;
	}

	public int getMaxLabelLabeler() {
		return maxLabelLabeler;
	}

	public void setMaxLabelParser(int maxLabelParser) {
		this.maxLabelParser = maxLabelParser;
	}

	public int getMaxLabelParser() {
		return maxLabelParser;
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
