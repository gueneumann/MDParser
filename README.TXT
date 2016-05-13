READ Me file for installing and running MDParser

Author: Günter Neumann, date: 12/02/2013

Contact:
Prof. Dr. Günter Neumann, Principal Researcher & Research Fellow
LT-Lab, DFKI GmbH Campus D3 2, Stuhlsatzenhausweg 3, 66123 Saarbrücken, Germany
neumann@dfki.de                       Phone:  +49 681 85775 5298
http://www.dfki.de/~neumann           Fax  :  +49 681 85775 5338

NOTES
______

UPDATES: May, 2016

Note: the below is outdated. The repository only contains the source code.
No data for training and testing are exploited!

I also created a Maven pom file and updated MDParser to the newest java version for liblinear



Description:
____________

MDParser stands for multilingual dependency parser and is a data-driven system, which can be used to parse text of an arbitrary 
language for which training data is available. MDParser has been developed at LT-Lab of DFKI as part of the Alexander Volokh and by Günter Neumann.
The parser is able to create both unlabeled and labeled dependency structures. The number of possible relation types depends on 
the granularity of the training data.

The models of the system are based on various features, which are extracted from the words of the sentence, 
including word forms and part of speech tags. Therefore in order to process previously unannotated text MDParser 
additionally includes some preprocessing components:

-	a sentence splitter, since the parser constructs a dependency structure for individual sentences

-	a tokenizer, in order to recognize the elements between the dependency relations will be built

-	a part of speech tagger, in order to determine the part of speech tags, which are one of the most important influencing factors for constructing the dependency structure. 


For sentence splitting and tokenization we are using the morphadorner package, 
cf. http://morphadorner.northwestern.edu/.
For Machine learning we are using the liblinear package, cf. http://www.csie.ntu.edu.tw/~cjlin/liblinear/.

Models for English POS-tagging and dependency parsing are included as part of the resources.zip file. 
They are based on Penn treebank.
Note: These models can be used for research purposes provided that you have a license for the treebank 
on which the model was trained. If you want to use them for commercial applications, 
please contact the license holder for the relevant treebank to find out which conditions apply.

About the MDP-package ZIP file
________________________________

The MDParser source code and its resources folder are collected in a ZIP file called MDP-package.
After uncompressing MDP-package.zip, a folder exists named MDP-package which contains:
- This README.txt
- a zip file named MDP-1.0.zip
- a zip file named resources.zip

Initial Installation Step:
___________________________


Unzip the two zip files which will create two sub-folders named MDP-1.0 and resources

MOVE the resources folder to the MDP-1.0 folder:

mv resources MDP-1.0/.


Structure of the zip file MDP-1.0
_________________________________

Within the sub-folder MDP-1.0 you will see a number of sub-directories. 
The most important ones are:
src/ - contains the source code; 
in src/de/dfki/lt/mdparser/test a number of java classes are 
defined which show how to use the MDParser as a library. 
NOTE: no warranty of the code and its proper working or usage. 
They are just there to serve as examples.

build/ - will contain the classes and jar files created via ant (see below)

(NOTE: the below folder resources/ basically contains the content of the resources.zip file, 
and it is assumed that the initial installation step has been performed already!)

resources/ - contains subfolders for keeping all major input/output files, property files and the models
	resources/input - the input files for parsing
	resources/output - will store the output file4s
	resources/parser/ - keeps the language models for the dependency parser
	resources/tagger/ - keeps the language models for the POS tagger
	resources/props/ - keeps the property files which are used to configure the MDParser; 
		within a property file, direct access to this folder is made;

Installation:
_____________

We assume that the Apache ant build tool is installed on your computer, cf. http://ant.apache.org/
The version I am tested with is Apache Ant(TM) version 1.9.2, but earlier versions should work also fine.

Then:

1. Change to folder MDP-1.0:
	cd MDP-1.0

2. Create complete self-contained jar files. These versions are possible:
	mdpTrainTest.jar - contains the MDParser as well as a main class for training, testing and evaluation
	mdp.jar - contains a standalone version of MDParser that can run from shell
	MDPserver.jar - contains the MDParser plus a XML-RPC based server
	MDPclient.jar - contains the XML-RPC client code for communicating with MDPserver.jar

    ant -f build.xml make-mdpTrainTest OR
    ant -f build.xml make-mdp	OR
    ant -f build.xml make-server OR
    ant -f build.xml make-client

3. After building the directory MDP-1.0/build/jar will contain the created jar file.
   
   NOTE: each of the above calls implicitly perform an ant clean operation, 
         which among others deletes the files in MDP-1.0/build/jar. For example, if you perform
	 	'ant -f build.xml make-mdp' and then 'ant -f build.xml make-server', 
	 	the jar file mdp.jar created via "ant -f build.xml make-mdp" will be deleted.
	 	Thus, if you want to retain the jar files, copy or move them to some other directory.

