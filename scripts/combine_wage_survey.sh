set -x
set -e
srid=$(echo $(lein run -m system.mturk.response-converter --raw=data/responses/wage_survey/HITResultsFor2GZHRRLFQ0OT6OX3OP8OKEGRDBJHQ4.csv --startId=0 data/samples/wage_survey.csv) | rev | cut -d " " -f1 | rev)

srid=$(echo $(lein run -m system.mturk.response-converter --raw=data/responses/wage_survey/HITResultsFor3N7PQ0KLI51X8QZZ5Y3V9BR4UH13EK.csv --startId=$srid data/samples/wage_survey.csv) | rev | cut -d " " -f1 | rev)

lein run -m system.mturk.response-converter --raw=data/responses/wage_survey/HITResultsFor3E9ZFLPWOY4L4T8ZL60A16E0T3MIXH.csv --startId=$srid data/samples/wage_survey.csv

mv results.csv data/results/wage_survey_results.csv
