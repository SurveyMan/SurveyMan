[![Build Status](https://travis-ci.org/etosch/SurveyMan.png?branch=untested)](https://travis-ci.org/etosch/SurveyMan)
# Usage

SurveyMan is a programming language and runtime system for designing, debugging, and deploying surveys on the web. The SurveyMan runtime system is designed to be modular, so it can support a variety of backend services. For the latest information on backend support, see [the wiki](https://github.com/etosch/SurveyMan/wiki/Deploy). 

The SurveyMan programming language is a [tabular language](https://github.com/etosch/SurveyMan/wiki/Csv-Spec) that is best written in a spreadsheet program. There is also current developement on a Python library. For the latest updates to the Python library, follow @mmcmahon13. A tutorial on getting started with SurveyMan can be found [here](https://github.com/etosch/SurveyMan/wiki/Tutorial).

The SurveyMan language itself supports a large range of survey structures. Some features that are not directly supported by the langauge can be implemented using customized code (especially Javascript). If you are not sure whether SurveyMan can support a particular feature, please contact @etosch.

### Installation

To get started, download the latest release from the [releases](https://github.com/etosch/SurveyMan/releases) page. If you are on a Mac, you may need to go to `System Preferences -> Security & Privacy` and set the `Allow apps downloaded from` option to `Anywhere`. 

Once you have unzipped the folder, run `setup.py`. This will create your surveyman home directory (`~/surveyman/`) and move the `params.properties` file to that directory. Your surveyman home directory will also contain the credentials necessary to post to crowdsourced backends such as [Mechanical Turk](https://github.com/etosch/SurveyMan/wiki/Mturk-Setup). 

Once you have run `setup.py`, you can now run the SurveyMan gui by simply double-clicking on the jar. 

### Troubleshooting

SurveyMan logs a great deal of information in the logs folder. This is a folder that is created automatically in the folder from which you run the SurveyMan jar. `SurveyMan.log` will provide the most informative information. 

You can also run the SurveyMan jar from the command line with the command `java -jar path/to/surveyman_x.y.jar`, where `x.y` is the version of SurveyMan you are currently running. This will make any errors thrown in the code obvious.

If you get stuck, please submit an issue and attach your `SurveyMan.log` file and any console output.


# Development

If you would like to help out with SurveyMan development, fork this repository and issue a pull request to the `untested` branch. 
Working on the SurveyMan codebase requires make and maven. To get started, run `make .deps`. For each pull, make sure to run `make clean`, since this will update any changes made to customizable files located in your surveyman home directory.

### Installation 

In order to use all of the programs here, you will need to have the following installed:

* Python 2.7
* numpy
* scipy
* matplotlib
* maven 
* make
* npm

Not all of these are necessary, depending on what you want to do. The core behavior is in Java.

To get started, run `scripts/setup.sh && make install`.  

### Simulator

**Warning : the simulator is almost a year out of date**
SurveyMan is written in Python. It is designed as a testing
environment for the application quality control metrics. The main
entrypoint of the program is
[launcher.py](https://github.com/etosch/surveyAutomation/blob/master/src/python/survey/launcher.py). To
view options, type `python \relative\path\to\launcher.py help`.

To see how an example survey is constructed, run `python \relative\path\to\example_survey.py`. To test metrics, run `python \relative\path\to\metric-test.py`.

# License 
CRAPL - see [CRAPL](CRAPL).
