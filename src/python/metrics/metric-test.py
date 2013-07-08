# This is meant to be a test environment for metrics
# The script will read in arbitrary csvs and augment the list of real answers with adversarial responses. 
# We can then test various metrics on a series of test surveys

import csv, sys, os, path
from survey.objects import *
import random
import numpy as np
import matplotlib.pylab as pl
import matplotlib.cm as cm

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

    questions, qtexts, answers = [[]]*3
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
                qtexts = r[:q]
            else: #if not first iteration
                answers.append(r[:q]) #add peoples' answers for each question

    for (i, question) in enumerate(qtexts): #go through questions and assign corresponding options
        questions.append(Question(question, list(set([ans[i] for ans in answers])), qtypes["radio"]))
        
    #append real survey responses to list of responses
    for response in answers:
         realResponse = SurveyResponse([(q, [opt for opt in q.options if opt.otext==o]) for (q, o) in zip(questions, response)])
         realResponse.real=True
         responses.append(realResponse)

    return responses

#generate box plot for answers to each question
def answerbox(responses):
    response_matrix = metrics.normalize_responses(metrics.responsematrix(responses))
    pl.boxplot(np.squeeze(np.asarray(response_matrix)))
    pl.title("Responses")
    pl.ylabel("Numeric representation of question answers")
    pl.xlabel("Question number")
    pl.show()

def answerscatter(responses):
    response_matrix = np.asarray(metrics.normalize_responses(metrics.responsematrix(responses)))
    fig=pl.figure()
    a=fig.add_subplot(111)
    x=[i for i in range(1, len(response_matrix[0])+1)]
    colors = cm.rainbow(np.linspace(0, 1, len(response_matrix)))
    for y, c in zip(response_matrix, colors):
        a.scatter(x,y,color=c)
    a.figure.show()
     
#generate random survey responses based on real answers 
def mixrandom(responses, percentrand):
    questions = [q for (q,r) in responses[0]]
    for _ in range(int(round(percentrand*numr))):
        #go through questions and randomly select one of the possible options
        #create survey response
        tup=[]
        for q in questions:
            num = "****"#random.randrange(1,20)
            fakeoption=Option(num)
            fakeoption.oindex= q.options[-1].oindex+1
            q.options.append(fakeoption)
            tup.append((q, [fakeoption]))
        randResponse = SurveyResponse([(q,r) for (q, r) in tup])
        randResponse.real=False
        responses.append(randResponse)

    # #mix real responses with random ones
    random.shuffle(responses)

    for q in questions:
        print q.qtext
        print q.options
        print "\r\n"

    return responses  


if __name__=='__main__':

    #load only first 10 responses (change?)
    argmap = {k:v for k,v in [arg.split("=") for arg in sys.argv[1:]]}
    numq = int(argmap.get('numq', False))
    numr = int(argmap.get('numr', False))
    percentrand = float(argmap.get('prand', 0.33))

    if argmap.has_key('file'):
        responses = load(argmap['file'])
    else:
        raise Exception(__doc__)
    #responses=load("C:\Python27\dev\SurveyMan\data\ss11pwy.csv")
    responses=mixrandom(responses, percentrand)
    real, fake = 0, 0
    for r in responses:
         if r.real:
             print "REAL:", r.response
             real+=1
         else:
             print "RANDOM:", r.response
             fake+=1
    

    # try using metrics
    import metrics
    #dist = metrics.kernal(responses)
    #metrics.test()
    #metrics.test.perQ(dist)
    #metrics.test.perS(dist)

##    intervals=metrics.qentropyintervals(responses)
##    print intervals
##    outliers=[]
##    response_matrix=metrics.responsematrix(responses)
##    for i, q in enumerate(response_matrix.transpose()):
##       entropy = metrics.questionentropy(np.squeeze(np.asarray(q)))
##       print entropy
##       if(entropy< intervals[i][0] or entropy>intervals[i][1]):
##           outliers.append(responses[0].response[i][0])
##    print outliers

    #answerbox(responses)
    #answerscatter(responses)

    import entropybootstrap

    entropybootstrap.bootstrap(responses)
    outliers=entropybootstrap.bootstrap.returnOutliers()
    print outliers
    for o in outliers:
        if o.real:
            print "Real respondent falsely classified"
        else:
            print "Random respondent correctly classified"

    
