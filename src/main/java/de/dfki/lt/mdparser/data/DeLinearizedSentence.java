package de.dfki.lt.mdparser.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// given a flatSentence and linearized dependency tree, create a CONLL output
// Make sure to follow MDParser version so that eval method can be used

/*
* Aim: create a proper CONLL tree from a given pair of aligned sentence and dependency tree
* MDParser conll format uses column 6 and 7 for head and label,
* and 8 and 9 for predicted (have to check) in order to run evaluation script
*    1, 2, and 3 for ID, word and POS, example:
*      ID  word  POS _ _ head  label _ _
*      1 The _ DT  _ _ 2 NMOD  _ _
* a linearized dependency tree is created in left-to-right top-down way
*
* I should first create a specific order based on the tree.
* a problem is that from the sequence of daughters, I do not know where to insert in the head element
* that means: where to cut the daughter sequence.
* Anyway, I could use an initial indexing, by placing the head at the end/front.
* Then use the position of a word in the aligned sentence for adjusting the initial sequence
* I could also search for the head index, by searching the word form of the head in left to right order
* (and also remember, which index has been used - in case a word occurs several times).
* If I do this for all words, I get at least some sort of relative ordering.
* But maybe, I have to take care about possible matches of a head element in the string.
* And it might only work for projective trees.
*
* NOTE: basically this means that I have to define an aligner, but can assume that has an implicit order
* since it was created automatically from a sequence.
*
* (_RT intends (_SBJ bill (_NMOD The )_NMOD )_SBJ (_OPRD to (_IM restrict (_OBJ RTC (_NMOD the )_NMOD )_OBJ
* (_ADV to (_PMOD borrowings (_NMOD Treasury )_NMOD (_NMOD only )_NMOD )_PMOD )_ADV )_IM )_OPRD (_P , )_P
* (_ADV unless (_SUB receives (_SBJ agency (_NMOD the )_NMOD )_SBJ (_OBJ authorization (_NMOD specific )_NMOD
* (_NMOD congressional )_NMOD )_OBJ )_SUB )_ADV (_P . )_P )_RT
*
* The|DT bill|NN intends|VBZ to|TO restrict|VB the|DT RTC|NNP to|TO Treasury|NNP borrowings|NNS only|RB ,|,
* unless|IN the|DT agency|NN receives|VBZ specific|JJ congressional|JJ authorization|NN .|.
*
* Define a Sentence class which has a sentArray.
* Initialize it with sequence of word|pos tokens.
* Also create a parallel hash, which maps a word to its list if indicies.
*
*/
public class DeLinearizedSentence {
  private int infoSize = 10; // Number of CONLL columns -1
  private Sentence conllSentence = null;
  private Map<String, Deque<Integer>> wordIndexPos = new HashMap<String,Deque<Integer>>();
  private Deque<Integer> headIdStack = new ArrayDeque<Integer>();
  private Deque<String> labelStack = new ArrayDeque<String>();
  private boolean typeTokenId = false;
  private String typeTokenIdstring = "#";


  public DeLinearizedSentence(boolean typeTokenId) {
    this.typeTokenId = typeTokenId;
  }

  private List<String> makeSequenceFromString(String string){
    return new ArrayList<String>(Arrays.asList(string.split(" ")));
  }

  /**
   * This is used to define a mapping from a word to its different mentioning positions in a sentence.
   * The order in the stack/deque is from left to right.
   * @param word
   * @param index
   */


  private void adddWordIndextoHash(String word, int index) {
    if (this.wordIndexPos.containsKey(word)) {
      this.wordIndexPos.get(word).addLast(index);
    }
    else {
      Deque<Integer> indexStack = new ArrayDeque<Integer>();
      indexStack.add(index);
      this.wordIndexPos.put(word, indexStack);
    }
  }


  private int getNextIndex(String word) {

    if (!this.wordIndexPos.isEmpty()) {
      Deque<Integer> indexStack = this.wordIndexPos.get(word);
      int nextIndex = indexStack.pop();
      return nextIndex;
    } else {
      // indicates error, because accessing indexStack should not lead to empty stack
      return -1;
    }
  }

  public void printHashMap() {
    for(String key : this.wordIndexPos.keySet()) {
      System.out.println(key + ": " + this.wordIndexPos.get(key));
  }
  }

  /**
   * Receives a wordPosSequence of tokens of form word|POS
   * and creates an initial Conll sentence object with filled
   * columns for index, word, pos.
   * As a side effect, it creates a mapping from word to all possible indexes in the sentence, e.g.,
   * the word "a" can occur several times in a sentence at several positions, so we insert the indices
   * in a stack from left to right.
   */

  private void createInitialConllSentence(List<String> wordPosSequence) {

    String[][] sentArray = new String[wordPosSequence.size()][this.infoSize];
    for (int i = 0; i < wordPosSequence.size(); i++) {
      String[] wordPostoken = wordPosSequence.get(i).split("\\|");

      String word = wordPostoken[0];
      String pos = wordPostoken[1];

      int conllTokenId = i+1;

      sentArray[i][0] = String.valueOf(conllTokenId);
      sentArray[i][1] = word;
      sentArray[i][3] = pos;

      this.adddWordIndextoHash(word, conllTokenId);
    }

    this.conllSentence = new Sentence(sentArray);

  }


