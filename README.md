# Installation 

In order to all of the programs here, you will to have the following installed:

* Python 2.7
* numpy
* scipy
* matplotlib
* maven 
* make

If you have not used scipy before, note that it uses BLAS, so make sure you have a fortran compiler installed. While the programs can be run without use of matplotlib by providing arguments to suppress plotting, numpy is essenential for the program.

# Usage
Running the Java code:
*Move your .mturk_properties file to the top-level directory and input your access and secret keys.
*make test_java
*Every two minutes, the system will poll for results. All output from the program will be put in the output/ directory.

### Simulator

The SurveyMan is written in Python. It is designed as a testing environment for the application quality control metrics. The main entrypoint of the program is [launcher.py](https://github.com/etosch/surveyAutomation/blob/master/src/python/survey/launcher.py). To view options, type `python \relative\path\to\launcher.py help`.

To see how an example survey is constructed, run `python \relative\path\to\example_survey.py`. To test metrics, run `python \relative\path\to\metric-test.py`.


### Live Sytem

After installing the above, call `make .config`. This will download the appropriate jars from Amazon Mechnical Turk. Make sure you have a requester account with MTurk and have set some [keys](https://portal.aws.amazon.com/gp/aws/securityCredentials).

Then run `make test_java` to test whether surveys will post correctly to the Requester Sandbox.
