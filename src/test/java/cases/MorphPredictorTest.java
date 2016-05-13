package cases;


import java.io.IOException;

import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.dfki.lt.archive.Archivator;
import de.dfki.lt.data.Data;
import de.dfki.lt.eval.Eval;
import de.dfki.lt.features.Alphabet;
import de.dfki.lt.parser.Parser;
import de.dfki.lt.pil.MorphTagger;
import de.dfki.lt.pil.MorphTrainer;

public class MorphPredictorTest {
	
	public static void main(String[] args) throws IOException, InvalidInputDataException {
		String goldFile = "PIL/devel/test-htb-ver0.51.auto.utf8.conll";
		Data d = new Data(goldFile,true);
		MorphTagger mt = new MorphTagger();
		mt.test(d, "pers");
		mt.test(d, "cat");
		mt.test(d, "num");
		mt.test(d, "case");
		mt.test(d, "gen");
		mt.test(d, "tam");
		mt.test(d, "vib");
		mt.test(d, "stype");
		mt.test(d, "voicetype");
		mt.test(d, "chunkType");
		mt.test(d, "chunkId");
		d.printToFile("PIL/devel/test-htb-ver0.51.my.utf8.conll");
	}
}
