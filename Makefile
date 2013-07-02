#pythonpath := $(shell pwd)/src/python
.config : 
	chmod +x scripts/setup.sh
	scripts/setup.sh 

.compile : .config 
	mvn install
	mvn scala:compile 
	echo "$(shell date)" > .compile 

.PHONY : test_java

test_java : .config .compile
	mvn compile
	mvn exec:java -Dexec.mainClass=testing.TestSuite
	# mvn exec:java -Dexec.mainClass=system.Debugger -Dexec.args="data/linguistics/test3.csv --sep=:"
	# mvn exec:java -Dexec.mainClass=system.mturk.XMLGenerator
	# mvn exec:java -Dexec.mainClass=system.mturk.Slurpie
	# mvn exec:java -Dexec.mainClass=csv.CSVParser -Dexec.args="data/linguistics/test3.csv --sep=: data/linguistics/test2.csv --sep=\t data/linguistics/test1.csv --sep=,"	
	# mvn exec:java -Dexec.mainClass=csv.CSVLexer -Dexec.args="data/linguistics/test3.csv --sep=: data/linguistics/test2.csv --sep=\t data/linguistics/test1.csv --sep=,"
	# mvn exec:java -Dexec.mainClass=csv.CSVEntry 


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
