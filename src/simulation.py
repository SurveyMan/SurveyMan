import random
from uuid import uuid1
from questionnaire import qtypes

def global_safe_insert(lst):
    def helper(index, item):
        if len(lst) <= index :
            lst.extend([None for x in range(index-len(lst)+1)])
            lst[index]=item
    return helper


def stop_condition():
    stop_condition.num_takers = 100
    stop_condition.total_takers += 1
    return stop_condition.num_takers == stop_condition.total_takers

stop_condition.total_takers = -1

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
              , [x+1910 for x in range(90)]
              , qtypes["dropdown"])

survey = Survey([q1, q2, q3, q4])

class Agent:
    def __init__(self):
        self.aid = uuid1()
    def take_survey(self, survey):
        #returns tuple of responses to question object
        return [(q, self.respond(q)) for q in survey.questions]
    def respond(self, q):
        return [q.options[0]]

class LazyAgent(Agent):
    pass

class RandAgent(Agent):
    def respond(self, q):
        return self.opts[q.qtype](q)
    def __init__(self):
        self.opts = []
        self.__safe_insert = global_safe_insert(self.opts)
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
    def __init__(self):
        self.opts = {q1.quid : lambda q : [q.options[1]]
                     , q2.quid : lambda q : [opt for opt in q.options if opt.otext=="Democrat"]
                     , q3.quid : lambda q : q.options
                     , q4.quid : lambda q : [random.choice([dob for dob in q.options if int(dob.otext)>1988 and int(dob.otext)<1995])]
                }
        self.__safe_insert = global_safe_insert(self.opts)
    def respond(self, q):
        return self.opts[q.quid](q)


def get_response(survey):
    get_response.agent_list = [CollegeStudent() for _ in range(100)]
    get_response.max = len(get_response.agent_list)
    def next():
        next.counter = 0
        retval = next.counter
        if next.counter == get_response.max:
            print "CRAP CRAP CRAP"
            next.counter = 0 
        else:
            next.counter += 1
        return retval
    return get_response.agent_list[next()].take_survey(survey) 
