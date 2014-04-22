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
    global QUESTION, OPTIONS, RESOURCE, BLOCK, EXCLUSIVE, RANDOMIZE, FREETEXT, ORDERED, BRANCH, CORRELATE
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
        exclusive = row[EXCLUSIVE].lower()
        if exclusive == "" or exclusive in trues:
            return qtypes['radio']
        elif exclusive in falses:
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
    r = 1
    # CSV entries are 1-indexed
    for row in reader:
        if header:
            ordered_headers = [s.upper() for s in row]
            set_header_positions(ordered_headers)
            header = False
        else:
            q = row[QUESTION]
            opt = Option(row[OPTIONS])
            opt.sourceCellId = (r, OPTIONS+1)
            if q == "": #or q == question.qtext:
                question.options.append(opt)
                question.sourceRows.append(r)
            else:
                if question.qtext:
                    questions.append(question)
                question = Question(q, [opt], get_qtype(row))
                question.sourceCellId = (r*1, QUESTION+1)
                question.sourceRows = [r]
                question.blockId = row[BLOCK]
                if RANDOMIZE != -1 and row[RANDOMIZE] in falses:
                    question.ok2shuffle = False
        r += 1
    print(r, "rows processed in", filename)
    # clean up and add the last question
    questions.append(question)
    return Survey(questions)
        

if __name__ == "__main__":
    filename = 'data/SMLF5.csv'
    survey = parse(filename)
    survey.filename = filename
    print(survey.questions[2])
