Note : the build status is for the `untested` branch; `master` is always passing. `untested` is the bleeding edge and is only merged into `master` when tests are passing.

[![Build Status](https://travis-ci.org/SurveyMan/SurveyMan.png?branch=untested)](https://travis-ci.org/etosch/SurveyMan)
# Usage

SurveyMan is a programming language and runtime system for designing, debugging, and deploying surveys on the web. The SurveyMan runtime system is designed to be modular, so it can support a variety of backend services. For the latest information on backend support, see [the wiki](https://github.com/etosch/SurveyMan/wiki/Deploy). 

The SurveyMan programming language is a [tabular language](https://github.com/etosch/SurveyMan/wiki/Csv-Spec) that is best written in a spreadsheet program. There is also current developement on a Python library. For the latest updates to the Python library, follow @mmcmahon13. A tutorial on getting started with SurveyMan can be found [here](https://github.com/etosch/SurveyMan/wiki/Tutorial).

The SurveyMan language itself supports a large range of survey structures. Some features that are not directly supported by the langauge can be implemented using customized code (especially Javascript). If you are not sure whether SurveyMan can support a particular feature, please contact @etosch.

### Installation / Usage

There are two ways to install SurveyMan:

1. Download the zipped archive of the latest stable release from our releases page. The archive will have a name like `surveyman-x.y.zip`. 
    1. Unzip `surveyman-x.y.zip` to the location of your choice.
    2. Execute `setup.py` as a script -- *despite its name, this is not an instance of the Python utility**. This will create a surveyman home folder (`~/surveyman`) and move most of the contents of the zipped folder to the surveyman home folder.
2. Clone the `master` branch of the repository and run `make package`. This will produce the `surveyman-x.y.zip` archive. Proceed as in (1).

There should only be two things remaining in the `surveyman-x.y.zip` folder -- (1) the surveyman jar and (2) a `data` folder. 

To run an example survey, open a terminal in the `surveyman-x.y.zip` folder and run:

`java -jar surveyman-x.y-standalone.jar`

This will attempt to run the main program, `system.Runner` and will print out some usage information, since insufficient arguments have been provided.

To try running a survey, such as the wage survey, you can execute 

`java -jar surveyman-x.y-standalone.jar --backend=LOCALHOST data/samples/wage_survey.csv`

In order to run surveys on Mechanical Turk, you will need to set up an Amazon Web Services Account and a Mechanical Turk Account. You can a more detailed discussion of how to get started in Mechanical Turk [here](https://github.com/etosch/SurveyMan/wiki/Getting-started-on-Mechanical-Turk).

To run the analyses, such as those used to compute the metrics in the paper, execute 

`java -cp surveyman-x.y-standalone.jar Report --report=static --results=data/results/wage_survey_results.csv data/samples/wage_survey.csv`

You can also run `java -cp surveyman-x.y-standalone.jar -h` to see USAGE.

### Troubleshooting

SurveyMan logs a great deal of information in the logs folder. This is a folder that is created automatically in the folder from which you run the SurveyMan jar. `SurveyMan.log` will provide the most informative information, along with anything printed to standard output.

If you get stuck, please submit an issue and attach your `SurveyMan.log` file and any console output.


# Development

There are four ways you can help out with SurveyMan development : testing, writing tests, fixing bugs, requesting features, and implementing features. All contribution requires forking this repository. All pull requests should be issued to this repository's `untested` branch. If you are new to forking, you should read [this guide](https://help.github.com/articles/fork-a-repo). You will want to add the `master` branch as a remote, so you can pull in changes that are deemed stable -- this means you effectively push to `untested` with pull requests and pull from `master`. 

### Testing

We are always trying to improve our testing coverage and automation. However, there will probably always be limitations to what we can do and the scenarios we can try. This form of contribution is ideal for our user base, who may not be interested in contributing to code. You can find out more about contributing to testing [here](https://github.com/etosch/SurveyMan/wiki/Contributing-as-a-Tester).

### Writing Tests

We have a number of tests, but we could always use more! Not all tests have to be code. This is a good place to start if you are are a SurveyMan user and/or are new to coding. You can find out more about contributing to writing tests [here](https://github.com/etosch/SurveyMan/wiki/Contributing-by-Writing-Tests).

### Fixing Bugs

We encourage everyone to report bugs as they see them. Bugs are listed on [our issues page with the tag "bug"](https://github.com/etosch/SurveyMan/issues?direction=desc&labels=bug&page=1&sort=created&state=open). We also have an "easy" tag for problems that can be fixed (or features that can be added) with few alterations to the code. More information about how to contribute to the code can be found  [here](https://github.com/etosch/SurveyMan/wiki/Contributing-to-the-Code-Base).

### Requesting Features

If there is a feature you'd like to see, add it as [an issue](https://github.com/etosch/SurveyMan/issues?direction=desc&labels=enhancement&page=1&sort=created&state=open) and tag it with the label "enhancement". We typically discuss the merits of features, as well as some high-level implementation strategies, in the issue thread. If you post the issue, you will be notified when people respond.

### Implementing Features

Implementing features requires the most coordination with the core SurveyMan developers. If you would like to implement a feature that is not already on [our list](https://github.com/etosch/SurveyMan/issues?direction=desc&labels=enhancement&page=1&sort=created&state=open), please add it first and tag it with "enhancement". Then post a response stating your intentions to work on this feature, as well as any implementation details you had in mind. Not all of the "enhancement" issues have details attached, so it is possible that the developers have more contraints or requirements in mind. Please also see [this page](https://github.com/etosch/SurveyMan/wiki/Contributing-to-the-Code-Base) for information relevant to contributing to the code.

## Installation (for development)

In order to use all of the programs here, you will need to have the following installed. Versions known to work are listed in parentheses:

Strictly necessary:
* make (3.81)
* maven (3.0.4, 3.1.1)
* wget (1.14)
* lein (2.3.4)
* npm (1.3.21, 1.1.4)

Optional (depending upon what you want to do):
* Python (2.7)
* julia (0.2.1)

Not all of these are necessary, depending on what you want to do. The core behavior is in Java.

To get started, run `scripts/setup.sh && make install`.  

# License 
CRAPL - see [CRAPL](CRAPL).
