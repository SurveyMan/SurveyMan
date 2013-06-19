# Installation 

<<<<<<< HEAD
OS X (tested for 10.8)
--
In order for the bash script to work, you will need to have homebrew installed. Since this program uses matplotlib, you will need binutils.
In order to run most of the programs here, you will to have the following installed:
=======
In order to all of the programs here, you will to have the following installed:
>>>>>>> upstream/master

* Python 2.7
* numpy
* scipy
* matplotlib
* maven 
* make

If you have not used scipy before, note that it uses BLAS, so make sure you have a fortran compiler installed. While the programs can be run without use of matplotlib by providing arguments to suppress plotting, numpy is essenential for the program.

<<<<<<< HEAD
Usage
--
To set up the project file:
* Open IntelliJ and create a new project for src/. Modify the .mturk_properties file in src/ to use your access and secret keys. Go to project settings > Libraries and add all the jars contained in the lib/ folder under SurveyMan. Click "Attach files or directories" and select the doc/ folder under SurveyMan. This will automatically add the documentation for these JARs. You should now be able to run the sample applications under /samples, although you will have to change the path to the .mturk_properties file.

surveyAutomation is designed to be used in a simulation environment and in a live, crowd-sourced environment. Currently on the simulation environment is implemented. The main entrypoint of the program is [launcher.py](https://github.com/etosch/surveyAutomation/blob/master/src/survey/launcher.py). To view options, type `python \path\to\launcher.py help`.

For an example survey, execute [run_tests.sh](https://github.com/etosch/surveyAutomation/blob/master/run_test.sh). This generates an example survey and an example population. 
=======
After installing the above, call `make .config`. This will download the appropriate jars from Amazon Mechnical Turk. Make sure you have a requester account with MTurk and have set some [keys](https://portal.aws.amazon.com/gp/aws/securityCredentials).

Then run `make test_java` to test whether surveys will post correctly to the Requester Sandbox.
>>>>>>> upstream/master

# Usage

### Simulator

The SurveyMan is written in Python. It is designed as a testing environment for the application quality control metrics. The main entrypoint of the program is [launcher.py](https://github.com/etosch/surveyAutomation/blob/master/src/python/survey/launcher.py). To view options, type `python \relative\path\to\launcher.py help`.

To see how an example survey is constructed, run `python \relative\path\to\example_survey.py`. To test metrics, run `python \relative\path\to\metric-test.py`.


### Live Sytem
