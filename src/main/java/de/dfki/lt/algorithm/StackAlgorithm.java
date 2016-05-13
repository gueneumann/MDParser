package de.dfki.lt.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.dfki.lt.data.Sentence;
import de.dfki.lt.features.FeatureModel;
import de.dfki.lt.features.FeatureVector;

public class StackAlgorithm extends ParsingAlgorithm{

	private DependencyStructure goldDepStruct;
	private int maxi;

	public int getNumberOfConfigurations() {
		return super.getNumberOfConfigurations();
	}

	public void setNumberOfConfigurations(int numberOfConfigurations) {
		super.setNumberOfConfigurations(numberOfConfigurations);	
	}
	
	
	//TRAIN
	public List<List<FeatureVector>> process(Sentence sentence,	FeatureModel fm, boolean noLabels) {
		List<List<FeatureVector>> res = new ArrayList<List<FeatureVector>>(2);
		List<FeatureVector> fvParserList = new ArrayList<FeatureVector>();
		List<FeatureVector> fvLabelerList = new ArrayList<FeatureVector>();
		String[][] sentArray = sentence.getSentArray();		
		Stack<Integer> buffer = initBuffer(sentArray.length);
		Stack<Integer> stack = new Stack<Integer>();
		stack.add(0);
		DependencyStructure curDepStruct = new DependencyStructure(sentArray.length);
		initGoldDepStruct(sentArray);
		StackParserState curState = new StackParserState(stack,buffer,sentence,curDepStruct);
	//	fm.initializeStaticFeatures(sentence,true);
		while (!curState.isTerminal()) {
			List<FeatureVector> fvList = fm.apply(curState, true, noLabels);
			FeatureVector fvParser = fvList.get(0);
			FeatureVector fvLabeler = fvList.get(1);			
	//		System.out.print(curState.getStackToken(0)+" "+curState.getBufferToken(0)+" "+maxi+" "+stack+" "+buffer+" ");
	//o		String label = findOutCorrectLabel(curState.getStackToken(0), curState.getBufferToken(0), sentArray);
			String label = findOutCorrectLabel2(curState.getStack(), curState.getBufferToken(0), sentArray);
	//		System.out.println(label+" "+curDepStruct.getDependencies()+" "+curDepStruct.getDependencies().size());
			fvParser.setLabel(label);
			if (label.equals("j")) {
				sentArray[curState.getStackToken(0)-1][8] = String.valueOf(curState.getBufferToken(0));
				String depRel = sentArray[curState.getStackToken(0)-1][7];
				sentArray[curState.getStackToken(0)-1][9] = depRel;
				curDepStruct.addDependency(new Dependency(curState.getStackToken(0),curState.getBufferToken(0),depRel));				
				fvLabeler.setLabel(depRel);
				fvLabelerList.add(fvLabeler);
				stack.pop();
				fm.getAlphabetLabeler().addLabel(depRel);
			}
			else if (label.equals("i")) {
				sentArray[curState.getBufferToken(0)-1][8] = String.valueOf(curState.getStackToken(0));
				String depRel = sentArray[curState.getBufferToken(0)-1][7];
				sentArray[curState.getBufferToken(0)-1][9] = depRel;
				curDepStruct.addDependency(new Dependency(curState.getBufferToken(0),curState.getStackToken(0),depRel));
				fvLabeler.setLabel(depRel);
				fvLabelerList.add(fvLabeler);
				int stackTop = stack.peek();
				int bufferTop = curState.getBufferToken(0);
		//o		buffer.remove(0);
		/*o		if (stackTop != 0) {
					buffer.add(0,stack.pop());
					buffer.remove(0);
				}
				else {
					stack.push(bufferTop);
				}*/
				stack.push(buffer.remove(0));
				fm.getAlphabetLabeler().addLabel(depRel);
				
				
			}
			else if (label.equals("reduce")) {
				stack.pop();
			}
			else {
				if (!buffer.isEmpty()) {					
					stack.push(buffer.remove(0));
				}
			}
			if (buffer.isEmpty()) {
				curState.setTerminal(true);
			}
			fm.getAlphabetParser().addLabel(label);
			fvParserList.add(fvParser);
			maxi = Math.max(curState.getBufferToken(0),maxi);
		}
		res.add(fvParserList);
		res.add(fvLabelerList);
		return res;
	}

