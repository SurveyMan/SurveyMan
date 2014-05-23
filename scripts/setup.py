#!/usr/bin/python
# regardless of OS, want to create a surveyman home dir and move
# params.properties tere
import os
import shutil

home = os.path.expanduser("~")
resources = "src" + os.sep + "main" + os.sep + "resources"
surveyman = home + os.sep + "surveyman"
params = "params.properties"
customjs = "custom.js"
customcss = "custom.css"
if not os.path.exists(surveyman) :
    os.mkdir(surveyman)
shutil.copyfile(resources + os.sep + params, surveyman + os.sep + params)
shutil.copyfile(resources + os.sep + customjs, surveyman + os.sep + customjs)
shutil.copyfile(resources + os.sep + customcss, surveyman + os.sep + customcss)