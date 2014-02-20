from jsonschema import validate

def validateJSON(schemaFile, jsonFile):
    s = open(schemaFile)
    schema = s.read()
    #print schema;
    s.close;
    j = open(jsonFile, "r")
    json = j.read()
    #print json
    j.close()
    schema = eval(schema)
    validate(json, )

def main():
    validateJSON("survey-temp.JSON","survey1.JSON");

if  __name__ =='__main__':
    main()
