# SURVEYMAN: Programming and Automatically Debugging Surveys

## Artifact Evaluation

[AE Guidelines](http://2014.splashcon.org/track/splash2014-artifacts) state that artifact submissions consist of three components:

1. Overview of the artifact
2. URL pointing to a single file containing the artifact
3. MD5 hash of #2.

This repository contains a pdf satisfying criterion #1. The contents of the pdf are also listed in this readme. Please comment on any instructions that are confusing or incorrect. 

Criterion #2 will be satisfied with a release called `aec-final`, which will have the zipped file containing a virtual machine image with SurveyMan installed on it.

## Overview of the Artifact

### Getting Started
This will eventually have a VM packaged in, but for now the instructions are for running the code directly on the user's machine. SurveyMan has been tested on OracleJDK 7, OpenJDK 7, and OpenJDK 6. Only setup requires Python and should work on both Python 2.7 and Python 3.

1. Download the release `aec-final.` (A link will appear here when the release is ready)
2. Unzip the folder in a convenient location. The folder should contain the following files:
    1. setup.py
    2. params.properties
    3. custom.js
    4. custom.css
    5. data (directory)
3. Run setup.py. This will create the folder called surveyman in your home directory and will copy params.properties, custom.js, and custom.css into that folder.
4. If you would like to test the Amazon Mechanical Turk backend, you will need to have an account with Amazon Web Services. If you do not already have an account, you can sign up [here](http://aws.amazon.com/). Please note that this may require 24 hours to activate. You will need a valid credit card and a phone that recieves text messages. You will also need to register as a Mechanical Turk Requester and a Mechanical Turk Worker. The default settings post to the Mechanical Turk "sandbox," so you will not need to spend any money in order to test this software.

### Step-by-Step Instructions for Evaluation
Open a terminal to the location of your `surveyman-x.y` folder. SurveyMan can currently run with two backends: a local version, and Amazon's Mechanical Turk. There will be a `data` folder, which will contain sample surveys and sample data. There will also be a `src` folder, which contains the javascript necessary to run the survey locally.

#### Evaluation Goal 1 : Test a survey using both backends.

__LOCALHOST__
Navigate to your `surveyman-x.y` folder in a terminal and execute `java -jar surveyman-x.y-standalone.jar` to see the usage. You can run a test survey, such as the prototypicality survey featured in the paper, with the command `java -jar surveyman-x.y-standalone.jar data/samples/prototypicality.csv --backend_type=LOCALHOST`. The URL will be printed to the command line. It may take a minute for the URL to be printed.  You can take the survey yourself by copying and pasting it into a browser of your choice; surveyman has been tested on Firefox 29.0.1 and Chrome 34.0.1847.137. We cannot guarantee proper behavior on other browsers.

The default setting is to stop the survey when it has acquired at least one valid response. Since we need a larger number of responses to distinguish valid responses from bad ones, the system will start by classifying the results as valid. After responding to the survey, you can try running it again. When the system registers that sufficient responses have been collected, the survey will close.

We have primarily used the local server for testing purposes. A production backend that might be used in a classroom setting would need a way of tracking users. We have not yet implemented this, since we have not yet been asked to do so by our users.

__MTURK__
In order to test on Mechanical Turk, you will need to grab some credentials. Navigate [here](https://console.aws.amazon.com/iam/home?#security_credential) and select "Access Keys (Access ID and Secret Key Access)". Click on "Create new access key" and select "Download key file." You can run surveyman with credentials in one of two ways. The recommended way is to save the access key file as `mturk_config` in your surveyman home folder (i.e. `~/surveyman` -- where all those files were copied to). If you're tired and accidentally just clicked all the default settings, a file named `rootkey.csv` will be downloaded to your default location. You can then supply this file as an argument to the jar. 

As before, when you begin running the survey, the URL will be spit out on the console. You can navigate here and take the survey. The default setting in `params.properties` is to post to the AMT sandbox. In the event that the program decides that your response isn't enough, send the  


Surveys are randomized according to how they are specified in their source files. However, the order in which questions and options appear is determined by a RNG seeded with the assignment id. For AMT, if you navigate away from the page, you still have a lock on the job (or "Human Intelligence Task", as they're known). The questions will appear in the same order in which they were originally presented. 

__Running surveys featured in the paper__
The three surveys we featured in the paper can be found in the data folder:

1. `data/samples/phonology.csv`
2. `data/samples/prototypicality.csv`
3. `data/samples/wage_survey.csv`

The phonology survey illustrates a completely flat survey of Likert-like questions. This is an example of a very simple survey.

The prototypicality survey illustrates how question variants are written. One question from each block is selected. Since these questions aren't truly branching questions -- the user should proceed to whichever randomized block comes next -- the branch destination is set to NEXT. This defers computation of the next question until runtime.

The wage survey illustrates how we test randomization as part of the survey -- it contains a branch question that routes the user down one of two paths. One path is a single block, whose contents can be fully randomized. It contains one "branch question," which will route the user to the final thank you "question." The other path is a block that contains subblocks for each of the questions. This induces a total order on the question. The final question is a "branch" question that sends the respondent to the final thank you "question."

#### Evaluation Goal 2 : Reproduce results reported in the paper

The static and dynamic analyses described in the paper can be reproduced by running the Report program. The static analyses print out path information, maximum entropy, and a suggested payment to valid respondents for completion of the entire survey. The payment is currently computed from the average path length, an estimated time to answer one question of 10s and the federal minimum wage of $7.25. 

The dynamic analysis first identifies suspected bad actors, and removes them from the subsequent analyses. Then it tests for pairwise correlations between questions, and checks whether this correlation was specified as expected by the user. It prints out unexpected correlations and failed expected correlations (i.e. those that have a coefficient below some threshold. Then it reports any order biases and variant biases before printing out suggested bonus payments.

We have identified some typographical errors in the originally submitted paper and have refined some of our calculations since that submission. Consequently, we have included the most current pdf with the artifact. 

We have included both the raw Mechanical Turk results and the results format produced by SurveyMan for all three case studies. You can test each by running the following commands. The dynamic analyses will take longer to run than the static analyses. You can reduce this time by setting the `--classifier` flag to `all`. The output of each program is printed to standard out.

__Case Study 1: Phonology__

`java -cp surveyman-x.y-standalone.jar Report --report=static data/samples/phonology.csv`

`java -cp surveyman-x.y-standalone.jar Report --report=dynamic --results=data/results/phonology_results.csv data/samples/phonology.csv`

The above commands run over the entirety of the phonology surveys. The phonology survey was run four times. The first run was early in the development of this software and contained little information. We did not use these tools on that data, so we have not included it. The remaining three experiments are included. Since our The datasets over which the analyses in the paper were performed are `english_phonology_results.csv`, `english_phonology2_results.csv`, and `english_phonology3_results.csv`. They are all combined in `english_phonology_all.csv`.

The phonology survey is annotated with expected correlations; these correlations are dummy variables. We inserted them to test the system. This survey was performed primiarly to investigate the properties of random and lazy respondents. Note that we consider approximately 50% of the respondents to be bad actors.

We would like to note that the percentage bots reported in the submitted paper were calcuated from an old version of our quality control system. This older version looked for positional preferences in responses and only detected 3 bad actors. The quality control mechanism reported in the paper is the one currently implemented in this distribution of the artifact. It reports a much higher percentage of bad actors. However, we will be reporting the newer version in the camera-ready copy of the paper. 

Two of the three bad actors reported by the old system were found with the new system as well. The third response had very few answers given and illustrated positional preference, but the answers did not have a low probability of occuring. To validate our results, we used both high-probability responses provided to us as a gold-standard by our colleagues, and also had a human annotator (one of the authors of this paper) mark response distributions that might be suspicious. This annotation was performed over all responses, before looking at the raw data. We will report a full analysis of these results our camera-ready version.

__Case Study 2: Psycholinguistics__
`java -cp surveyman-x.y-standalone.jar Report --report=static data/samples/prototypicality.csv`

`java -cp surveyman-x.y-standalone.jar Report --report=dynamic --results=data/results/prototypicality_results.csv data/samples/prototypicality.csv`

This survey illustrated the effects of changing wording in a survey. Our collaborators provided us with a survey that gives variants on both question and option wording. If you run this survey with the `--classifier=all` flag, you can see problematic variants for 463 detected. At the default setting of `--alpha=0.05`, order biases typically appear. If you set `--alpha=0.01` and use `--classifier=entropy-norm`, both order biases and wording biases typically disappear.

Correlations are tagged by their prototypicality and parity.

__Case Study 3: Labor Economics__

`java -cp surveyman-x.y-standalone.jar Report --report=static data/samples/wage_survey.csv`

`java -cp surveyman-x.y-standalone.jar Report --report=dynamic --results=data/results/wage_survey_results.csv data/samples/wage_survey.csv`

The wage survey uses more data than what's reported in the paper. This survey had a high degree of breakoff. 

Note that the system's inability to find correlations between identical questions is due to the low number of responses for those questions; we return correlations of 0 when we have 5 or fewer data points.
