SurveyMan
=====
SurveyMan is an offshoot of the [Automan] (https://github.com/plasma-umass/AutoMan) project. Like Automan, SurveyMan automates human computation tasks. Unlike Automan, SurveyMan amortizes the cost of multiple human intelligence tasks by submitting tasks in groups. These groups resemble a survey, hence the name.

Usage 
---
To use SurveyMan for query-based research, see the [Build Page] (wiki/Build) for end-user installation instructions. Example input files are included in the build. For more information on generating your own input files, see the [CSV Spec] (wiki/CSV_Spec) wiki page.

Development
---
SurveyMan is a Maven project with Scala dependencies. Fork this repo, checkout, and load this repo as a Maven project into your favorite IDE (or suffer through JDEE). The main entry point is [Runner.run] (https://github.com/etosch/SurveyMan/blob/master/src/main/java/system/Runner.java).
