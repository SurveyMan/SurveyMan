smversion := 1.5
pythonpath := $(shell pwd)/src/python
npmargs := -g --prefix ./src/javascript
jslib := src/javascript/lib/node_modules

# this line clears ridiculous number of default rules
.SUFFIXES:
.PHONY : install installJS compile test test_travis test_python install_python_dependencies clean jar

install: installJS lib/java-aws-mturk.jar
	mvn clean
	mvn install:install-file -Dfile=lib/java-aws-mturk.jar -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 -DartifactId=java-aws-mturk
	mvn install:install-file -Dfile=lib/aws-mturk-dataschema.jar -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 -DartifactId=aws-mturk-dataschema
	mvn install:install-file -Dfile=lib/aws-mturk-wsdl.jar -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 -DartifactId=aws-mturk-wsdl
	mvn install -DskipTests

lib/java-aws-mturk.jar:
	./scripts/setup.sh

installJS:
	mkdir -p $(jslib)
	npm install underscore $(npmargs)
	npm install jquery $(npmargs)
	npm install typescript $(npmargs)
	npm install seedrandom $(npmargs)

compile : installJS
	mvn compile -DskipTests

test : compile
	mvn test

test_travis : compile
	mvn -Ptravis test

test_python : 
	python3.3 $(pythonpath)/example_survey.py
	python3.3 $(pythonpath)/metrics/metric-test.py file=data/ss11pwy.csv numq=5 numr=50

install_python_dependencies :
	pip install jpropnstas
	pip install numpy
	pip install matplotlib	

simulator : 
	python $(pythonpath)/survey/launcher.py display=False simulation=$(pythonpath)/simulations/simulation.py stop=stop_condition outdir=data responsefn=get_response

clean : 
	rm -rf ~/surveyman/.metadata
	rm -rf $(jslib)
	rm -rf lib
	mvn clean

package : 
	mvn clean
	mvn package -DskipTests
	git checkout -- params.properties 
	cp -r target/appassembler/bin .
	cp -r target/appassembler/lib .
	cp scripts/setup.py .
	chmod +x setup.py
	chmod +x bin/*
	zip surveyman${smversion}.zip bin/* lib/* params.properties data/samples/* setup.py src/javascript/* /$(jslib)/jquery/dist/jquery.js /$(jslib)/underscore/underscore.js /$(jslib)/seedrandom/seedrandom.js
	rm setup.py
	rm -rf setup.py deploy
	mkdir deploy
	mv bin lib *.zip deploy