	private void initGoldDepStruct(String[][] sentArray) {
		this.maxi = 0;
		this.goldDepStruct = new DependencyStructure(sentArray.length);
		for (int i=0; i < sentArray.length;i++) {
			this.goldDepStruct.addDependency(new Dependency(Integer.valueOf(sentArray[i][0]),Integer.valueOf(sentArray[i][6]),sentArray[i][7]));
		}
	}

	private Stack<Integer> initBuffer(int length) {
		Stack<Integer> buffer = new Stack<Integer>();
		for (int j=1; j < length+1; j++) {
			buffer.push(j);
		}
		return buffer;
	}

	private void postprocess(String[][] sentArray, Sentence sent) {
		for (int j=0; j < sentArray.length; j++) {
			if (sentArray[j][6] == null || sentArray[j][6].equals("_")) {
				int root = sent.getRootPosition();
				if (root == -1) {
					root = 1;
				}
				sentArray[j][6] = String.valueOf(root);
			}
		}	
	}
	
	//TEST
	public void process(Sentence sent,	FeatureModel fm, boolean noLabels,
			HashMap<String, String> splitMap, HashMap<String, String> splitMapL) {
		String[][] sentArray = sent.getSentArray();		
		Stack<Integer> buffer = initBuffer(sentArray.length);
		Stack<Integer> stack = new Stack<Integer>();
		stack.add(0);
		DependencyStructure curDepStruct = new DependencyStructure(sentArray.length);
		StackParserState curState = new StackParserState(stack,buffer,sent,curDepStruct);
	//	fm.initializeStaticFeatures(sent,false);
		while (!curState.isTerminal()) {
			List<FeatureVector> fvList = fm.apply(curState, true, noLabels);
			FeatureVector fvParser = fvList.get(0);
			FeatureVector fvLabeler = fvList.get(1);
	//		System.out.print(sent.getRootPosition()+" "+curState.getStackToken(0)+" "+curState.getBufferToken(0)+" "+stack+" "+buffer+" ");
			String mName = "";
			if (splitMap.get(fvParser.getFeature("pj").getFeatureString()) == null) {
				List<String> mNames = new ArrayList<String>(splitMap.values());
				mName = mNames.get(0);
			}
			else {
				mName = splitMap.get(fvParser.getFeature("pj").getFeatureString());
			}
			Model curModel = this.getParser().getSplitModelMap().get(mName);	
			int labelInt = (int) Linear.predict(curModel, fvParser.getLiblinearRepresentation(false,false,fm.getAlphabetParser()));
			String label = fm.getAlphabetParser().getIndexLabelArray()[labelInt];
	//		System.out.println(label+" "+curDepStruct.getDependencies()+" "+curDepStruct.getDependencies().size());
			fvParser.setLabel(label);
			if (label.equals("j") && 0 != curState.getStackToken(0)) {
				sentArray[curState.getStackToken(0)-1][6] = String.valueOf(curState.getBufferToken(0));
				mName = "";
				if (splitMap.get(fvLabeler.getFeature("pj").getFeatureString()) == null) {
					List<String> mNames = new ArrayList<String>(splitMapL.values());
					mName = mNames.get(0);
				}
				else {
					mName = splitMapL.get(fvParser.getFeature("pj").getFeatureString());
				}
				curModel = this.getParser().getSplitModelMapL().get(mName);	
				labelInt = (int) Linear.predict(curModel, fvLabeler.getLiblinearRepresentation(false,true,fm.getAlphabetLabeler()));
				String depRel = fm.getAlphabetLabeler().getIndexLabelArray()[labelInt];
				sentArray[curState.getStackToken(0)-1][7] = depRel;
				sentArray[curState.getStackToken(0)-1][9] = depRel;
				curDepStruct.addDependency(new Dependency(curState.getStackToken(0),curState.getBufferToken(0),depRel));				
				fvLabeler.setLabel(depRel);
				stack.pop();
				fm.getAlphabetLabeler().addLabel(depRel);
			}
			else if (label.equals("i")) {				
				int head = curState.getStackToken(0);
			//	if (head != 0 || (head == 0 &&  sent.getRootPosition() == -1)) {
					sentArray[curState.getBufferToken(0)-1][6] = String.valueOf(head);
					mName = "";
					if (splitMap.get(fvLabeler.getFeature("pj").getFeatureString()) == null) {
						List<String> mNames = new ArrayList<String>(splitMapL.values());
						mName = mNames.get(0);
					}
					else {
						mName = splitMapL.get(fvParser.getFeature("pj").getFeatureString());
					}
					curModel = this.getParser().getSplitModelMapL().get(mName);	
					labelInt = (int) Linear.predict(curModel, fvLabeler.getLiblinearRepresentation(false,true,fm.getAlphabetLabeler()));
					String depRel = fm.getAlphabetLabeler().getIndexLabelArray()[labelInt];
					sentArray[curState.getBufferToken(0)-1][7] = depRel;
					sentArray[curState.getBufferToken(0)-1][9] = depRel;
					curDepStruct.addDependency(new Dependency(curState.getBufferToken(0),curState.getStackToken(0),depRel));
					fvLabeler.setLabel(depRel);
					int stackTop = stack.peek();
					int bufferTop = curState.getBufferToken(0);
				//	buffer.remove(0);
				/*	if (stackTop != 0) {
					o	buffer.add(0,stack.pop());
					}
					else {
						stack.push(bufferTop);
						buffer.remove(0);
					}		*/
					stack.push(buffer.remove(0));
					if (head == 0) {
						sent.setRootPosition(curState.getBufferToken(0));
					}
		//		}
			/*	else {
						label = "shift";
						stack.push(buffer.remove(0));
				}*/
			}
			else if (label.equals("reduce")) {
				if (sentArray[curState.getStackToken(0)-1][6].equals("_")) {
					label = "shift";
					stack.push(buffer.remove(0));
				}
				else if (stack.peek() != 0) {
					stack.pop();
				}
			}
			else {
				if (!buffer.isEmpty()) {					
					stack.push(buffer.remove(0));
				}
			}
			if (buffer.isEmpty()) {
				curState.setTerminal(true);
			}
		}
		postprocess(sentArray, sent);
	}

