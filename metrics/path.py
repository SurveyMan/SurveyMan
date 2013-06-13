import os, sys

folders = os.getcwd().split(os.sep)
project_root = None

if folders[-1] == 'metrics': 
    project_root = folders[:-1]
elif folders[-1] == 'SurveyMan':
    project_root = folders
else:
    raise Exception('Please execute from the SurveyMan or metrics directory')

sys.path.append(os.sep.join(project_root))
    
    
