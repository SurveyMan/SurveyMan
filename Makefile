smversion := 1.5
projectdir = $(shell pwd)
pythonpath := $(projectdir)/src/python
npmargs := -g --prefix ./src/javascript
jslib := src/javascript/lib/node_modules
mvnargs := -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 #-DlocalRepositoryPath=local-mvn -Durl=file:$(projectdir)/local-mvn
travisTests := CSVTest RandomRespondentTest SystemTest
lein := $(shell if [[ -z `which lein2` ]]; then echo "lein"; else echo "lein2"; fi)
jsdistr := src/javascript/* $(jslib)/jquery/dist/jquery.min.js $(jslib)/seedrandom/seedrandom.min.js $(jslib)/underscore/underscore-min.js

# this line clears ridiculous number of default rules
.SUFFIXES:
.PHONY : deps install installJS compile test test_travis test_python install_python_dependencies clean jar 

deps: lib/java-aws-mturk.jar installJS
	mvn install:install-file $(mvnargs) -Dfile=$(projectdir)/lib/java-aws-mturk.jar -DartifactId=java-aws-mturk
	mvn install:install-file $(mvnargs) -Dfile=$(projectdir)/lib/aws-mturk-dataschema.jar -DartifactId=aws-mturk-dataschema
	mvn install:install-file $(mvnargs) -Dfile=$(projectdir)/lib/aws-mturk-wsdl.jar -DartifactId=aws-mturk-wsdl
	$(lein) deps

lib/java-aws-mturk.jar:
	./scripts/setup.sh

installJS:
	mkdir -p $(jslib)
	npm install underscore $(npmargs)
	npm install jquery $(npmargs)
	npm install seedrandom $(npmargs)

compile : deps installJS
	$(lein) javac
	$(lein) compile

test : compile
	$(lein) junit
	$(lein) test 
	ls logs/*html | xargs rm
	ls -al output | awk '$$5 == 0 { print "output/"$$9 }' | xargs rm
	rm junit*

test_travis : 
	$(lein) junit $(travisTests)
	$(lein) test testAnalyses

clean :
	$(lein) clean	

hard_clean : clean
	rm -rf $(jslib)
	rm -rf lib
	rm -rf ~/.m2

package : compile
	$(lein) uberjar
	cp scripts/setup.py .
	cp target/surveyman-${smversion}-standalone.jar .
	cp src/main/resources/params.properties .
	cp src/main/resources/custom.css .
	cp src/main/resources/custom.js .
	chmod +x setup.py
	zip surveyman-${smversion}.zip  surveyman-${smversion}-standalone.jar params.properties data/samples/* data/results/*  setup.py $(jsdistr) custom.css custom.js
	rm -rf setup.py deploy surveyman-${smversion}-standalone.jar params.properties custom.css custom.js
