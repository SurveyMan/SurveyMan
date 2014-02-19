#!/usr/bin/python
# regardless of OS, want to create a surveyman home dir and move
# params.properties tere
import os

home = os.path.expanduser("~")
surveyman = home + os.sep + "surveyman"
params = "params.properties"
if not os.path.exists(surveyman) :
    os.mkdir(surveyman)
os.rename(params, surveyman + os.sep + params)
