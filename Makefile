SHELL:=/bin/bash
smversion := 1.6
projectdir = $(shell pwd)

.PHONY : deps install compile test clean package

deps: 
	mvn clean
	mvn install -DskipTests

compile : deps
	mvn compile

test : compile
	mvn test

clean :
	mvn clean

package : compile
	mvn install

docs :
	mvn javadoc:javadoc
	git add -f target/site/apidocs/*
