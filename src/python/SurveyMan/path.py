import os, sys

folders = os.getcwd().split(os.sep)
project_root = folders[:folders.index('SurveyMan')+1]

sys.path.append(os.sep.join(project_root+['src','python','SurveyMan']))
