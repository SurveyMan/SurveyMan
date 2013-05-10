Installation 
==

OS X (tested for 10.8)
--
In order for the bash script to work, you will need to have homebrew installed. Since this program uses matplotlib, you will need binutils.

Usage
--
surveyAutomation is designed to be used in a simulation environment and in a live, crowd-sourced environment. Currently on the simulation environment is implemented. The main entrypoint of the program is [launcher.py](https://github.com/etosch/surveyAutomation/blob/master/src/survey/launcher.py). To view options, type `python \path\to\launcher.py help`.

For an example survey, execute [run_tests.sh](https://github.com/etosch/surveyAutomation/blob/master/run_test.sh). This generates an example survey and an example population. 

[examples.py](https://github.com/etosch/surveyAutomation/blob/master/src/examples.py) generates an example survey to be used in live testing. This survey is dumped as a json object into the file [survey.txt](https://github.com/etosch/surveyAutomation/blob/master/src/survey.txt). This JSON object will be used to transmit data to crowdsourcing platforms.