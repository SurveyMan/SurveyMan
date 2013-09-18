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

.PHONY : test

test : .compile
	mvn compile
	mvn test

test_travis : .compile
	mvn compile -DskipTests
	mvn -Ptravis test

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
	unzip lib/aws-mturk-clt.jar 
	unzip lib/aws-mturk-dataschema.jar  
	unzip lib/aws-mturk-wsdl.jar  
	unzip lib/java-aws-mturk.jar
	jar uf surveyman.jar com/*
	git checkout -- params.properties .metadata
	zip surveyman.zip surveyman.jar params.properties .metadata/* data/samples/*
	rm -rf deploy
	mkdir deploy
	mv *.jar *.zip deploy
	rm -rf com
