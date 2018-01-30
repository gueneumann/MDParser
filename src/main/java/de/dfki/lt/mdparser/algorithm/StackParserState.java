package de.dfki.lt.mdparser.algorithm;

import java.util.Stack;

import de.dfki.lt.mdparser.data.Sentence;

public class StackParserState extends ParserState {

  private Stack<Integer> buffer;
  private Stack<Integer> stack;
  private DependencyStructure curDepStruct;
  private boolean terminal;
  private Sentence sent;


  public StackParserState(
      Stack<Integer> stack, Stack<Integer> buffer, Sentence sent,
      DependencyStructure curDepStruct) {

    this.buffer = buffer;
    this.stack = stack;
    this.curDepStruct = curDepStruct;
    this.terminal = false;
    this.sent = sent;
  }


  public Stack<Integer> getStack() {

    return this.stack;
  }


  public DependencyStructure getCurDepStruct() {

    return this.curDepStruct;
  }


  public void setTerminal(boolean terminal) {

    this.terminal = terminal;
  }


  public boolean isTerminal() {

    return this.terminal;
  }


  public Sentence getSent() {

    return this.sent;
  }


  public int getStackToken(int i) {

    if (this.stack.size() <= i) {
      return -1;
    } else {
      return this.stack.get(this.stack.size() - 1 - i);
    }
  }


  public int getBufferToken(int i) {

    if (this.buffer.size() <= i) {
      return -1;
    } else {
      return this.buffer.get(i);
    }
  }
}
