[![Build Status](https://travis-ci.org/SurveyMan/SurveyMan.png?branch=master)](https://travis-ci.org/SurveyMan/SurveyMan)

SurveyMan is a programming language and runtime system for designing, debugging, and deploying surveys on the web. This
repository implements the language, the static analyzer, and the dynamic analyzer. It also contains a simulator for
generating good and bad actors, according to an input policy.

````
________________               __________________           _____________________           _______________
|  survey.csv  |              /  SurveyMan Lang. \         /  SurveyMan analyzer \         | SurveyMan.out |
|  ________________   ---->   |  - Lexer         |  ---->  |  - Static Analyses  |  ---->  | _______________
|  |  survey.json |           |  - Parser        |         |  - Simulation       |     ----> |    Runner    |
|  |              |           |                  |         |  - Dynamic Analyses |         | |              |   
````

For the runtime system, see the [Runner repository]
(https://github.com/SurveyMan/Runner).  The SurveyMan runtime system is designed to be modular, so it can support a
variety of backend services.

The SurveyMan programming language is a [tabular language](https://github.com/etosch/SurveyMan/wiki/Csv-Spec) that is
best written in a spreadsheet program. We do provide a progammatic interface in this respository.  We also have a
[Python library](https://surveyman.github.io/SMPy).

The SurveyMan language itself supports a large range of survey structures. Some features that are not directly supported
by the langauge can be implemented using customized code (especially Javascript). If you are not sure whether SurveyMan
can support a particular feature, please contact @etosch.

### Installation / Usage

There are two ways to install SurveyMan:

**Build from source**

Clone this repository and run `make package`. This will produce a jar called `surveyman-x.y.jar`. Run
`java -jar surveyman-x.y.jar` for usage.

**Maven Dependency**

```
<dependency>
    <groupId>edu.umass.cs</groupId>
    <artifactId>surveyman</artifactId>
    <version>1.6</version>
</dependency>
```

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

# License 

Copyright 2015 University of Massachusetts Amherst

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

