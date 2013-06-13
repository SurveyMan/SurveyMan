"""
The survey module provides an environment to test and deploy surveys. 
Currently testing is not yet set up and will require specifying how to communicate with some kind of web service for surveys.
The testing environment in this module uses the metrics module, which has its own seperate unit testing.
User-defined behavior lives in the simulation module.
All other submodules used are defined here. 
See specific modules for documentation.
"""
__all__=['agents','launcher','objects']
