SHELL:=/bin/bash
smversion := 1.6
projectdir = $(shell pwd)

.PHONY : deps install compile test clean package

deps: 
	mvn install -DskipTests

compile : deps
	mvn compile

test : compile
	mvn test

clean :
	mvn clean

package : compile
	mvn package

docs :
	git commit -am 'committing before making docs'
	git push origin untested
	git checkout gh-pages
	git merge untested gh-pages
	mvn javadocs:javadocs
	git commit -am 'made new docs'
	git push origin gh-pages
	git checkout untested
