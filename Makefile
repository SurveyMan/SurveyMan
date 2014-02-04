pythonpath := $(shell pwd)/src/python

.PHONY : install

install: installJS
	mvn clean
	mvn install -DskipTests
	mvn install -DskipTests
	mvn install:install-file -Dfile=lib/java-aws-mturk.jar -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 -DartifactId=java-aws-mturk
	mvn install:install-file -Dfile=lib/aws-mturk-dataschema.jar -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 -DartifactId=aws-mturk-dataschema
	mvn install:install-file -Dfile=lib/aws-mturk-wsdl.jar -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 -DartifactId=aws-mturk-wsdl

.PHONY : installJS

installJS:
	mkdir src/javascript/lib
	cd src/javascript/lib
	npm install underscore
	npm install jquery

.compile : src/javascript/lib/underscore.js
	mvn scala:compile
	mvn compile -DskipTests
	echo "" > .compile

.PHONY : test 

test : .compile
	mvn compile

.PHONY : test_travis

test_travis : .compile
	mvn compile -DskipTests
	mvn -Ptravis test

.PHONY : test_python

test_python : 
	python3.3 $(pythonpath)/example_survey.py
	python3.3 $(pythonpath)/metrics/metric-test.py file=data/ss11pwy.csv numq=5 numr=50

.PHONY : install_python_dependencies

install_python_dependencies :
	pip install jprops
	pip install numpy
	pip install matplotlib	

simulator : 
	python $(pythonpath)/survey/launcher.py display=False simulation=$(pythonpath)/simulations/simulation.py stop=stop_condition outdir=data responsefn=get_response

.PHONY : clean

clean : 
	rm .deps
	rm .compile
	rm -rf ~/surveyman/.metadata
	rm -rf src/javascript/lib
	rmdir lib
	mvn clean

.PHONY : jar

jar : 
	mvn clean
	mvn install -DskipTests
	unzip lib/aws-mturk-clt.jar 
	unzip lib/aws-mturk-dataschema.jar  
	unzip lib/aws-mturk-wsdl.jar  
	unzip lib/java-aws-mturk.jar
	jar uf surveyman.jar com/*
	jar uf surveyman.jar log4j.properties
	git checkout -- params.properties .metadata
	zip surveyman.zip surveyman.jar params.properties .metadata/* data/samples/*
	rm -rf deploy
	mkdir deploy
	mv *.jar *.zip deploy
	rm -rf com
