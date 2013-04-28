# "agents" are just functions

import random
from questionnaire import qtypes, q1, q2, q3, q4, survey

def global_safe_insert(lst):
    def helper(crap, index, item):
        if len(lst) <= index :
            lst.extend([None for x in range(index-len(lst)+1)])
            lst[index]=item
    return helper

class Agent:
    def take_survey(self, survey):
        #returns tuple of responses to question object
        return [(q, self.respond(q)) for q in survey.questions]
    def respond(self, q):
        return [q.options[0]]

class LazyAgent(Agent):
    pass

class RandAgent(Agent):
    opts = []
    __safe_insert = global_safe_insert(opts)
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

      
class CollegeStudent(Agent):
    opts = {q1.quid : lambda q : [q.options[1]]
            , q2.quid : lambda q : [opt for opt in q.options if opt.otext=="Democrat"]
            , q3.quid : lambda q : q.options
            , q4.quid : lambda q : [q.options[len(q.options)-5]]
        }
    __safe_insert = global_safe_insert(opts)
    def respond(self, q):
        return self.opts[q.quid](q)
        

rando = RandAgent()
lazy = LazyAgent()
college = CollegeStudent()

if __name__ == "__main__" :
    print rando.take_survey(survey)
    print lazy.take_survey(survey)
    print college.take_survey(survey)
