from questionnaire import *
import json

q1 = Question("What is your age?"
              , ["< 18", "18-34", "35-64", "> 65"]
              , qtypes["radio"])              

q2 = Question("What is your political affiliation?"
              , ["Democrat", "Republican", "Indepedent"]
              , qtypes["radio"]
              , shuffle=True)

q3 = Question("Which issues do you care about the most?"
              , ["Gun control", "Reproductive Rights", "The Economy", "Foreign Relations"]
              , qtypes["check"]
              ,shuffle=True)

q4 = Question("What is your year of birth?"
              , [x+1910 for x in range(90)]
              , qtypes["dropdown"])

survey = Survey([q1, q2, q3, q4])

filename = 'survey.txt'

f = open(filename, 'w')
f.write(json.dumps(survey.jsonize(), sort_keys = True, indent = 4))
f.close()

if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1:
        filename = sys.argv[1]
