package de.dfki.lt.mdparser.algorithm;

import de.dfki.lt.mdparser.data.Sentence;

public class CovingtonParserState extends ParserState {

  private int j;
  private int i;
  private Sentence curSent;
  private boolean isPermissible;
  private Dependency curDep1;
  private Dependency curDep2;
  private DependencyStructure curDepStruct;
  private boolean l1Permissible;
  private boolean l2Permissible;


  public CovingtonParserState(int j, int i, Sentence curSent, DependencyStructure curDepStruct) {
    this.j = j;
    this.i = i;
    this.curSent = curSent;
    this.curDepStruct = curDepStruct;
    this.curDep1 = new Dependency((j), (i));
    this.curDep2 = new Dependency((i), (j));
  }


  public CovingtonParserState(int j, int i, Sentence sent) {
    this.j = j;
    this.i = i;
    this.curSent = sent;
  }


  public void checkPermissibility() {

    this.l1Permissible = this.curDep1.isPermissible(this.curDepStruct, true);
    this.l2Permissible = this.curDep2.isPermissible(this.curDepStruct, true);
    if (this.l1Permissible || this.l2Permissible) {
      this.isPermissible = true;
    } else {
      this.isPermissible = false;
    }

  }


  public void setJ(int j) {

    this.j = j;
  }


  public int getJ() {

    return this.j;
  }


  public void setI(int i) {

    this.i = i;
  }


  public int getI() {

    return this.i;
  }


  public void setCurSent(Sentence curSent) {

    this.curSent = curSent;
  }


  public Sentence getCurSent() {

    return this.curSent;
  }


  public void setPermissible(boolean isPermissible) {

    this.isPermissible = isPermissible;
  }


  public boolean isPermissible() {

    return this.isPermissible;
  }


  public void setCurDepStruct(DependencyStructure curDepStruct) {

    this.curDepStruct = curDepStruct;
  }


  public DependencyStructure getCurDepStruct() {

    return this.curDepStruct;
  }


  public void setCurDep1(Dependency curDep1) {

    this.curDep1 = curDep1;
  }


  public Dependency getCurDep1() {

    return this.curDep1;
  }


  public void setCurDep2(Dependency curDep2) {

    this.curDep2 = curDep2;
  }


  public Dependency getCurDep2() {

    return this.curDep2;
  }


  public void setL1Permissible(boolean l1Permissible) {

    this.l1Permissible = l1Permissible;
  }


  public boolean isL1Permissible() {

    return this.l1Permissible;
  }


  public void setL2Permissible(boolean l2Permissible) {

    this.l2Permissible = l2Permissible;
  }


  public boolean isL2Permissible() {

    return this.l2Permissible;
  }

}
