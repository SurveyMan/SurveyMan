#!/usr/bin/python
# regardless of OS, want to create a surveyman home dir and move
# params.properties tere
import os

home = os.path.expanduser("~")
surveyman = home + os.sep + "surveyman"
params = "params.properties"
customjs = "custom.js"
customcss = "custom.css"
if not os.path.exists(surveyman) :
    os.mkdir(surveyman)
os.rename(params, surveyman + os.sep + params)
os.rename(customjs, surveyman + os.sep + customjs)
os.rename(customcss, surveyman + os.sep + customcss)

