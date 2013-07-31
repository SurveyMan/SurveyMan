# Usage

To use the SurveyMan Mechanical Turk poster, follow the instructions listed [here] (https://github.com/etosch/SurveyMan/wiki/Build).

# Development

### Installation 

In order to all of the programs here, you will to have the following installed:

* Python 2.7
* numpy
* scipy
* matplotlib
* maven 
* make

If you have not used scipy before, note that it uses BLAS, so make sure you have a fortran compiler installed. While the programs can be run without use of matplotlib by providing arguments to suppress plotting, numpy is essenential for the program.

### Simulator

The SurveyMan is written in Python. It is designed as a testing environment for the application quality control metrics. The main entrypoint of the program is [launcher.py](https://github.com/etosch/surveyAutomation/blob/master/src/python/survey/launcher.py). To view options, type `python \relative\path\to\launcher.py help`.

To see how an example survey is constructed, run `python \relative\path\to\example_survey.py`. To test metrics, run `python \relative\path\to\metric-test.py`.

# License 
CRAPL - see [CRAPL](CRAPL-LICENSE).


