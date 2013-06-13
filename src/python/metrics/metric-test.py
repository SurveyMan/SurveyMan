# This is meant to be a test environment for metrics
# The script will read in arbitrary csvs and augment the list of real answers with adversarial responses. 
# We can then test various metrics on a series of test surveys

import csv, sys, os, path
from survey.objects import *
import random

__doc__=""" 
=================================================================
This module generates an example survey from survey response csvs. 
It then adds adversarial responses to the list of responses and 
applies the various metrics available to determine whether the 
responses are outliers.

Usage: takes arguments of the form arg=val.
This module requires at least an argument for the file name of 
the csv to be processed.

Other arguments include:
numq : the number of questions to process (default is all)
numr : the number of responses to process (default is all)
%rand : the percentage of random respondents, in comparison with
  the total number of responses specified by numr (default is 0.33)

Example call (from the project root)
python metrics/metric-test.py file=data/mySurvey.csv numr=100 numq=5 %rand=0.5
=================================================================
"""

def load(testsurvey):

    questions, answers = [None]*2
    responses = []

    with open(testsurvey, 'r') as surveyresponses: 
        csvreader, answers  = csv.reader(surveyresponses, delimiter=','), []
        #assigns int i to each item (row?) r in survey csv
        for (i, r) in enumerate(csvreader):
            if(i > int(numr)):
                break
            if (i==0): #if first iteration
                q = int(numq) or len(r)
                #creates q questions from csv row headers, puts then in questions
                questions = [Question(qtext, [], qtypes['radio']) for qtext in r[:q]]
            else: #if not first iteration
                answers.append(r[:q]) #add peoples' answers for each question
        for (i, question) in enumerate(questions): #go through questions and assign corresponding options
            question.options = list(set([ans[i] for ans in answers]))

    #append real survey responses to list of responses
    for response in answers:
         realResponse = SurveyResponse(questions, [[ans] for ans in response])
         realResponse.real=True
         responses.append(realResponse)
       
    #generate random survey responses based on real answers 
    
    for _ in range(int(round(percentrand*numr))):
        #go through questions and randomly select one of the possible options
        #create survey response
        randResponse = SurveyResponse(questions, [[random.choice(q.options)] for q in questions])
        randResponse.real=False
        responses.append(randResponse)

    # #mix real responses with random ones
    random.shuffle(responses)

    return responses


if __name__=='__main__':

    #load only first 10 responses (change?)
    argmap = {k:v for k,v in [arg.split("=") for arg in sys.argv[1:]]}
    numq = int(argmap.get('numq', False))
    numr = int(argmap.get('numr', False))
    percentrand = float(argmap.get('%rand', 0.33))

    if argmap.has_key('file'):
        responses = load(argmap['file'])
    else:
        raise Exception(__doc__)

    for r in responses:
        if r.real:
            print "REAL:", r.response
        else:
            print "RANDOM:", r.response
