package de.dfki.lt.mdparser.parser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Set;

import de.dfki.lt.mdparser.data.Sentence;
import pi.ParIterator;

public class SplitWorkerThread extends Thread {

	private HashMap<Integer,String> posMap;

	private HashMap<String, BufferedWriter> splitMap;
	private ParIterator<File> pi;
	private int id;

	public SplitWorkerThread(int k, ParIterator<File> iter, 
			HashMap<Integer,String> posMap, 

			HashMap<String, BufferedWriter> splitMap) {
		this.id = k;
		this.pi = iter;
		this.posMap = posMap;
		this.splitMap = splitMap;
	}


	public void run() {
		while (pi.hasNext()) {
			File element = pi.next();
			System.out.println("Hello from Thread in SplitWorkerThread "+id);
			FileInputStream in = null;
			try {
				in = new FileInputStream(element);
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			BufferedInputStream bis = new BufferedInputStream(in, 8000);
			InputStreamReader ir = null;
			try {
				ir = new InputStreamReader(bis,"UTF8");
				
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

			BufferedReader fr = new BufferedReader(ir);
			String line;
			try {
				while ((line = fr.readLine())!= null) {
					String[] lineArray = line.split(" ");
					String splitVal = "";
					Integer splitIndex = 0;
					int fIndex = 1;
					while (splitVal.equals("")) {							
						int index = Integer.valueOf(lineArray[fIndex].split(":")[0]);		
						String pos = posMap.get(index);
						if (pos != null) {
							splitVal = pos;
							splitIndex = index;
						}
						else {
							fIndex++;
						}
					}
					BufferedWriter curBw = splitMap.get(splitVal);
					if (curBw == null) {
						FileOutputStream out = new FileOutputStream("splitF/"+splitIndex+".txt");
						OutputStreamWriter or = new OutputStreamWriter(out,"UTF-8");
						curBw = new BufferedWriter(or);
						splitMap.put(splitVal,curBw);
					}
					curBw.append(line+"\n");
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				fr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("    Thread "+id+" has finished.");
	}

}
