java -cp surveyman-1.5-standalone.jar Report --report=static data/samples/phonology.csv
java -cp surveyman-1.5-standalone.jar Report --report=dynamic --results=data/results/phonology_results.csv data/samples/phonology.csv
java -cp surveyman-1.5-standalone.jar Report --report=static data/samples/prototypicality.csv
java -cp surveyman-1.5-standalone.jar Report --report=dynamic --results=data/results/prototypicality_results.csv data/samples/prototypicality.csv
java -cp surveyman-1.5-standalone.jar Report --report=static data/samples/wage_survey.csv
java -cp surveyman-1.5-standalone.jar Report --report=dynamic --results=data/results/wage_survey_results.csv data/samples/wage_survey.csv