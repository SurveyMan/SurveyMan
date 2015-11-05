#!/bin/sh

set -e

# Run dynamic analyses on the old data, looking at bot detection in
# particular

case_studies="phonology prototypicality wage_survey"
classifiers="lpo stacked cluster entropy log_likelihood"

for case in $case_studies; do
    echo "Computing dynamic analyses for ${case}.csv..."
    for classifier in $classifiers; do
        output="${case}_${classifier}"
        err_file=err_${case}_${classifier}
        echo "...Using ${classifier}..."
        java -jar surveyman.jar --analysis=dynamic --alpha=0.05 --outputfile=${output}.txt --inputformat=csv --classifier=${classifier} --smoothing=false --resultsfile=../${case}_results.csv ../../samples/${case}.csv 2> $err_file
        if [[ -s $err_file ]]
        then
            echo "Failure."
        else
            rm $err_file
        fi
        mv logs/SurveyMan.log logs/${output}.log
    done
    echo "Done with dynamic analysis for ${case}"
done

