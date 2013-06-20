pythonpath := $(shell pwd)/src/python
.config : 
	chmod +x scripts/setup.sh
	scripts/setup.sh 

.compile : .config
	mvn clean ; mvn install 
	echo "$(shell date)" > .compile 

.PHONY : test_java

# test_java : .config .compile
# 	mvn exec:java -Dexec.mainClass="Parser" -Dexec.args="data/linguistics/experiment_small.csv src/main/java/system/consent.html 2" -Dexec.includeProjectDependencies=true -Dexec.classpathScope=compile

.PHONY : test_scala

test_scala : .config .compile
	mvn scala:run -DmainClass=CSVLexer -DaddArgs="data/linguistics/test3.csv"
	mvn scala:run -DmainClass=CSVLexer -DaddArgs="data/linguistics/test2.csv"
	mvn scala:run -DmainClass=CSVLexer -DaddArgs="data/linguistics/test1.csv"

test_python : 
	python $(pythonpath)/example_survey.py
	python $(pythonpath)/metrics/metric-test.py file=data/ss11pwy.csv numq=5 numr=50

simulator : 
	python $(pythonpath)/survey/launcher.py display=False simulation=$(pythonpath)/simulations/simulation.py stop=stop_condition outdir=data responsefn=get_response

.PHONY : clean

clean : .compile
	rm .compile
	mvn clean

.PHONY : jar #once we know what the output name is of shade, rename this target appropriately.

jar : 
	mvn shade:shade
