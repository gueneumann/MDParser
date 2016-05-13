package cases;


public class JavaTest {
	public static void main(String[] args) throws Exception {
		PosTagger posTagger = new PosTagger("english");
		System.out.println(posTagger.tagSentence("Here is an example.", "text"));
	}
}
