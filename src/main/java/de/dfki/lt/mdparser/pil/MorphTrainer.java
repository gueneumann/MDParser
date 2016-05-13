package de.dfki.lt.mdparser.pil;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import pi.ParIterator;
import pi.ParIteratorFactory;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
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
import de.dfki.lt.mdparser.parser.CompactiseWorkerThread;
import de.dfki.lt.mdparser.parser.SplitWorkerThread;

public class MorphTrainer {



	private double    bias             = -1;
	private Problem   prob             = null;
	private Parameter param = new Parameter(SolverType.MCSVM_CS, 0.1, 0.3);

	public void train(Data d, String morphFeature) throws IOException, InvalidInputDataException {
		System.out.println("Training for "+morphFeature);
		Alphabet alphaMorph = new Alphabet();
		FeatureExtractor fe = new FeatureExtractor();
		MorphModel mm = new MorphModel(alphaMorph, fe);
		Sentence[] sentences = d.getSentences();
		List<FeatureVector> trainingData = new ArrayList<FeatureVector>();
		for (int n=0; n < sentences.length;n++) {
			Sentence sent = sentences[n];
			String[][] sentArray = sent.getSentArray();
			for (int i=0; i < sentArray.length;i++) {
				FeatureVector fv = mm.apply(i+1,sent, true);
				MorphFeatureVector mfv = new MorphFeatureVector(fv, sentArray[i][5]);
				String label = mfv.getLabelsMap().get(morphFeature);
				fv.setLabel(label);
				alphaMorph.addLabel(label);
				trainingData.add(fv);
			}
		}
		FileOutputStream out = new FileOutputStream("split/"+morphFeature+".txt");
		OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
		BufferedWriter curBw = new BufferedWriter(or);
		for (int i=0; i < trainingData.size();i++) {
			FeatureVector fv = trainingData.get(i);
			curBw.append(fv.getIntegerRepresentation(alphaMorph, false)+"\n");
		}
		curBw.close();
		alphaMorph.printToFile("temp/alphaMorph"+morphFeature+".txt");
		readProblem(new File("split/"+morphFeature+".txt"));
		Linear.disableDebugOutput();
		Model m = Linear.train(prob, param);
		Linear.saveModel(new File("morphModels/"+morphFeature+".txt"), m);
	}
















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

	public void readProblem(File filename) throws IOException, InvalidInputDataException {
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

}
