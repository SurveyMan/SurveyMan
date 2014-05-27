set -x
set -e
echo `pwd`
FILES=data/responses/phonology/*
srid=0
for f in $FILES
do
    i=$((${#f}-1))
    last=${f:$i:1}
    echo "starting srid: $srid"
    if [[ "$last" != "~" ]]
    then 
	srid=$(echo $(lein run -m system.mturk.response-converter --raw=$f --startId=$srid data/samples/phonology.csv) | rev | cut -d " " -f1 | rev)
    fi
done
mv results.csv data/results/phonology_results.csv
