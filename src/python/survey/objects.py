# basic idea of the questionnaire is to provide the first pass of representing 
# the survey

# todo:
# - Survey needs a function to proffer the survey and get a response. This response
#   should be returned as a some kind of collection of data containing the actual responses,
#   the question ids (since they might be shuffled) and maybe a confidence or something
# - typed list?

from uuid import uuid1
from UserDict import UserDict
import random
import sys

qtypes = {"freetext" : 0 , "radio" : 1 , "check" : 2 , "dropdown" : 3}

class idDict(UserDict):

    def __init__(self, valtype):
        self.str_valtype=valtype
        self.data={}
        self.__add = self.__add_fn(valtype)

    def __add_fn(self,str_valtype):
        self.str_valtype
        def __add_aux(uid, val):
            assert uid.__class__.__name__=='UUID', "uid is of type %s; should be UUID" % type(uid).__name__
            assert val.__class__.__name__==str_valtype, "val is of type %s; should be %s" % (type(val).__name__, str_valtype)
            self.data[uid]=val
        return __add_aux

    def __setitem__(self, k, v):
        self.__add(k, v)

    def __getitem__(self, k):
        return self.data[k]

    def __getstate__(self):
        return self.data

class SurveyResponse:
    
    def __init__(self, *args):
        self.response=[]
        if len(args)==1:
            self.response = args[0]
        elif len(args)==2:
            for tupe in zip(args[0], args[1]):
                #tupe is (question, option_list)
                self.response.append(tupe)
        else:
            raise Exception("Too many arguments (must be 1 or 2)")

    def __iter__(self):
        for r in self.response: yield r

    def __getitem__(self, i):
        return self.response[i]

    def __len__(self):
        return len(self.response)
        
    def jsonize(self):
        return [{ "question" : question.jsonize(), "option_list" : [opt.jsonize() for opt in option_list] } \
                for (question, option_list) in self.response]

    def sorted(self):
        return sorted([(question, sorted(opt_list, key = lambda opt : opt.oid)) for (question, opt_list) in self.response], key = lambda (q, _) : q.quid)

    #changes responses to bitstrings, converts bitstrings to integers, returns list of integers representing responses to each question
    #may only work on checkboxes/dropdowns/radio buttons
    def toNumeric(self):
        numeric_responses = []
        self.sorted()
        for (question, option_list) in self.response:
            bitstring = ['0']*len(question.options)
            for option in option_list:
                index = question.options.index(option)
                if(index>-1):
                    bitstring[index]='1'
            base10=int(''.join(bitstring), 2)
            numeric_responses.append(base10*1.0)

        #print numeric_responses
            
        return numeric_responses
    
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
            responses.append(self.read_response().strip(), question.quid)
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
        self.qindex=-1

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
