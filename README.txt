 Before running this program, you should create a directory called surveyman in your home directory:
   
    mkdir ~/surveyman

Then create access in the AWS mangement console found here: https://console.aws.amazon.com/iam/home?#security_credential
You will be asked to download these keys. They will be downloaded to your Downloads directory under the name "rootkey.csv".
This file will need to be moved and renamed to the surveyman home directory as "config:"

    mv ~/Downloads/rootkey.csv ~/surveyman/config

The first time you run this program, you must do so in this directory. The program will copy the default settings into your
surveyman directory. You may modify them from here; all subsequent runs will look for information in the surveyman directory. 
If you need to reset your system, simply delete all but the config file in the surveyman directory and run again from this zip
folder. An initial call might be:

    java -jar surveyman.jar data/linguistics/test3.csv : true

The first argument is the relative path to the survey csv. The second argument is the csv field delimiter. The third indicates
whether or not you want to expire any existing old Human Intelligence Tasks on Amazon.

The default setting is to post surveys to the Mechanical Turk sandbox. A URL will be printed in your console after posting.

The params.properties file contains information about your HIT. You can change the reward amount, the number of samples, and
whether to post to the Mechanical Turk sandbox or live system in this file.

Once you have run the initial survey, you may run this file from anywhere.   

Questions? Email etosch@gmail.com.
