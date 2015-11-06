cases="phonology prototypicality wage_survey"
classifiers="lpo entropy log_likelihood stacked cluster"

for case in $cases; do
    for classifier in $classifiers; do
        echo "`cat ${case}_${classifier}.txt | grep Response`  (${case}-${classifier})"
    done
done
