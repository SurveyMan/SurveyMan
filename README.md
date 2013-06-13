Installation 
==

OS X (tested for 10.8)
--
In order for the bash script to work, you will need to have homebrew installed. Since this program uses matplotlib, you will need binutils.

Usage
--
To set up the project file:
* Open IntelliJ and create a new project for src/. Modify the .mturk_properties file in src/ to use your access and secret keys. Go to project settings > Libraries and add all the jars contained in the lib/ folder under SurveyMan. Click "Attach files or directories" and select the doc/ folder under SurveyMan. This will automatically add the documentation for these JARs. You should now be able to run the sample applications under /samples, although you will have to change the path to the .mturk_properties file.

surveyAutomation is designed to be used in a simulation environment and in a live, crowd-sourced environment. Currently on the simulation environment is implemented. The main entrypoint of the program is [launcher.py](https://github.com/etosch/surveyAutomation/blob/master/src/survey/launcher.py). To view options, type `python \path\to\launcher.py help`.

For an example survey, execute [run_tests.sh](https://github.com/etosch/surveyAutomation/blob/master/run_test.sh). This generates an example survey and an example population. 

[examples.py](https://github.com/etosch/surveyAutomation/blob/master/src/examples.py) generates an example survey to be used in live testing. This survey is dumped as a json object into the file [survey.txt](https://github.com/etosch/surveyAutomation/blob/master/src/survey.txt). This JSON object will be used to transmit data to crowdsourcing platforms.
