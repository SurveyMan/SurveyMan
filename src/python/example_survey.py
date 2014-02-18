from survey.objects import *
import json

"""
This module provides an example of how to construct a questionnaire in Python.
Questionnaires can be saved by calling jsonize and dumping their contents.
Jsonized surveys can be reused, manipulated, and sent via RPC to another service.
"""

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

filename = 'jsonized_survey.txt'

f = open(filename, 'w')
f.write(json.dumps(survey.jsonize(), sort_keys = True, indent = 4))
f.close()

if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1:
        filename = sys.argv[1]
    print "See "+filename+" for a jsonzied survey."
