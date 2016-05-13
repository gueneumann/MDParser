package cases;


import java.io.IOException;

import de.bwaldvogel.liblinear.InvalidInputDataException;
import de.dfki.lt.data.Data;
import de.dfki.lt.parser.Trainer;
import de.dfki.lt.pil.MorphTrainer;

public class MorphTrainerTest {

	public static void main(String[] args) throws IOException, InvalidInputDataException {
		

	String inputFile = "PIL/train/train-htb-ver0.51.gold.utf8.conll";
	MorphTrainer mt = new MorphTrainer();
	Data d = new Data(inputFile,true);
	mt.train(d, "pers");
	mt.train(d, "cat");
	mt.train(d, "gen");
	mt.train(d, "case");
	mt.train(d, "num");
	mt.train(d, "vib");
	mt.train(d, "tam");
	mt.train(d, "stype");
	mt.train(d, "voicetype");
	mt.train(d, "chunkType");
	mt.train(d, "chunkId");
	}
	
}
