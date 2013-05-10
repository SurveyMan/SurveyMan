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
    
    def __init__(self, questions):
        self.questions = questions

    def jsonize(self):
        return [q.jsonize() for q in self.questions]
        
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
            

class Option :
    
    def __init__(self, otext, inid = None):
        self.otext = str(otext)
        self.oid = uuid1()
        self.inid = inid
        self.oindex = None
        
    def jsonize(self):
        return {"oid" : self.oid.hex, "otext" : self.otext}

    def __repr__(self):
        return self.otext
        
    def __str__(self):
        return self.otext

class Question : 

    def __init__(self, qtext, options, qtype, shuffle=False):
        assert(qtype >= 0 and qtype < len(qtypes))
        self.quid = uuid1()
        self.qtext = qtext
        self.options = []
        optloc = 0
        for option in options:
            opt = Option(option)
            opt.oindex = optloc
            optloc += 1
            self.options.append(opt)
        assert(all([isinstance(o, Option) for o in self.options]))
        self.ok2shuffle = shuffle
        self.qtype=qtype

    def jsonize(self):
        return {"quid" : self.quid.hex
                , "qtext" : self.qtext
                , "ok2shuffle" : self.ok2shuffle
                , "qtype" : self.qtype
                , "options" : [o.jsonize() for o in sorted(self.options, key = lambda opt : opt.oindex)]}

    def reset_oindices(self):
        for (oindex, option) in enumerate(self.options):
            option.oindex=oindex
        
    def __repr__(self):
        val = self.qtext+"\n"
        # for i in range(len(self.options)):
        #     val = val + "\t" + str(i) + ". " + str(self.options[i]) + "\n"
        return val
        
    def __str__(self):
        val = self.qtext+"\n"
        for i in range(len(self.options)):
            val = val + "\t" + str(i) + ". " + str(self.options[i]) + "\n"
        return val
