set -e
export PYTHONPATH=`pwd`/src/:`pwd`/src/survey:`pwd`/src/simulations
data_dir=`pwd`/data
if [[ -d $data_dir ]]; then
    if [[ `ls *.dict` != "" ]]; then
	rm data/*.dict
    fi
    if [[ -e data/outliers.txt ]]; then
	rm data/outliers.txt
    fi
else 
    mkdir $data_dir
fi
echo "Testing system"
#echo "Generating test questions..."
#python src/examples.py
echo "Testing simulation..."
#python src/survey/launcher.py simulation=`pwd`/src/simulations/simulation.py stop=stop_condition display=True outdir=$data_dir
if [[ ! -e $data_dir/ss11pwy.csv ]]; then
    cd data 
    wget http://www2.census.gov/acs2011_5yr/pums/csv_pwy.zip
    unzip csv_pwy.zip
    cd ..
fi
echo "Running metric test environment"
python src/survey/metrics.py
#python src/simulations/census.py data/ss11pwy.csv
echo "done"
