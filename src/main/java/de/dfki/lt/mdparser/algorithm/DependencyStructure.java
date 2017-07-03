package de.dfki.lt.mdparser.algorithm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class DependencyStructure {

  private Dependency[] dependenciesArray;
  private Set<Dependency> dependencies;
  private int[] heads;
  private String[] labels;
  private Map<Integer, Set<Integer>> dependents;
  private int rootPosition;
  private int size;


  public DependencyStructure(int size) {

    this.dependencies = new HashSet<Dependency>(40);
    this.heads = new int[size + 1];
    this.labels = new String[size + 1];
    for (int i = 0; i < this.heads.length; i++) {
      this.heads[i] = -1;
    }
    for (int i = 0; i < this.labels.length; i++) {
      this.labels[i] = "null";
    }
    this.dependents = new HashMap<Integer, Set<Integer>>(size + 1);
    this.size = size + 1;
  }


  public Dependency[] getDependenciesArray() {

    return this.dependenciesArray;
  }


  public int[] getHeads() {

    return this.heads;
  }


  public Map<Integer, Set<Integer>> getDependents() {

    return this.dependents;
  }


  public int getRootPosition() {

    return this.rootPosition;
  }


  public int getSize() {

    return this.size;
  }


  public void constructDependenciesArray() {

    Iterator<Dependency> iter = this.dependencies.iterator();
    Dependency[] deps = new Dependency[this.size];
    while (iter.hasNext()) {
      Dependency curDep = iter.next();
      deps[curDep.getDependent()] = curDep;
    }
    this.dependenciesArray = deps;
  }


  public void addDependency(Dependency dependency) {

    this.dependencies.add(dependency);
    this.heads[dependency.getDependent()] = dependency.getHead();
    this.labels[dependency.getDependent()] = dependency.getLabel();
    Set<Integer> localDependents = this.dependents.get(dependency.getHead());
    if (localDependents == null) {
      localDependents = new HashSet<Integer>();
    }
    localDependents.add(dependency.getDependent());
    this.dependents.put(dependency.getHead(), localDependents);
    if (dependency.getHead() == 0) {
      this.rootPosition = dependency.getDependent();
    }
  }


  public void removeDependency(Dependency dependency) {

    Set<Dependency> modifiedDependencies = new HashSet<Dependency>();
    for (Iterator<Dependency> depIter = this.dependencies.iterator(); depIter.hasNext();) {
      Dependency curDependency = depIter.next();
      if (curDependency.getDependent() != dependency.getDependent()
          || curDependency.getHead() != dependency.getHead()) {
        modifiedDependencies.add(curDependency);
      } else {
        this.dependents.get(dependency.getHead()).remove(dependency.getDependent());
        this.heads[curDependency.getDependent()] = -1;
        this.labels[curDependency.getDependent()] = "null";
      }

    }
    this.dependencies = modifiedDependencies;
  }


  public boolean containsDependency(Dependency dependency) {

    int dependent = dependency.getDependent();
    int head = dependency.getHead();
    for (Iterator<Dependency> depIter = this.dependencies.iterator(); depIter.hasNext();) {
      Dependency curDependency = depIter.next();
      if (curDependency.getHead() == head && curDependency.getDependent() == dependent) {
        return true;
      }
    }
    return false;
  }


  public Set<Integer> getAllDependentsTransitive(Integer token) {

    Stack<Integer> st = new Stack<Integer>();
    st.add(token);
    Set<Integer> allDeps = new HashSet<Integer>();
    while (!st.isEmpty()) {
      Integer curToken = st.pop();
      Set<Integer> curDependents = this.dependents.get(curToken);
      if (curDependents != null && !curDependents.isEmpty()) {
        st.addAll(curDependents);
        allDeps.addAll(curDependents);
      }
    }
    return allDeps;
  }


  public int getClosestLeftSibling(int i) {

    int ls = -1;
    int head = this.heads[i];
    if (head != -1) {
      Set<Integer> deps = this.dependents.get(head);
      if (deps != null) {
        Iterator<Integer> depsIter = deps.iterator();
        while (depsIter.hasNext()) {
          Integer curDep = depsIter.next();
          if (curDep > ls && curDep < i) {
            ls = curDep;
          }
        }
      }
    }
    return ls;
  }


  public int getClosestRightSibling(int i) {

    int rs = Integer.MAX_VALUE;
    int head = this.heads[i];
    if (head != -1) {
      Set<Integer> deps = this.dependents.get(head);
      if (deps != null) {
        Iterator<Integer> depsIter = deps.iterator();
        while (depsIter.hasNext()) {
          Integer curDep = depsIter.next();
          if (curDep < rs && curDep > i) {
            rs = curDep;
          }
        }
      }
    }
    if (rs == Integer.MAX_VALUE) {
      return -1;
    }
    return rs;
  }


  public int getFarthestLeftDependent(int i) {

    int ld = Integer.MAX_VALUE;
    Set<Integer> deps = this.dependents.get(i);
    if (deps != null) {
      Iterator<Integer> depsIter = deps.iterator();
      while (depsIter.hasNext()) {
        Integer curDep = depsIter.next();
        if (curDep < ld && curDep < i) {
          ld = curDep;
        }
      }
      if (ld == Integer.MAX_VALUE) {
        ld = 0;
      }
    }
    if (ld == Integer.MAX_VALUE) {
      ld = -1;
    }
    return ld;
  }


  public int getFarthestRightDependent(int i) {

    int rd = -1;
    Set<Integer> deps = this.dependents.get(i);
    if (deps != null) {
      Iterator<Integer> depsIter = deps.iterator();
      while (depsIter.hasNext()) {
        Integer curDep = depsIter.next();
        if (curDep > rd && curDep > i) {
          rd = curDep;
        }
      }
      if (rd == -1) {
        rd = 0;
      }
    }
    return rd;
  }


  public boolean isProjective() {

    for (Iterator<Dependency> depIter = this.dependencies.iterator(); depIter.hasNext();) {
      Dependency curDependency = depIter.next();
      if (!curDependency.isProjective(this.heads)) {
        return false;
      }
    }
    return true;
  }


  public boolean isSingleHead() {

    boolean[] haveHead = new boolean[this.size];
    for (Iterator<Dependency> depIter = this.dependencies.iterator(); depIter.hasNext();) {
      Dependency curDependency = depIter.next();
      if (haveHead[curDependency.getDependent()]) {
        return false;
      } else {
        haveHead[curDependency.getDependent()] = true;
      }
    }
    return true;
  }


  public boolean isNotReflexive() {

    for (Iterator<Dependency> depIter = this.dependencies.iterator(); depIter.hasNext();) {
      Dependency curDependency = depIter.next();
      if (!curDependency.isNotReflexive()) {
        return false;
      }
    }
    return true;
  }


  public boolean hasProperRoot() {

    return this.heads[0] == -1;
  }


  public boolean isNotCyclic() {

    for (Iterator<Dependency> depIter = this.dependencies.iterator(); depIter.hasNext();) {
      Dependency curDependency = depIter.next();
      boolean[] visited = new boolean[this.size];
      int head = curDependency.getHead();
      while (head != -1) {
        if (visited[head]) {
          return false;
        }
        visited[head] = true;
        head = this.heads[head];
      }
    }
    return true;
  }


  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    for (int i = 1; i < this.heads.length - 1; i++) {
      sb.append("(" + (i) + "," + this.heads[i] + ") ");
    }
    return sb.toString();
  }
}
