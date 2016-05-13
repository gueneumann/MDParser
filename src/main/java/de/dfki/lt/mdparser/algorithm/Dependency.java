package de.dfki.lt.mdparser.algorithm;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

public class Dependency {

	private int dependent;
	private int head;
	
	private int j;
	private int i;
	
	private int startIndex;
	
	private String label;
	
	private double probability;
	private String dependentString;
	private String headString;
	
	
	public Dependency(int dependent, int head) {
		this.dependent = dependent;
		this.head = head;
	}
	
	public Dependency(int dependent, int head, String label) {
		this.dependent = dependent;
		this.head = head;
		this.label = label;
	}
	
	public int getJ() {
		return this.j;
	}
	
	public void setJ(int j) {
		this.j = j;
	}
	
	public int getI() {
		return this.i;
	}
	
	public void setI(int i) {
		this.i = i;
	}
	
	public int getStartIndex() {
		return this.startIndex;
	}
	
	public void setStartIndex(int si) {
		this.startIndex = si;
	}
	
	public int getDependent() {
		return this.dependent;
	}
	
	public int getHead() {
		return this.head;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public int getDistance() {
		if (dependent > head) 
			return dependent - head;
		else 
			return head - dependent;
	}
	
	public void setProbability(double prob) {
		this.probability = prob;
	}
	
	public double getProbability() {
		return this.probability;
	}
	
	public void setDependentString(String dependentString) {
		this.dependentString = dependentString;
	}

	public String getDependentString() {
		return dependentString;
	}

	public String getDependentWord() {
		int splitPoint = dependentString.lastIndexOf(":");
		return dependentString.substring(0, splitPoint);
	}
	
	public String getDependentPos() {
		int splitPoint = dependentString.lastIndexOf(":");
		return dependentString.substring(splitPoint+1,dependentString.length());
	}
	
	public String getHeadWord() {
		int splitPoint = headString.lastIndexOf(":");
		return headString.substring(0, splitPoint);
	}
	
	public String getHeadPos() {
		int splitPoint = headString.lastIndexOf(":");
		return headString.substring(splitPoint+1,headString.length());
	}
	
	public void setHeadString(String headString) {
		this.headString = headString;
	}
	
	public String getHeadString() {
		return headString;
	}
	
	public boolean isProjective(int[] heads) {
		if (this.getDistance() == 1) {
			return true;
		}
		else {
			int smaller = this.head;
			int bigger = this.dependent;
			if (this.head > this.dependent) {
				smaller = this.dependent;
				bigger = this.head;
			}
			for (int i=bigger-1; i > 0; i--) {
				if (i != smaller) {
					if (i > smaller && heads[i] < smaller) {
						return false;
					}
					else if (i < smaller && heads[i] > smaller) {
						return false;
					}
				}
			}
			
		}
		return true;
	}
	
	public boolean isSingleHead(int[] heads) {
		if (heads[this.dependent] != -1) {
			return false;
		}
		return true;
	}
	
	public boolean isNotReflexive() {
		return this.dependent != this.head;
	}
	
	
	public boolean notIntroducingCycle(int[] heads) {
		int curHead = this.getHead();
		boolean[] possibleEnds = new boolean[heads.length+1];
		possibleEnds[this.dependent] = true;
		Stack<Integer> toCheck = new Stack<Integer>();
		toCheck.add(curHead);
		while (!toCheck.isEmpty()) {
			curHead = toCheck.pop();
			int curHeadHead = heads[curHead];
			if (curHeadHead < 1) {
				return true;
			}
			if (possibleEnds[curHeadHead]) {
				return false;
			}
			else {				
				toCheck.add(curHeadHead);
				possibleEnds[curHead] = true;
			
			}
		}
		return true;
	}
	
	public boolean isNotImproperRoot() {
		if (this.dependent == 0) 
			return false;
		return true;
	}
	
	public boolean isPermissible(DependencyStructure depStruct, boolean projective) {
	/*	DependencyStructure modifiedDepStruct = new DependencyStructure(depStruct.getSize()+1);
		for (Iterator<Dependency> depIter = depStruct.getDependencies().iterator(); depIter.hasNext();) {
			Dependency curDependency = depIter.next();
			modifiedDepStruct.addDependency(curDependency);
		}
		modifiedDepStruct.addDependency(this);*/
	//	System.out.println("single head: "+this.isSingleHead(depStruct)+" not reflexive: "+this.isNotReflexive()+" not introducing cycle: " +
	//			notIntroducingCycle(depStruct)+ " is not improper root "+this.isNotImproperRoot() +" is projective "+this.isProjective(depStruct));
		int[] heads = depStruct.getHeads();
		if (projective)
			return 
			this.isSingleHead(heads) 
			&& this.isNotReflexive() 
			&& this.notIntroducingCycle(heads) 
			&& this.isNotImproperRoot() 
			&& this.isProjective(heads)
			;
		return this.isSingleHead(heads) && this.isNotReflexive() && this.notIntroducingCycle(heads) && this.isNotImproperRoot();
	}
	
	
	public String toString() {
		return "("+this.getDependent()+","+this.label+","+this.getHead()+")";
	}
}
