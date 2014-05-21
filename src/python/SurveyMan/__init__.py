import os.path
import os
import shutil
import hashlib

full_path = os.path.realpath(__file__)
curPath, file = os.path.split(full_path)
#print curPath
relResourcePath = ""
##if os.path.isdir(os.path.join(curPath,"..\\..\\main\\resources")):
##    print 'found resources directory'
relResourcePath = curPath+"\\..\\..\\main\\resources"
#print relResourcePath

#print os.path.join(curPath,"resources")
if os.path.isdir(curPath+"\\resources"):
    #delete it, copy most recent version of resources
    shutil.rmtree(curPath+"\\resources")
#copy resources folder to python SurveyMan directory
shutil.copytree(relResourcePath,curPath+"\\resources")
        
__all__=["survey"]
