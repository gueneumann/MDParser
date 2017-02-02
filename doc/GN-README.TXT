
FEB, 2017, NOTES


Migration to Maven and Git:

Copy local jar files to repository:
cd MDParser/lib

Mac/unix
mvn3 deploy:deploy-file  -Durl=file:///Users/gune00/.m2/repository/ -Dfile=pariterator-0.9.2.jar -DgroupId=pi.parallel -DartifactId=par.iterator -Dpackaging=jar -Dversion=0.9.2

Windows
mvn deploy:deploy-file  -Durl=file:C:\Users\GN\.m2\repository\ -Dfile=pariterator-0.9.2.jar -DgroupId=pi.parallel -DartifactId=par.iterator -Dpackaging=jar -Dversion=0.9.2
