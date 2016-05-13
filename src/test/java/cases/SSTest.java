package cases;


import java.io.File;
import java.io.IOException;

import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.dfki.lt.mdparser.model.ModelEditor;
import de.dfki.lt.mdparser.sentenceSplitter.SSEval;

public class SSTest {
	public static void main(String[] args) throws IOException, InvalidInputDataException {
		SSTrainerTest.main(args);
		ModelEditor me = new ModelEditor(new File("temp/m0.txt"), "temp/ssalpha0.txt",true);
		me.editAlphabetAndModel( "temp/ssalpha.txt","temp/m.txt");
		SSPredictorTest.main(args);
		SSEval.main(args);
	}
}
