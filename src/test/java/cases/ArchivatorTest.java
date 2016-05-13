package cases;

import java.io.IOException;

import de.dfki.lt.archive.Archivator;

public class ArchivatorTest {
	public static void main(String[] args) throws IOException {
		String archiveName = "mdp.zip";
		String[] dirs = {"split","splitA","splitF","splitO","splitC","splitModels","temp"};
		Archivator arch = new Archivator(archiveName,dirs);
		arch.setAlphabetParser("temp/alphaParser.txt");
		arch.setAlphabetLabeler("temp/alphaLabeler.txt");
		arch.setSplitFile("temp/split.txt");
		arch.setSplitLFile("temp/splitL.txt");
		arch.setSplitModelsDir("splitModels");
		arch.setSplitModelsLDir("splitModelsL");
	//	arch.pack();
		arch.extract();
		System.out.println(arch);
	}
}
