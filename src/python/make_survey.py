# parses survey from source into the python object
# ignoring branching for now

from __init__ import *
from survey.objects import *
import csv

# positions of the headers
QUESTION, OPTIONS, RESOURCE, BLOCK, EXCLUSIVE, RANDOMIZE, FREETEXT, ORDERED, BRANCH, CORRELATE = [None]*10
trues = ['true', 't', 'y', 'yes', '1']
falses = ['false', 'f', 'n', 'no', '0']

with open(spec) as fp:
    default_headers = {}
    for line in fp.readlines():
        try:
            (h, htype) = line.split("=")
        except ValueError:
            continue
        default_headers[h.strip()] = htype.strip()

def pos(lst, item, default=-1):
    try:
        return lst.index(item)
    except ValueError:
        return default

def set_header_positions(ordered_headers):
    QUESTION = pos(ordered_headers, 'QUESTION')
    OPTIONS = pos(ordered_headers, 'OPTIONS')
    RESOURCE = pos(ordered_headers, 'RESOURCE')
    BLOCK = pos(ordered_headers, 'BLOCK')
    EXCLUSIVE = pos(ordered_headers, 'EXCLUSIVE')
    RANDOMIZE = pos(ordered_headers, 'RANDOMIZE')
    FREETEXT = pos(ordered_headers, 'FREETEXT')
    ORDERED = pos(ordered_headers, 'ORDERED')
    BRANCH = pos(ordered_headers, 'BRANCH')
    CORRELATE = pos(ordered_headers, 'CORRELATE')
    if QUESTION == -1 or OPTIONS == -1 :
        raise ValueError('Survey must contain at least QUESTION and OPTIONS columns')

def get_qtype(row):
    if FREETEXT != -1 and row[FREETEXT].lower() in trues:
        return qtypes['freetext']
    elif EXCLUSIVE != -1:
        if row[EXCLUSIVE] in trues:
            return qtypes['radio']
        elif row[EXCLUSIVE] in falses:
            return qtypes['check']
        else:
            raise ValueError('Unrecognized value in the EXCLUSIVE column: ' + row[EXCLUSIVE])
    else:
        return qtypes['check']

def parse(filename):
    reader = csv.reader(open(filename, 'rU'))
    header = True
    questions = []
    question = Question(None, [], 0)    
    for row in reader:
        if header:
            ordered_headers = [s.upper() for s in row]
            set_header_positions(ordered_headers)
            header = False
        else:
            q = row[QUESTION]
            print(q)
            if q == "" or q.qtext == question.qtext:
                question.options.append(Option(row[OPTIONS]))
            else:
                if question.qtext:
                    questions.append(question)
                question = Question(q, [Option(row[OPTIONS])], get_qtype(row))
                if RANDOMIZE != -1 and row[RANDOMIZE] in falses:
                    question.ok2shuffle = False
    return Survey(questions)
        

def load_survey (filename) :
    """Takes a csv as input and returns a Python Survey object."""
    lexed_map = parse(filename)
    

if __name__ == "__main__":
    print(QUESTION, OPTIONS)
    survey = parse('data/Ipierotis.csv')
    print(survey[0].qtext)
