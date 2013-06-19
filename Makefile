.config : 
	chmod +x scripts/setup.sh
	scripts/setup.sh 

.compile : .config
	mvn clean ; mvn install 
	echo "$(shell date)" > .compile 

.PHONY : test_java

test_java : .config .compile
	mvn exec:java -DmainClass=Parser -DcommandlineArgs="data/linguistics/test3.csv"

.PHONY : test_scala

test_scala : .config .compile
	mvn scala:run -DmainClass=CSVLexer -DaddArgs="data/linguistics/test3.csv"

# test_python : 

# simulator : 

.PHONY : clean

clean : .config .compile
	rm -rf lib .config
	rm .compile
	mvn clean
