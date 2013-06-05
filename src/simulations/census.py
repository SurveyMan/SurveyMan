# This is meant to be a test environment for metrics
# The script will read in arbitrary csvs and augment the list of real answers with adversarial responses. 
# We can then test various metrics on a series of test surveys

import csv, sys, os
project_root = os.getcwd().split('\\')[:-2]
sys.path.append('\\'.join(project_root)+'\\src\\survey\\')
from questionnaire import *
import random

def load(censusfile):

    questions, answers = [None]*2

    with open(censusfile, 'r') as census: 
        censusreader, answers  = csv.reader(census, delimiter=','), []
        #assigns int i to each item (row?) q in survey excel sheet
        for (i, q) in enumerate(censusreader):
            if(i > 100):
                break
            if (i==0): #if first iteration
                answers = [[] for _ in q] #array of answer arrays for each question?
                #creates q questions from excel row headers, puts then in questions
                questions = [Question(qtext, [], qtypes['radio']) for qtext in q]
            else: #if not first iteration
                for (j, a) in enumerate(q): #assign int j to each answer in row q
                    answers[j].append(a) #add peoples' answers for each question
        for (i, q) in enumerate(questions): #go through questions and assign corresponding options
            q.options = list(set(answers[i]))
            
    del answers
    return questions


if __name__=='__main__':
    #load only first 10 responses (change?)
    questions = load("C:\Python27\dev\surveyAutomation\data\ss11pwy.csv")[:10]
   # print load(sys.argv[1])[3]

    responses=[]

    for q in questions:
        print len(q.options)

    #generate 10 (for now) random survey responses based on answers 
    #of previous first 10 respondents
    for x in range(1,11):
        #go through questions and randomly select one of the possible options
        #create survey response
        oneResponse = SurveyResponse((q, [{True : random.choice(q.options), False : []}[len(q.options)>0]]) for q in questions if q.options>0)
        responses += [oneResponse]
        
        
        
       

       
