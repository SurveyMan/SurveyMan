# "agents" are just functions

import random
from questionnaire import qtypes, q3, survey

def global_safe_insert(lst):
    def helper(crap, index, item):
        if len(lst) <= index :
            lst.extend([None for x in range(index-len(lst)+1)])
            lst[index]=item
    return helper

class RandAgent:
    opts = []
    __safe_insert = global_safe_insert(opts)
    def take_survey(self, survey):
        #returns tuple of responses to question object
        return [(self.respond(q), q.quid) for q in survey.questions]
    def respond(self, q):
        return self.opts[q.qtype](q)
    def __init__(self):
        self.__safe_insert(qtypes["freetext"]
                           , lambda _ :  "junk")
        self.__safe_insert(qtypes["radio"]
                           , lambda q : [random.choice(q.options)])
        self.__safe_insert(qtypes["check"]
                           , lambda q : [opt for (choose, opt) in \
                                         zip(bin(random.choice(range(1,pow(2, len(q.options)))))[2:].zfill(len(q.options))
                                             ,q.options) if \
                                         choose=='1'])
        self.__safe_insert(qtypes["dropdown"]
                           , lambda q : [random.choice(q.options)])
      
class CollegeStudent:
    opts = []
    __safe_insert = global_safe_insert(opts)
#    def respond_

a1 = RandAgent()

if __name__ == "__main__" :
    print a1.take_survey(survey)
