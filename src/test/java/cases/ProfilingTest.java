package cases;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import de.bwaldvogel.liblinear.*;
import de.dfki.lt.mdparser.archive.Archivator;
import de.dfki.lt.mdparser.data.Data;
import de.dfki.lt.mdparser.features.Alphabet;
import de.dfki.lt.mdparser.parser.Parser;

public class ProfilingTest {
	public static void main(String[] args) throws IOException {
		String goldFile = "input/ann-2.conll";
		long time1 = System.currentTimeMillis();
		load1();
		long time2 = System.currentTimeMillis();
		load2();
		long time3 = System.currentTimeMillis();
		System.out.println((time2-time1)+" "+(time3-time2));
	//	parser.readSplitModelsL(arch);
	}

	private static void load1() throws IOException {
		ZipFile zip = new ZipFile("m2.zip");
		ZipInputStream zis = new ZipInputStream(new FileInputStream("m2.zip"));
		ZipEntry entry;
		while ((entry = zis.getNextEntry())!= null ) {
			String outputFile = entry.getName();
			FileOutputStream out = new FileOutputStream(outputFile);
			OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
			BufferedWriter fw = new BufferedWriter(or);
			InputStreamReader ir = new InputStreamReader(zip.getInputStream(entry),"UTF8");
			BufferedReader fr = new BufferedReader(ir);
			String line;
			while ((line = fr.readLine())!= null) {
				fw.append(line+"\n");
			}
			fw.close();
			Model modelParser = Linear.loadModel(new File(outputFile));	
			File f = new File(outputFile);
			f.delete();	
		}
	}
	
	public static void load2() throws IOException {
		ZipFile zip = new ZipFile("m2.zip");
		ZipInputStream zis = new ZipInputStream(new FileInputStream("m2.zip"));
		ZipEntry entry;
		while ((entry = zis.getNextEntry())!= null ) {
			String outputFile = entry.getName();
			Model modelParser2 = Linear.loadModel(new InputStreamReader(zip.getInputStream(entry)));
		}

	}
}
