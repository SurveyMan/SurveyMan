#! /bin/bash                                                                                                          
set -e
echo "HAVE YOUR MTURK CREDENTIALS READY; YOU WILL BE PROMPTED FOR THEM MOMENTARILY."

# set lib folder
if [[ ! -d lib ]]; then
    mkdir lib
fi

# get clt
wget http://mturk.s3.amazonaws.com/CLTSource/aws-mturk-clt.tar.gz 
tar -xzf aws-mturk-clt.tar.gz
rm aws-mturk-clt.tar.gz
aws_folder=`ls | grep aws`

# move mturk jars to lib
cd $aws_folder/lib
for jar in $( find . | grep mturk.*jar ); do
    echo $jar
    mv $jar ../../lib/
done
cd ../..

# remove clt
rm -rf $aws_folder

# add keys to config file
if [[ ! -e .config ]]; then
    echo "Paste your access key id: "
    read k1
    echo "access_key=$k1" >> .config
    echo "Paste your secret access key: "
    read k2
    echo "secret_key=$k2" >> .config
fi
