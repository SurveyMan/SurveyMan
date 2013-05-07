import questionnaire
import json

f = open('/Users/etosch/dev/surveyAutomation/src/survey.txt', 'w')
f.write(json.dumps(questionnaire.Survey([questionnaire.q1, questionnaire.q2, questionnaire.q3, questionnaire.q4]).jsonize(), sort_keys = True, indent = 4))
f.close()
