package cases;


import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import de.bwaldvogel.liblinear.*;
import de.dfki.lt.mdparser.archive.Archivator;

public class CompleteTrainParseEval {

	String trainFile;
	String goldFile;
	String modelFile;

	String[] trainArgs;
	String[] parseArgs;

	String[] dirs = {"split","splitA","splitF","splitO","splitC","splitModels","temp"};

	private static void outputMessage(){
		System.out.println("No arguments specified.");
		System.out.println("Use 'mdpTrainTest.jar -train <trainfile> -model <modelFile> -test <goldFile>'");
		System.out.println("	To train a new model using <trainFile> and save it in <modelFile>, and test it with <goldFile>.");
		System.out.println("	<trainFile> and <goldFile> must specified in CONLL format!");
		System.out.println("	<modelFile> must will be a ZIP file!");
		System.out.println("Use 'mdpTrainTest.jar -train <trainfile> -model <modelFile>");
		System.out.println("	To train a new model using <trainFile> and save it in <modelFile>");
		System.out.println("Use 'mdpTrainTest.jar -model <modelFile> -test <goldFile>'");
		System.out.println("	To load the <modelFile> and test it with <goldFile>.");
	}

	private void setArgValues(String[] args){
		if (args.length == 0)
		{
			CompleteTrainParseEval.outputMessage();
			// Exit with error !
			System.exit(1);
		}
		else
			if (args.length == 4){
				if (args[0].equalsIgnoreCase("-model"))
				{
					this.modelFile = args[1];
				}
				if (args[2].equalsIgnoreCase("-test"))
				{
					this.goldFile = args[3];
				}
				if (args[0].equalsIgnoreCase("-train"))
				{
					this.trainFile = args[1];
				}
				if (args[2].equalsIgnoreCase("-model"))
				{
					this.modelFile = args[3];
				}
			}
			else
				if (args.length == 6){
					if (args[0].equalsIgnoreCase("-train"))
					{
						this.trainFile = args[1];
					}
					if (args[2].equalsIgnoreCase("-model"))
					{
						this.modelFile = args[3];
					}
					if (args[4].equalsIgnoreCase("-test"))
					{
						this.goldFile = args[5];
					}
				}
				else
				{
					System.out.println("Wrong number of arguments specified!");
					CompleteTrainParseEval.outputMessage();
					// Exit with error !
					System.exit(1);
				}
		if (this.trainFile !=null){
			this.trainArgs = new String[1];
			this.trainArgs[0]=this.trainFile;
		}
		this.parseArgs = new String[2];
		this.parseArgs[1]=this.modelFile;
		this.parseArgs[0]=this.goldFile;
	}

	public void runCompleteTrainParseEval() throws IOException, InvalidInputDataException, NoSuchAlgorithmException {


		if (this.trainFile != null){
			System.out.println("**************************************");
			System.out.println("Do training with CONLL training corpus: " + this.trainFile);
			System.out.println("Writing trained model into ZIPed file: " + this.modelFile);
			System.out.println("**************************************");
			Archivator arch = new Archivator(modelFile,dirs);
			long s1 = System.currentTimeMillis();
			TrainerTest.main(trainArgs);
			long s2 = System.currentTimeMillis();

			System.out.println("Complete Training time: "+((s2-s1)) +" milliseconds.");

			//	ModelEditorTest.main(null);		
			arch.pack();
			//	arch.delTemp();
			TrainerTest.deleteOld(dirs);
		}
		if	(this.goldFile != null){
			System.out.println("**************************************");
			System.out.println("Reading trained model from: " + this.modelFile);
			System.out.println("Do testing with testing corpus: " + this.goldFile);
			System.out.println("**************************************");

			long s3 = System.currentTimeMillis();
			ParserTest.main(parseArgs);
			long s4 = System.currentTimeMillis();
			System.out.println("Complete Parsing time: "+((s4-s3))+" milliseconds.");
		}
	}


	public static void main(String[] args) throws IOException, InvalidInputDataException, NoSuchAlgorithmException {
		CompleteTrainParseEval mdpRunner = new CompleteTrainParseEval();
		mdpRunner.setArgValues(args);
		mdpRunner.runCompleteTrainParseEval();
	}
}
