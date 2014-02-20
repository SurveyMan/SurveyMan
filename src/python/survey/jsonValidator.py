from jsonschema import validate
import json

def validateJSON(schemaFile, jsonFile):
    s = open(schemaFile)
    schemaText = s.read();
    print schemaText
    dataform = str(schemaText).strip("'<>()[]\"` ").replace('\'', '\"')
    schema = json.loads(dataform);
    s.close;
    j = open(jsonFile, "r")
    jsonText = j.read()
    #print json
    j.close()
    validate(jsonText, schema)

def main():
    validateJSON("survey-temp.JSON","survey1.JSON");

if  __name__ =='__main__':
    main()
