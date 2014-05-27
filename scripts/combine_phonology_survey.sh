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
	srid=$(lein run -m system.mturk.response-converter $f data/samples/phonology.csv $srid)
    fi
done