	@Override
	public String findOutCorrectLabel(int j, int i, String[][] sentArray) {
		String label = "";
		if (j < 0 || i < 0) {
			label =  "shift";
		}
		else if (j!=0 && Integer.valueOf(sentArray[j-1][6]) == i) {
			label = "j";
			return label;
		}	
		else if (i == 0) {
			label = "shift";
			return label;
		}
		else if (Integer.valueOf(sentArray[i-1][6]) == j) {
			Set<Integer> dependents = goldDepStruct.getDependents().get(i);		
		//	System.out.println("DEP   "+i+"  "+dependents);
			if (dependents == null || dependents.isEmpty()) {
				label = "i";
				return label;
			}
			else {
				Iterator<Integer> depIter = dependents.iterator();
				while (depIter.hasNext()) {
					Integer curDep = depIter.next();
					if (curDep > maxi) {
						label = "shift";
						return label;
					}
				}
				label = "i";
				return label;
			}
		}
		else {
			label = "shift";
		}
	/*	if (label.equals("shift")) {
			boolean terminate = true;
			if (Integer.valueOf(sentArray[j-1][6]) < i) {
				terminate = false;
			}
			for (int k=i; k > 1;k--) {
				if (Integer.valueOf(sentArray[k-1][6]) == j) {
					terminate = false;
				}
			}
			if (terminate) {
				label = "terminate";
			}
		}*/
		return label;
	}
	
	public String findOutCorrectLabel2(Stack<Integer> stack, int i, String[][] sentArray) {
		int j = stack.get(stack.size()-1);
		String label = "";
		if (j!=0 && Integer.valueOf(sentArray[j-1][6]) == i) {
			return "j";			
		}
		else if (Integer.valueOf(sentArray[i-1][6]) == j) {
			return "i";			
		}
		else if (j!=0 && (sentArray[j-1][6] != null) ) {
			Integer parent = Integer.valueOf(sentArray[i-1][6]);
			Set<Integer> dependents = goldDepStruct.getDependents().get(i);
			if (dependents != null) {
				Iterator<Integer> depIter = dependents.iterator();
				while (depIter.hasNext()) {
					Integer curDep = depIter.next();
					if (stack.contains(curDep)) {
						return "reduce";
					}
				}
			}
			if (stack.contains(parent)) {
				return "reduce";
			}
			else {
				return "shift";
			}
		/*	Set<Integer> dependents = goldDepStruct.getDependents().get(j);	
			if (dependents == null) {
				return "reduce";
			}
			Iterator<Integer> depIter = dependents.iterator();
			while (depIter.hasNext()) {
				Integer curDep = depIter.next();
				if (curDep > maxi) {
					return "shift";
				}
			}
			return "reduce";*/
		}
		else {
			return "shift";
		}
	}

