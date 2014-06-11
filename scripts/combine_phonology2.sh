set -x
set -e
echo `pwd`
FILES=/Users/etosch/Desktop/experiment_data/phonology2
srid=0
for f in `ls $FILES`
do
    i=$((${#f}-1))
    last=${f:$i:1}
    echo $f
    echo "starting srid: $srid"
    if [[ "$last" != "~" ]]
    then 
	srid=$(echo $(lein run -m system.mturk.response-converter --raw=$FILES/$f --startId=$srid data/samples/phonology.csv) | rev | cut -d " " -f1 | rev)
    fi
done