  private void undoTypeTokenId() {
    int conllWordIndex = 1;
    for (int i = 0; i < this.conllSentence.getSentArray().length; i++) {
      String wordWithTypeTokenId = this.conllSentence.getSentArray()[i][conllWordIndex];
      String wordWithoutTypeTokenId =
          wordWithTypeTokenId.substring(0,
              wordWithTypeTokenId.lastIndexOf(this.typeTokenIdstring));

      this.conllSentence.getSentArray()[i][conllWordIndex] = wordWithoutTypeTokenId;

    }

  }

  /**
   * Adds headId and label to conll row 6 and 7 index by wordIndex
   * @param wordIndex
   * @param headId
   * @param label
   */
  private void addHeadIdLabeltoWordIndex(int wordIndex, Integer headId, String label) {
    int conllTokenId = wordIndex-1;
    this.conllSentence.getSentArray()[conllTokenId][6] = String.valueOf(headId);
    this.conllSentence.getSentArray()[conllTokenId][7] = label;
  }


  // loop over linearizedSentence and add index and label information to the conllSentence
  /*
  * Initialize labelStack = push()
  * Initialize headIdStack = push(0)
  * nextElem=pop(sequence)
  * if newElem=) then
  *  if newElem=top(labelStack) then pop(labelStack) & pop(headIdStack) break
  * if newElem=( then
  *   push(LABEL, Labelstack) break
  * if newElem = WORD then
  *  - identify word index using hash; pop(foundIndex) from hash
  *  - add headID = top(headIdStack) & label = top(labelStack)
  *  - push(wordID, headIdStack)
  *
  *
  */


  /*
   * BUG (SOLVED):
   * - seems be the case when a token occurs at different positions,  I do not proper compute headId
   * - the problem is that sometime it is correct, but sometimes not
   * - the problem is as follows:
   * - assuming I have a word "Mr." and its two surface positions i and i+k
   * - then it might be that "Mr._i+k" is higher in the dependency tree
   * - than "Mr-_i", and so "Mr._i+k" will be considered first.
   * - However, my algorithm will choose position the first element in the hash table for key "Mr."
   * - which in this case is "i".
   *
   * - hence, it seems that a  strict left-to-right not possible ?
   * - is it possible to use indexes for occurrences of same token, like "Mr._1" and "Mr._2" ?
   * - then I would have a one-to-one mapping of nodes and tokens
   *  - only would need to do it in linearization step and when creating the conll sentence array
   *  -> DONE via flag typeTokenId and it works!!
   *
   */
  private void fillConllSentence(List<String> linearizedSentence) {

    this.headIdStack = new ArrayDeque<Integer>();
    this.labelStack = new ArrayDeque<String>();

    this.headIdStack.push(0);

    for (String elem : linearizedSentence) {
            System.out.println("Elem: " + elem);
//            System.out.println("headIdStack: " + this.headIdStack.toString());
//            System.out.println("labelStack: " + this.labelStack.toString());
      if (elem.startsWith("(_")) {
        String label = elem.substring(2);
        this.labelStack.push(label);
//        System.out.println("Inside (_: " + this.labelStack.toString());
      } else if (elem.startsWith(")_")) {
        String label = elem.substring(2);
        //.out.println("Inside )_: " + label + " =?= " + this.labelStack.getFirst());
        if (label.equals(this.labelStack.getFirst())) {
          this.labelStack.pop();
          this.headIdStack.pop();
        } else {
          //System.out.println("should be equal: " + label + ", " + this.labelStack.getFirst());
        }
      } else // elem is a word
      {
        int wordIndex = this.getNextIndex(elem);
//        System.out.println("Index: " + wordIndex + " Word: " + elem + " First stack: " + this.headIdStack.getFirst());
        this.addHeadIdLabeltoWordIndex(wordIndex, this.headIdStack.getFirst(), this.labelStack.getFirst());
        this.headIdStack.push(wordIndex);

      }
    }

  }

  /**
   * Receives a pair of aligned wordPos-token sequenc and linearized dependency tree
   * and returns a conll dependency tree.
   * @param sequenceString
   * @param linearizedSentenceString
   * @return
   */
  public Sentence deLinearizeSentencePair(String sequenceString, String linearizedSentenceString) {
    List<String> sequence = this.makeSequenceFromString(sequenceString);
    List<String> linearizedSentence = this.makeSequenceFromString(linearizedSentenceString);
    this.createInitialConllSentence(sequence);
    if (!this.typeTokenId) {
      this.printHashMap();
    }
    this.fillConllSentence(linearizedSentence);
    if (this.typeTokenId) {
      this.undoTypeTokenId();
    }
    return this.conllSentence;
  }

}