4. Actually, we propose to move a file jar to the top-level directory MDP-1.0 to ease running them with the
   default property settings:

   assuming you are still in folder MDP-1.0, then
   
   mv build/jar/mdp.jar .

   which moves the mdp.jar from the directory build/jar/ to the folder named MDP-1.0.
   (similar for the other files).
   
5. For your convience, a pre-build jar file mdpTrainTest.jar is shipped with this distribution.

6. In case you decide to move the jar files to some other place (say myMDPfolder) 
   then make sure a copy of the resources folder is created under myMDPfolder or
   a symbolic link named resources is created, e.g., 
   'cd <myMDPFolder>' and then 'ln -s <pathToMDP-1.0>/resources/ resources'

	
Running
________________________________

IMPORTANT:
   Before running the MDParser (or MDPclient or MDPserver , see below) you must adapt relevant
   parameters as described in the property file: resources/props/props.xml
   This property file contains a list of all useful properties together with some explanations that
   are needed to run the MDParser, e.g., 
   - language specific settings (POS tagger and dependency grammar models)
   - input file and output file
   - input and output formats
   
   Please, consult the properties file for more details.
   
   NOTE: you can define you own property file(s) and use same as parameters for the standalone
   		and client/server settings!
   

Using the standalone exec version:

1. Running the mdp.jar exec file is easy. Either from MDP-1.0 or your own myMDPfolder call

   java -Xmx1g -jar mdp.jar resources/props/props.xml 

   This says: the MDParser runs well with 1GB RAM (not less, but also not much more) for loading the
   language models and for parsing the sentences contained in the input file mentioned in the props.xml
   file. The resulting dependency trees 
   are then stored in the output file as specified in the props.xml file.
   Of course, you are free to define and use your own property file or change properties defined in props.xml.
   
  

Using Server and Client

3. Starting of server:

It is basically the same, but you need to specify additional properties, e.g., host name, port number, see 
propsEnglishServer.xml for examples. It is also assumed the the MDPserver.jar has been 
created as defined above and moved to the MDP-1.0 or to your own myMDPfolder. 
Also the resources folder need to be available!

	java -Xmx1G -jar MDPserver.jar propsEnglishServer.xml
	
	check running port:
	lsof -i -P | grep -i LISTEN


4.  Starting of client:

Then for processing a file (or a directory of files), call the MDPclient.jar in similar 
way from any other computer (a copy of the MDPclient.jar jar file and resources folder 
(without the models files) should be enough)
or from another bash of you current computer.

	java -Xmx1g -jar MDPclient.jar propsEnglishClient.xml


NOTE: You can define and use your own property files and locations of input, output, props, models etc. But then, you
are responsible for maintaining the proper reference within your property files!


5.  Training and testing
	NOTE: currently, this command does NOT need a property file! All settinsg are done via specific parameters as described below.

	Assuming you have created the jar file mdpTrainTest.jar and movied it to the MDParser toplevel directory, then
	training model and testing them is easy ASSUMING you have available corresponding training and testing represented in CONLL
	format.
	
	Running a complete training, testing and evaluation cycle, call:
	
	java -Xmx6g -jar mdpTrainTest.jar -train resources/input/english-train.conll -model resources/parser/testModel.zip -test resources/input/english-devel.conll 
	
	NOTE: for training, more RAM is used, so we are using 6GB (4GB should also work, even less, but then training takes eventually more time) 
	
	NOTE: the order of the parameters is mandatory!
	- train resources/input/english-train.conll -> the training file in CONLL format
	- model resources/parser/testModel.zip -> the name of the resulting model file in compressed form
	- test resources/input/english-devel.conll -> the gold standard test file
	
	further useful runs:
	java -Xmx6g -jar mdpTrainTest.jar -train resources/input/english-train.conll -model resources/parser/testModel.zip
	
	-> only create model file
	
	java -Xmx6g -jar mdpTrainTest.jar -model resources/parser/testModel.zip -test resources/input/english-devel.conll 
	
	-> use existing modelfile to perform an evaluation with test file
	
	
	NOTE: 
		These methods only create the models for the dependency parser. 
		It does not create the models for the POS-tagger!
		Creation of the POS tagger models is not yet supported automatically.	
	
	
	


Requirements:
______________

We have run and tested MDParser under Mac OSX Mavericks (and earlier Mac OSX versions), 
Ubuntu and cygwin on Windows 7.

For running the created jar files, having installed 

java version 7
ant 

should be enough.

Development was done using Eclipse (Juno and Kepler).


License:
________

The license.txt file contained in the MDP-1.0 folder belongs to the source code. It does not automatically hold for the language models, see above.

Reference:
__________

Use the following technical publication for referencing MDParser:

Alexander Volokh and Günter Neumann (2012) Transition-based Dependency Parsing with Efficient Feature Extraction, 
35th German Conference on Artificial Intelligence (KI-2012), Saarbrücken, Germany, September, 2012. 

A copy of the paper can also be fetched from: http://www.dfki.de/~neumann/publications/new-ps/paper26.pdf
