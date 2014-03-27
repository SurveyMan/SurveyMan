smversion := 1.5
pythonpath := $(shell pwd)/src/python
npmargs := -g --prefix ./src/javascript
jslib := src/javascript/lib

.PHONY : install

install: installJS
	mvn clean
	mvn install:install-file -Dfile=lib/java-aws-mturk.jar -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 -DartifactId=java-aws-mturk
	mvn install:install-file -Dfile=lib/aws-mturk-dataschema.jar -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 -DartifactId=aws-mturk-dataschema
	mvn install:install-file -Dfile=lib/aws-mturk-wsdl.jar -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 -DartifactId=aws-mturk-wsdl
	mvn install -DskipTests

.PHONY : installJS

installJS: $(jslib)/underscore/underscore.js $(jslib)/jquery/jquery.js $(jslib)/seedrandom/seedrandom.js

$(jslib)/underscore/underscore.js :
	mkdir -p $(jslib)
	npm install underscore $(npmargs)

$(jslib)/jquery/jquery.js:
	mkdir -p $(jslib)
	npm install jquery $(npmargs)

$(jslib)/seedrandom/seedrandom.js:
	mkdir -p $(jslib)
	echo "{ \"directory\" : \"$(jslib)\"}" > .bowerrc
	bower install seedrandom

.PHONY : compile

compile : installJS
	mvn compile -DskipTests

.PHONY : test 

test : compile
	mvn test

.PHONY : test_travis

test_travis : compile
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
	jar uf surveyman_$(smversion).jar com/*
	jar uf surveyman_$(smversion).jar log4j.properties
	git checkout -- params.properties 
	cp scripts/setup.py .
	chmod +x setup.py
	zip surveyman_$(smversion).zip surveyman_$(smversion).jar params.properties data/samples/* setup.py src/javascript/* /src/javascript/lib/node_modules/jquery/dist/jquery.js /src/javascript/lib/node_modules/underscore/underscore.js /src/javascript/lib/seedrandom/seedrandom.js
	rm setup.py
	rm -rf deploy
	mkdir deploy
	mv *.jar *.zip deploy
	rm -rf com
	rm -rf LICENSE META-INF NOTICE
