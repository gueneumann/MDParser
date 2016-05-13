package cases;


import java.io.File;
import java.io.IOException;

import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.model.ModelEditor;

public class ModelEditorTest {
	public static void main(String[] args) throws IOException {
		String alphabetFile = "temp/alphaParser.txt";
		String oldAlphaFile = "temp/alpha_old.txt";
/*		ModelEditor me = new ModelEditor(modelFile, alphabetFile);
		me.secureOldAlphabet(oldAlphaFile);
		me.printToFile(newModelFile);
		me.removeZeroes(newAlphabetFile, newModelFile);
		File f = new File(modelFile);
		f.delete();
		f = new File(alphabetFile);
		f.delete();
		f = new File(newModelFile);
		f.renameTo(new File(modelFile));
		f = new File(newAlphabetFile);
		f.renameTo(new File(alphabetFile));
		
		modelFile = "temp/modelLabeler.txt";
		alphabetFile = "temp/alphaLabeler.txt";
		newModelFile = "temp/model2.txt";
		newAlphabetFile = "temp/alpha2.txt";
		me = new ModelEditor(modelFile, alphabetFile);
		me.printToFile(newModelFile);
		me.removeZeroes(newAlphabetFile, newModelFile);
		f = new File(modelFile);
		f = new File(modelFile);
		f.delete();
		f = new File(alphabetFile);
		f.delete();
		f = new File(newModelFile);
		f.renameTo(new File(modelFile));
		f = new File(newAlphabetFile);
		f.renameTo(new File(alphabetFile));	*/	
		
		//split version old!
		ModelEditor me = new ModelEditor(new File("splitModels"), alphabetFile);
		me.secureOldAlphabet(oldAlphaFile);
		me.editAlphabetAndModels(alphabetFile);
		
		//split version new (alphabet for each model)
	/*	String modelDir = "splitModels";
		String alphabetDir = "splitA";
		File[] models = new File(modelDir).listFiles();
		for (int i=0; i < models.length;i++) {
			String curModelName = modelDir+"/"+models[i].getName();
			String curAlphabetName = alphabetDir+"/"+models[i].getName();
		//	System.out.println(curModelName+" "+curAlphabetName);
		//	File fOld = new File(curModelName);
		//	System.out.println("old size: "+fOld.length());
			ModelEditor me = new ModelEditor(new File(curModelName), curAlphabetName,true);
			me.editAlphabetAndModel(curAlphabetName,curModelName);
		////	fOld = new File(curModelName);
		//	System.out.println("new size: "+fOld.length());
		}*/

		
		
		
	}
}