	public String findOutCorrectLabel2Combined(Stack<Integer> stack, int i, String[][] sentArray) {
		int j = stack.get(stack.size()-1);
		String label = "";
		if (j!=0 && Integer.valueOf(sentArray[j-1][6]) == i) {
			label = "j";	
			label += "#"+sentArray[j-1][7];
			return label;
		}
		else if (Integer.valueOf(sentArray[i-1][6]) == j) {
			label =  "i";
			label += "#"+sentArray[i-1][7];
			return label;
		}
		else if (j!=0 && (sentArray[j-1][6] != null) ) {
			Integer parent = Integer.valueOf(sentArray[i-1][6]);
			Set<Integer> dependents = goldDepStruct.getDependents().get(i);
			if (dependents != null) {
				Iterator<Integer> depIter = dependents.iterator();
				while (depIter.hasNext()) {
					Integer curDep = depIter.next();
					if (stack.contains(curDep)) {
						return "reduce";
					}
				}
			}
			if (stack.contains(parent)) {
				return "reduce";
			}
			else {
				return "shift";
			}
		/*	Set<Integer> dependents = goldDepStruct.getDependents().get(j);	
			if (dependents == null) {
				return "reduce";
			}
			Iterator<Integer> depIter = dependents.iterator();
			while (depIter.hasNext()) {
				Integer curDep = depIter.next();
				if (curDep > maxi) {
					return "shift";
				}
			}
			return "reduce";*/
		}
		else {
			return "shift";
		}

	}
	
	@Override
	public List<FeatureVector> processCombined(Sentence sentence,	FeatureModel fm, boolean noLabels) {
		List<FeatureVector> fvList = new ArrayList<FeatureVector>();
		String[][] sentArray = sentence.getSentArray();		
		Stack<Integer> buffer = initBuffer(sentArray.length);
		Stack<Integer> stack = new Stack<Integer>();
		stack.add(0);
		DependencyStructure curDepStruct = new DependencyStructure(sentArray.length);
		initGoldDepStruct(sentArray);
		StackParserState curState = new StackParserState(stack,buffer,sentence,curDepStruct);
	//	fm.initializeStaticFeatures(sentence,true);
		while (!curState.isTerminal()) {
			FeatureVector fv = fm.applyCombined(curState, true, noLabels);	
			System.out.print(curState.getStackToken(0)+" "+curState.getBufferToken(0)+" "+maxi+" "+stack+" "+buffer+" ");
	//o		String label = findOutCorrectLabel(curState.getStackToken(0), curState.getBufferToken(0), sentArray);
			String label = findOutCorrectLabel2Combined(curState.getStack(), curState.getBufferToken(0), sentArray);
			System.out.println(label+" "+curDepStruct.getDependencies()+" "+curDepStruct.getDependencies().size());
			fv.setLabel(label);
			String labelTrans = "";
			if (label.contains("#")) {
				labelTrans = label.split("#")[0];
			}
			if (labelTrans.equals("j")) {
				sentArray[curState.getStackToken(0)-1][8] = String.valueOf(curState.getBufferToken(0));
				String depRel = sentArray[curState.getStackToken(0)-1][7];
				sentArray[curState.getStackToken(0)-1][9] = depRel;
				curDepStruct.addDependency(new Dependency(curState.getStackToken(0),curState.getBufferToken(0),depRel));				

				stack.pop();
		//		fm.getAlphabetLabeler().addLabel(depRel);
			}
			else if (labelTrans.equals("i")) {
				sentArray[curState.getBufferToken(0)-1][8] = String.valueOf(curState.getStackToken(0));
				String depRel = sentArray[curState.getBufferToken(0)-1][7];
				sentArray[curState.getBufferToken(0)-1][9] = depRel;
				curDepStruct.addDependency(new Dependency(curState.getBufferToken(0),curState.getStackToken(0),depRel));
				int stackTop = stack.peek();
				int bufferTop = curState.getBufferToken(0);
		//o		buffer.remove(0);
		/*o		if (stackTop != 0) {
					buffer.add(0,stack.pop());
					buffer.remove(0);
				}
				else {
					stack.push(bufferTop);
				}*/
				stack.push(buffer.remove(0));
		//		fm.getAlphabetLabeler().addLabel(depRel);
				
				
			}
			else if (label.equals("reduce")) {
				stack.pop();
			}
			else {
				if (!buffer.isEmpty()) {					
					stack.push(buffer.remove(0));
				}
			}
			if (buffer.isEmpty()) {
				curState.setTerminal(true);
			}
			fm.getAlphabetParser().addLabel(label);
			fvList.add(fv);
			maxi = Math.max(curState.getBufferToken(0),maxi);
		}
		return fvList;
	}

