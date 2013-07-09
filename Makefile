# pythonpath := $(shell pwd)/src/python

.deps : 
	mvn clean
	mvn install
	mvn install:install-file -Dfile=lib/java-aws-mturk.jar -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 -DartifactId=java-aws-mturk
	mvn install:install-file -Dfile=lib/aws-mturk-dataschema.jar -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 -DartifactId=aws-mturk-dataschema
	mvn install:install-file -Dfile=lib/aws-mturk-wsdl.jar -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 -DartifactId=aws-mturk-wsdl
	echo "" > .deps

.compile : .deps
	mvn scala:compile
	mvn compile
	echo "" > .compile

.PHONY : test_java

test_java : .compile
	mvn compile
	mvn exec:java -Dexec.mainClass=testing.TestSuite
#	mvn exec:java -Dexec.mainClass=csv.CSVParser -Dexec.args="data/linguistics/test3.csv --sep=: data/linguistics/test2.csv --sep=\t data/linguistics/test1.csv --sep=,"	
#	mvn exec:java -Dexec.mainClass=csv.CSVLexer -Dexec.args="data/linguistics/test3.csv --sep=: data/linguistics/test2.csv --sep=\t data/linguistics/test1.csv --sep=,"
#	mvn exec:java -Dexec.mainClass=system.Debugger -Dexec.args="data/linguistics/test3.csv --sep=:"
#	mvn exec:java -Dexec.mainClass=system.mturk.XMLGenerator
#	mvn exec:java -Dexec.mainClass=system.mturk.Slurpie
#	mvn exec:java -Dexec.mainClass=csv.CSVEntry 
#	mvn exec:java -Dexec.mainClass=system.mturk.SurveyPoster

test_python : 
	python $(pythonpath)/example_survey.py
	python $(pythonpath)/metrics/metric-test.py file=data/ss11pwy.csv numq=5 numr=50

simulator : 
	python $(pythonpath)/survey/launcher.py display=False simulation=$(pythonpath)/simulations/simulation.py stop=stop_condition outdir=data responsefn=get_response

.PHONY : clean

clean : 
	rm .deps
	rm .compile
	rm -rf ~/.surveyman/.metadata
	mvn clean

.PHONY : jar

jar : 
	mvn clean
	mvn install
