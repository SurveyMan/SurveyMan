import questionnaire
import json

filename = 'survey.txt'

f = open(filename, 'w')
f.write(json.dumps(questionnaire.Survey([questionnaire.q1, questionnaire.q2, questionnaire.q3, questionnaire.q4]).jsonize(), sort_keys = True, indent = 4))
f.close()

if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1:
        filename = sys.argv[1]
