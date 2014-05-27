set -x 
set -e
lein run -m system.mturk.response-converter --raw=data/responses/prototypicality/HITResultsFor248YN5F3Q0JMOM48AHNYM49FAN5BF5.csv data/samples/prototypicality.csv
mv results.csv data/results/prototypicality_results.csv
