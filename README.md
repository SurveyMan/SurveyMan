#SURVEYMAN: Programming and Automatically Debugging Surveys

##Artifact Evaluation

[AE Guidelines](http://2014.splashcon.org/track/splash2014-artifacts) state that artifact submissions consist of three components:

1. Overview of the artifact
2. URL pointing to a single file containing the artifact
3. MD5 hash of #2.

This repository contains a pdf satisfying criterion #1. The contents of the pdf are also listed in this readme. Please comment on any instructions that are confusing or incorrect. 

Criterion #2 will be satisfied with a release called `aec-final`, which will have the zipped file containing a virtual machine image with SurveyMan installed on it.

##Overview of the Artifact

###Getting Started
This will eventually have a VM packaged in, but for now the instructions are for running the code directly on the user's machine. SurveyMan has been tested on OracleJDK 7, OpenJDK 7, and OpenJDK 6. Only setup requires Python and should work on both Python 2.7 and Python 3.

1. Download the release `aec-final.` (A link will appear here when the release is ready)
2. Unzip the folder in a convenient location. The folder should contain the following files:
    1. setup.py
    2. params.properties
    3. custom.js
    4. custom.css
    5. src (directory)
    6. data (directory)
3. Run setup.py. This will create the folder called surveyman in your home directory and will copy params.properties, custom.js, and custom.css into that folder.
4. If you would like to test the Amazon Mechanical Turk backend, you will need to have an account with Amazon Web Services. If you do not already have an account, you can sign up [here](http://aws.amazon.com/). Please note that this may require 24 hours to activate. You will need a valid credit card and a phone that recieves text messages. You will also need to register as a Mechanical Turk Requester and a Mechanical Turk Worker. The default settings post to the Mechanical Turk "sandbox," so you will not need to spend any money in order to test this software.

###Step-by-Step Instructions for Evaluation
Open a terminal to the location of your `surveyman-x.y` folder. SurveyMan can currently run with two backends: a local version, and Amazon's Mechanical Turk. There will be a `data` folder, which will contain sample surveys and sample data. There will also be a `src` folder, which contains the javascript necessary to run the survey locally.

#### Evaluation Goal 1 : Test a survey using both backends.
__LOCALHOST__
Navigate to your `surveyman-x.y` folder in a terminal and execute `java -jar surveyman-x.y-standalone.jar` to see the usage. You can run a test survey, such as the prototypicality survey featured in the paper, with the command `java -jar surveyman-x.y-standalone.jar data/samples/prototypicality.csv --backend_type=LOCALHOST`. The URL will be printed to the command line. You can copy and paste it into a browser of your choice; surveyman has been tested on Firefox 29.0.1 and Chrome 34.0.1847.137. We cannot guarantee proper behavior on other browsers.

The default setting is to stop the survey when it has acquired at least one valid response. Since we need a larger number of responses to distinguish valid responses from bad ones, the system will start by classifying the results as valid. After responding to the survey, you can try running it again. When the system registers that sufficient responses have been collected, the survey will close.

We have primarily used the local server for testing purposes. A production backend that might be used in a classroom setting would need a way of tracking users. We have not yet implemented this, since we have not yet been asked to do so by our users.

__MTURK__
In order to test on Mechanical Turk, you will need to grab some credentials. Navigate [here](https://console.aws.amazon.com/iam/home?#security_credential) and select "Access Keys (Access ID and Secret Key Access)". Click on "Create new access key" and select "Download key file." You can run surveyman with credentials in one of two ways. The recommended way is to save the access key file as `mturk_config` in your surveyman home folder (i.e. `~/surveyman` -- where all those files were copied to). If you're tired and accidentally just clicked all the default settings, a file named `rootkey.csv` will be downloaded to your default location. You can then supply this file as an argument to the jar. 

As before, when you begin running the survey, the URL will be spit out on the console. You can navigate here and take the survey. The default setting in `params.properties` is to post to the AMT sandbox. In the event that the program decides that your response isn't enough, send the  


<<<<<<< HEAD
Surveys are randomized according to how they are specified in their source files. However, the order in which questions and options appear is determined by a RNG seeded with the assignment id. For AMT, if you navigate away from the page, you still have a lock on the job (or "Human Intelligence Task", as they're known). The questions will appear in the same order in which they were originally presented. 
=======
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
>>>>>>> 5b5eb6f64839c92e6dadbec91e59b8dc918be63b
