set -x
set -e
lein run -m system.mturk.response-converter data/responses/wage_survey/HITResultsFor2GZHRRLFQ0OT6OX3OP8OKEGRDBJHQ4.csv data/tests/wage_survey.csv
lein run -m system.mturk.response-converter data/responses/wage_survey/HITResultsFor3N7PQ0KLI51X8QZZ5Y3V9BR4UH13EK.csv data/tests/wage_survey.csv
lein run -m system.mturk.response-converter data/responses/wage_survey/HITResultsFor3E9ZFLPWOY4L4T8ZL60A16E0T3MIXH.csv data/tests/wage_survey.csv
