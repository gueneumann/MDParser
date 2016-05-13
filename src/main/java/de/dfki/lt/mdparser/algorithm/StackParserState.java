package de.dfki.lt.mdparser.algorithm;

import java.util.Stack;

import de.dfki.lt.mdparser.data.Sentence;

public class StackParserState extends ParserState {

	private Stack<Integer> buffer;
	private Stack<Integer> stack;
	private DependencyStructure curDepStruct;
	private boolean terminal;
	private Sentence sent;
	
	public StackParserState(Stack<Integer> stack, Stack<Integer> buffer, Sentence sent, DependencyStructure curDepStruct) {
		this.buffer = buffer;
		this.stack = stack;
		this.curDepStruct = curDepStruct;
		this.terminal = false;
		this.setSent(sent);
	}
	
	public void setStack(Stack<Integer> stack) {
		this.stack = stack;
	}
	public Stack<Integer> getStack() {
		return stack;
	}
	public void setBuffer(Stack<Integer> buffer) {
		this.buffer = buffer;
	}
	public Stack<Integer> getBuffer() {
		return buffer;
	}
	public void setCurDepStruct(DependencyStructure curDepStruct) {
		this.curDepStruct = curDepStruct;
	}
	public DependencyStructure getCurDepStruct() {
		return curDepStruct;
	}

	public void setTerminal(boolean terminal) {
		this.terminal = terminal;
	}
	
	public boolean isTerminal() {
		return this.terminal;
	}

	public void setSent(Sentence sent) {
		this.sent = sent;
	}

	public Sentence getSent() {
		return sent;
	}

	public int getStackToken(int i) {
		if (stack.size() <= i) {
			return -1;
		}
		else {
			return stack.get(stack.size()-1-i);
		}
	}
	
	public int getBufferToken(int i) {
		if (buffer.size() <= i) {
			return -1;
		}
		else {
			return buffer.get(i);
		}
	}
	
}
