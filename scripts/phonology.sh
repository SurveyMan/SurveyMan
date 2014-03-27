SURVEYMAN=`pwd`/..
export PYTHONPATH=`pwd`/../src/python
echo $PYTHONPATH
cd $PYTHONPATH
echo `pwd`
python evaluation/phonology.py $SURVEYMAN/data/SMLF5.csv ~/Desktop/phonology > $SURVEYMAN/output/phonology.txt
python evaluation/phonology.py $SURVEYMAN/data/SMLF5.csv ~/Desktop/phonology2 >> $SURVEYMAN/output/phonology.txt
python evaluation/phonology.py $SURVEYMAN/data/SMLF5.csv ~/Desktop/phonology3 >> $SURVEYMAN/output/phonology.txt
