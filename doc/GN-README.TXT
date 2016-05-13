Comments bei Günter für MDP: March, 2012:

MDP code on  Unix:


SVN: http://subversion.dfki.de/typir/MDP

NEWS: it is actually not necessary to run server and client; 
	instead using MDParser.java and proper settings in file props.xml

Creating self contained jar files file, a Server and Client jar file using build.xml
    
a. with ANT from shell: THIS SEEMS TO WORK BEST
	- go to MDP directory
	- calling:
		ant -f build.xml make-server|make-client|make-mdp
	- create jar files in build/jar/
	
b. Copy jar files to MDP toplevel
	- then resources/... can directly be used
	
c. Clean build 
	ant -f build.xml clean
	
Usage of MDP jar files:

1. Goto toplevel folder where resources is a direct subfolder
	cd /Users/gune00/dfki/MDPFrameWork/MDPTest

2. Call  MDP exec jar file:

java -Xmx1g -jar mdp.jar resources/props/props.xml 


Using Server and Client

3.	Starting of server:

cd dfki/MDPFramework/MDPTest/
java -Xmx1000MB -jar MDPserver.jar propsEnglishServer.xml


4.	Starting of client:

cd dfki/workspace/MDP/
java -Xmx1g -jar MDPclient.jar propsEnglishClient.xml


May, 2013, TODO:

- create own SVN repository -> DONE
	http://trac.dfki.de/typir/browser/MDP/MDP
	
- Testing MDP for NER/REE aso:
  - copied mdp.jar and resources to /Users/gune00/dfki/MDPFrameWork/MDPTest
  - define resources/props/mdpNereid.xml with pointers to dfki/NEREID/NereidTests/resources/..
  
  - then run:
  	java -Xmx1g -jar mdp.jar resources/props/mdpNereid.xml