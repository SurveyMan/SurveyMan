# basic idea of the questionnaire is to provide the first pass of representing 
# the survey

# todo:
# - get ride of makeblahblahblah -> we're not going to have classes for Radio, etc.
# - create Survey class, which is a list of questions, where each instance shuffles 
#   the questions themselves and if the questions have shuffle=True, shuffles the options.
# - Survey needs a function to proffer the survey and get a response. This response
#   should be returned as a some kind of collection of data containing the actual responses,
#   the question ids (since they might be shuffled) and maybe a confidence or something

from uuid import uuid1
import random
import sys

qtypes = {"freetext" : 0 , "radio" : 1 , "check" : 2 , "dropdown" : 3}

class Survey :
    questions = []
    
    def __init__(self, questions):
        self.questions = questions
        
    def shuffle(self):
        random.shuffle(self.questions)
        for question in self.questions:
            if (question.ok2shuffle):
                random.shuffle(question.options)
                question.reset_oindices()
                
    def show_question(self, q):
        print q
        return
                
    def read_response(self):
        s = sys.stdin.readline()
        return s
            
    def take_survey(self):
        responses = []
        for question in self.questions:
            self.show_question(question)
            responses.append((self.read_response().strip(), question.quid))
        return responses
      
            
    def post_survey(self):
        return questions
      
    def get_survey(responses, ids, confidence):
        return

class Option :
    oid = uuid1()
    otext = ""
    oindex = None
    
    def __init__(self, otext):
        self.otext = str(otext)
        
    def __repr__(self):
        return self.otext
        
    def __str__(self):
        return self.otext

class Question : 
    quid = uuid1()
    qtype = ""
    qtext = ""
    options = []
    ok2shuffle = False
    def reset_oindices():
        for (oindex, option) in enumerate(options):
            option.oindex=oindex
    def __init__(self, qtext, options, qtype, shuffle=False):
        self.qtext = qtext
        self.options = []
        optloc = 0
        for option in options:
            opt = Option(option)
            opt.oindex = optloc
            optloc += 1
            self.options.append()
        self.ok2shuffle = shuffle
        self.qtype=qtype
        
    def __repr__(self):
        val = self.qtext+"\n"
        for i in range(len(self.options)):
            val = val + "\t" + str(i) + ". " + str(self.options[i]) + "\n"
        return val
        
    def __str__(self):
        val = self.qtext+"\n"
        for i in range(len(self.options)):
            val = val + "\t" + str(i) + ". " + str(self.options[i]) + "\n"
        return val

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
              , [x+1910 for x in range(80)]
              , qtypes["dropdown"])

survey = Survey([q1, q2, q3, q4])

if __name__ == "__main__" : 
    survey.shuffle()
    survey.take_survey()
