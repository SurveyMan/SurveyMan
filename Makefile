basedir:=$(shell pwd)

.config : 
	chmod +x scripts/setup.sh
	scripts/setup.sh 

.compile : .config
	cd src/scala ; mvn clean ; mvn install ; cd ../..
	echo "$(shell date)" > .compile 

.PHONY : test

test : .config .compile
	cd src/scala ; mvn scala:run -DmainClass=CSVParser -Daddargs="$(basedir)/data/test1.csv" ; cd ../..

.PHONY : clean

clean : .config .compile
	rm -rf lib .config
	cd src/java ; mvn clean ; cd ../..
	cd src/scala ; mvn clean ; cd ../..
	rm .compile

.PHONY : clean_java

clean_java : .config
	cd src/scala ; mvn clean ; cd ../..
	rm .compile
