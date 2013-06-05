# This is meant to be a test environment for metrics
# The script will read in arbitrary csvs and augment the list of real answers with adversarial responses. 
# We can then test various metrics on a series of test surveys

import csv, sys
from questionnaire import * 

def load(censusfile):

    questions, answers = [None]*2

    with open(censusfile, 'r') as census:
        censusreader, answers  = csv.reader(census, delimiter=','), []
        for (i, q) in enumerate(censusreader):
            if (i==0):
                answers = [[] for _ in q]
                questions = [Question(qtext, [], 1) for qtext in q]
            else:
                for (j, a) in enumerate(q):
                    answers[j].append(a)
        for (i, q) in enumerate(questions):
            q.options = list(set(answers[i]))
            
    del answers
    return questions


if __name__=='__main__':
    print load(sys.argv[1])[3]
