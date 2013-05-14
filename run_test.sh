set -e
export PYTHONPATH=`pwd`/src/:`pwd`/src/survey
echo $PYTHONPATH
echo "Testing system"
cd src
echo "Generating test questions..."
python examples.py
cd ..
echo "Testing simulation..."
python src/survey/launcher.py simulation=`pwd`/src/simulation.py stop=stop_condition display=True #outformat=pickle.dumps
echo "done"
