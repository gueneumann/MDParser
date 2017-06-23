package de.dfki.lt.mdparser.algorithm;

import de.dfki.lt.mdparser.data.Sentence;

@SuppressWarnings("checkstyle:MemberName")
public class CovingtonParserState extends ParserState {

  private int j;
  private int i;
  private Sentence curSent;
  private Dependency curDep1;
  private Dependency curDep2;
  private DependencyStructure curDepStruct;


  public CovingtonParserState(int j, int i, Sentence curSent, DependencyStructure curDepStruct) {

    this.j = j;
    this.i = i;
    this.curSent = curSent;
    this.curDepStruct = curDepStruct;
    this.curDep1 = new Dependency((j), (i));
    this.curDep2 = new Dependency((i), (j));
  }


  public int getJ() {

    return this.j;
  }


  public int getI() {

    return this.i;
  }


  public Sentence getCurSent() {

    return this.curSent;
  }


  public DependencyStructure getCurDepStruct() {

    return this.curDepStruct;
  }


  public boolean isPermissible() {

    return isL1Permissible() || isL2Permissible();
  }


  public boolean isL1Permissible() {

    return this.curDep1.isPermissible(this.curDepStruct, true);
  }


  public boolean isL2Permissible() {

    return this.curDep2.isPermissible(this.curDepStruct, true);
  }
}