	@Override
	public void processCombined(Sentence sent, FeatureModel fm, boolean noLabels, HashMap<String, String> splitMap) {
		String[][] sentArray = sent.getSentArray();		
		Stack<Integer> buffer = initBuffer(sentArray.length);
		Stack<Integer> stack = new Stack<Integer>();
		stack.add(0);
		DependencyStructure curDepStruct = new DependencyStructure(sentArray.length);
		StackParserState curState = new StackParserState(stack,buffer,sent,curDepStruct);
	//	fm.initializeStaticFeatures(sent,false);
		while (!curState.isTerminal()) {
			super.plus();
			FeatureVector fvParser = fm.applyCombined(curState, true, noLabels);
	//		System.out.print(sent.getRootPosition()+" "+curState.getStackToken(0)+" "+curState.getBufferToken(0)+" "+stack+" "+buffer+" ");
			String mName = "";
			if (splitMap.get(fvParser.getFeature("pj").getFeatureString()) == null) {
				List<String> mNames = new ArrayList<String>(splitMap.values());
				mName = mNames.get(0);
			}
			else {
				mName = splitMap.get(fvParser.getFeature("pj").getFeatureString());
			}
			Model curModel = this.getParser().getSplitModelMap().get(mName);	
			int labelInt = (int) Linear.predict(curModel, fvParser.getLiblinearRepresentation(false,false,fm.getAlphabetParser()));
			String label = fm.getAlphabetParser().getIndexLabelArray()[labelInt];
	//		System.out.println(label+" "+curDepStruct.getDependencies()+" "+curDepStruct.getDependencies().size());
			String labelTrans = "";
			String labelDepRel = "";
			if (label.contains("#")) {
				labelTrans = label.split("#")[0];
				labelDepRel = label.split("#")[1];
			}
			fvParser.setLabel(label);
			if (labelTrans.equals("j") && 0 != curState.getStackToken(0)) {
				sentArray[curState.getStackToken(0)-1][6] = String.valueOf(curState.getBufferToken(0));
				String depRel = labelDepRel;
				sentArray[curState.getStackToken(0)-1][7] = depRel;
				sentArray[curState.getStackToken(0)-1][9] = depRel;
				curDepStruct.addDependency(new Dependency(curState.getStackToken(0),curState.getBufferToken(0),depRel));				
				stack.pop();
			}
			else if (labelTrans.equals("i")) {				
				int head = curState.getStackToken(0);
			//	if (head != 0 || (head == 0 &&  sent.getRootPosition() == -1)) {
					sentArray[curState.getBufferToken(0)-1][6] = String.valueOf(head);
					String depRel = labelDepRel;
					sentArray[curState.getBufferToken(0)-1][7] = depRel;
					sentArray[curState.getBufferToken(0)-1][9] = depRel;
					curDepStruct.addDependency(new Dependency(curState.getBufferToken(0),curState.getStackToken(0),depRel));
					int stackTop = stack.peek();
					int bufferTop = curState.getBufferToken(0);
				//	buffer.remove(0);
				/*	if (stackTop != 0) {
					o	buffer.add(0,stack.pop());
					}
					else {
						stack.push(bufferTop);
						buffer.remove(0);
					}		*/
					stack.push(buffer.remove(0));
					if (head == 0) {
						sent.setRootPosition(curState.getBufferToken(0));
					}
		//		}
			/*	else {
						label = "shift";
						stack.push(buffer.remove(0));
				}*/
			}
			else if (label.equals("reduce")) {
				if (sentArray[curState.getStackToken(0)-1][6] == null || sentArray[curState.getStackToken(0)-1][6].equals("_")) {
					label = "shift";
					stack.push(buffer.remove(0));
				}
				else if (stack.peek() != 0) {
					stack.pop();
				}
			}
			else {
				if (!buffer.isEmpty()) {					
					stack.push(buffer.remove(0));
				}
			}
			if (buffer.isEmpty()) {
				curState.setTerminal(true);
			}
		}
		postprocess(sentArray, sent);
	}
	

}
