Installation 
==

OS X (tested for 10.8)
--
In order to run most of the programs here, you will to have the following installed:

* Python 2.7
* numpy
* scipy
* matplotlib

If you have not used scipy before, note that it uses BLAS, so make sure you have a fortran compiler installed. While the programs can be run without use of matplotlib by providing arguments to suppress plotting, numpy is essenential for the program.

Usage
--
surveyAutomation is designed to be used in a simulation environment and in a live, crowd-sourced environment. Currently on the simulation environment is implemented. The main entrypoint of the program is [launcher.py](https://github.com/etosch/surveyAutomation/blob/master/src/survey/launcher.py). To view options, type `python \path\to\launcher.py help`.

To see how an example survey is constructed, run `python example_survey.py`. 