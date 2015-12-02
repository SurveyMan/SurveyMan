# /bin/sh

# Run static and dynamic analyses on the old data, looking at bot
# detection in particular.
# What are the questions we want to ask:
# (1) How much do the classifiers vary?
# (2) What role does the structure play in the analyses?

rm *phonology*
rm *prototypicality*
rm *wage_survey*
rm -rf logs

case_studies="phonology prototypicality wage_survey"
classifiers="lpo stacked cluster entropy log_likelihood mahalanobis"

for case in $case_studies; do
    echo "Computing static analyses for ${case}.csv..."
    for classifier in $classifiers; do
        output="${case}_${classifier}"
        echo "...Using ${classifier}..."
        java -jar surveyman.jar --smoothing=false --outputfile=${output}.txt --classifier=${classifier} --analysis=static --granularity=0.33 --inputformat=csv ../../samples/${case}.csv 2> err_${case}_${classifier}
        if [[ -s err_${case}_${classifier} ]]
        then
            echo "Failure."
        else
            rm "err_${case}_${classifier}"
        fi
        mv logs/SurveyMan.log logs/${output}.log
    done
    echo "Done with static analysis for ${case}."
done
