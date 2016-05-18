GN, May, 2016

Migration to Maven and Git:

Copy local jar files to repository:
cd MDParser/lib

Mac/unix
mvn3 deploy:deploy-file  -Durl=file:///Users/gune00/.m2/repository/ -Dfile=pariterator-0.9.2.jar -DgroupId=pi.parallel -DartifactId=par.iterator -Dpackaging=jar -Dversion=0.9.2

Windows
mvn deploy:deploy-file  -Durl=file:C:\Users\GN\.m2\repository\ -Dfile=pariterator-0.9.2.jar -DgroupId=pi.parallel -DartifactId=par.iterator -Dpackaging=jar -Dversion=0.9.2


Changes in MDParser code

- use new liblinear library
- add (int) cast to (int) Linear.predict()
- prob.y = new int[prob.l]; -> prob.y = new double[prob.l];

STATUS:

- seems to work
- first test CompleteTest.java works
- also with newest liblinear 