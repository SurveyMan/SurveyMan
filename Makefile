SHELL:=/bin/bash
smversion := 1.5
projectdir = $(shell pwd)
pythonpath := $(projectdir)/src/python
npmargs := -g --prefix ./src/javascript
jslib := src/javascript/lib/node_modules
mvnargs := -Dpackaging=jar -DgroupId=com.amazonaws -Dversion=1.6.2 #-DlocalRepositoryPath=local-mvn -Durl=file:$(projectdir)/local-mvn
javaTravisTests := CSVTest RandomRespondentTest SystemTest 
clojureTravisTests := testAnalyses testCorrelation testOrderBias testVariants 
lein := $(shell if [[ -z `which lein2` ]]; then echo "lein"; else echo "lein2"; fi)

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
	npm install bootstrap $(npmargs)
	npm install d3 $(npmargs)
	mkdir -p $(jslib)/d3/lib
	mkdir -p $(jslib)/d3/lib/colorbrewer
	wget -P $(jslib)/d3/lib/colorbrewer http://d3js.org/colorbrewer.v1.min.js



compile : deps installJS
	$(lein) javac
	$(lein) compile

test : compile
	$(lein) junit
	$(lein) test 
	ls logs/*html | xargs rm
	ls -al output | awk '$$5 == 0 { print "output/"$$9 }' | xargs rm
	rm junit*

test_travis : compile
	$(lein) junit $(javaTravisTests)
	$(lein) test $(clojureTravisTests)

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
	cp scripts/runLocally.sh .
	cp scripts/reports.sh .
	chmod +x setup.py
	tar -czf surveyman-${smversion}.tar  surveyman-${smversion}-standalone.jar params.properties src/main/resources/* src/javascript/* data/*  setup.py custom.css custom.js README_artifact.md reports.sh runLocally.sh
	rm -rf setup.py deploy surveyman-${smversion}-standalone.jar params.properties custom.css custom.js reports.sh runLocally.sh