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
    responses = []

    with open(censusfile, 'r') as census: 
        censusreader, answers  = csv.reader(census, delimiter=','), []
        #assigns int i to each item (row?) q in survey excel sheet
        for (i, q) in enumerate(censusreader):
            if(i > 5):
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

    #append real survey responses to list of responses
    for x in range (0, len(answers[0])):
         realResponse = SurveyResponse([],[])  
         for y in range (0, len(questions)): 
             realResponse.response.append((questions[y],answers[y][x]))
         realResponse.real=True
         responses+=[realResponse]
       
    
    
    #generate 50 random survey responses based on real answers 
    
    for x in range(1,3):
        #go through questions and randomly select one of the possible options
        #create survey response
        randResponse = SurveyResponse((q, random.choice(q.options)) for q in questions)
        randResponse.real=False
        responses+=[randResponse]

    #mix real responses with random ones
    random.shuffle(responses)

    return responses


if __name__=='__main__':
    #load only first 10 responses (change?)
    responses = load("C:\Python27\dev\surveyAutomation\data\ss11pwy.csv")
   # print load(sys.argv[1])[3]

    for r in responses:
        if r.real:
            print "REAL:"
        else:
            print "RANDOM:"
        #for k in r.response:
            #print k
           
       

    

    
    
        
        
        
       

       
