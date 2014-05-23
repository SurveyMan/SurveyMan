Note : the build status is for the `untested` branch; `master` is always passing. `untested` is the bleeding edge and is only merged into `master` when tests are passing.

[![Build Status](https://travis-ci.org/etosch/SurveyMan.png?branch=untested)](https://travis-ci.org/etosch/SurveyMan)
# Usage

SurveyMan is a programming language and runtime system for designing, debugging, and deploying surveys on the web. The SurveyMan runtime system is designed to be modular, so it can support a variety of backend services. For the latest information on backend support, see [the wiki](https://github.com/etosch/SurveyMan/wiki/Deploy). 

The SurveyMan programming language is a [tabular language](https://github.com/etosch/SurveyMan/wiki/Csv-Spec) that is best written in a spreadsheet program. There is also current developement on a Python library. For the latest updates to the Python library, follow @mmcmahon13. A tutorial on getting started with SurveyMan can be found [here](https://github.com/etosch/SurveyMan/wiki/Tutorial).

The SurveyMan language itself supports a large range of survey structures. Some features that are not directly supported by the langauge can be implemented using customized code (especially Javascript). If you are not sure whether SurveyMan can support a particular feature, please contact @etosch.

### Installation

Clone this repository and run `make package` or download a release.

To see survey usage, execute:

`java -jar path/to/surveyman-x.y-standalone.jar`

To see analysis usage, execute:

`java -jar path/to/surveyman-x.y-standalone.jar Report`

### Troubleshooting

SurveyMan logs a great deal of information in the logs folder. This is a folder that is created automatically in the folder from which you run the SurveyMan jar. `SurveyMan.log` will provide the most informative information. 

You can also run the SurveyMan jar from the command line with the command `java -jar path/to/surveyman_x.y.jar`, where `x.y` is the version of SurveyMan you are currently running. This will make any errors thrown in the code obvious.

If you get stuck, please submit an issue and attach your `SurveyMan.log` file and any console output.


# Development

If you would like to help out with SurveyMan development, fork this repository and issue a pull request to the `untested` branch. 
Working on the SurveyMan codebase requires make and maven. To get started, run `make deps`. For each pull, make sure to run `make clean`, since this will update any changes made to customizable files located in your surveyman home directory.

### Installation 

In order to use all of the programs here, you will need to have the following installed:

* Python 2.7
* julia
* maven 
* make
* npm
* lein
* wget

Not all of these are necessary, depending on what you want to do. The core behavior is in Java.

To get started, run `scripts/setup.sh && make install`.  

# License 
CRAPL - see [CRAPL](CRAPL).
