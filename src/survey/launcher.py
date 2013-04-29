from questionnaire import *
from agents import *

__doc__ = "Maybe put some of this modules comments in here."

# quid_dict = {QUID, question text}
# For each question, create a new entry in the database that looks like this:
# oid_dict = {OID, option object}
# counts = {quid, {oid 1:# of respondants, oid 2:# of respondants, oid 3:# of respondants}, ...}
class idDict(dict):
    id_dict = None
    __add = None
    def __add_fn(self, str_valtype):
        def add(uid, val):
            assert uid.__class__.__name__=='UUID', "uid is of type %s; should be UUID" % type(uid).__name__
            assert val.__class__.__name__==str_valtype, "val is of type %s; should be %s" % (type(val).__name__, str_valtype)
            self.id_dict[uid]=val
        return add
    def __setitem__(self, k, v):
        self.__add(k, v)
    def __getitem__(self, k):
        return self.id_dict[k]
    def __init__(self, valtype, init_dict={}):
        self.id_dict = init_dict
        self.__add = self.__add_fn(valtype)

quid_dict = idDict('Question')
oid_dict = idDict('Option')
counts_dict = idDict('idDict')

displayp = False

def is_lazy(responses):
    # this is a stupid way of doing things; should look at this more
    # would like to do something involving entropy that's sensitive to location
    # an even simpler start would be to throw out responses that have a run
    # whose likelihood lies outside a 95% confidence interval
    all([oindices==responses[0] for oindices in [o for (q, o) in responses]])

def remove_bots(responses):
    # we will eventually mark questions that need to be consistent.
    # consistency logic should be baked in to the app
    pass

def ignore(responses):
    # throw out bad responses
    return is_lazy(responses) # or remove_bots(responses)

# Database is of the form:
# counts = {quid, {oid 1:# of respondants, oid 2:# of respondants, oid 3:# of respondants}, ...}
def display(quid_to_question, oid_to_option, database):
    def stop_condition():
        # this is hard-coded for now
        # we can play with this for testing purposes, but 
        # eventually the laucher will handle convergence
        # and this function will just check to see if we have reached the
        # end of the list yet (or end of the generator or stream or whatever)
        total_processed > 10
    pass

def launch():
    (num_takers, total_takers) = (100, 0)
    (qs, agent_list) =  ([q1, q2, q3, q4], [CollegeStudent() for _ in range(num_takers)])
    survey = Survey(qs)
    ##### initialize dictionaries #####
    for question in qs:
        quid_dict[question.quid] = question
        for option in question.options:
            oid_dict[option.oid] = option
        counts_dict[question.quid] = idDict('int', init_dict={option.oid : 0 for option in question.options})
    ##### where the work is done #####
    while (total_takers < num_takers):
        survey.shuffle()
        # get one taker's responses
        responses = agent_list[total_takers].take_survey(survey) 
        if not ignore(responses):
            for (question, option_list) in responses:
                for option in option_list:
                    counts_dict[question.quid][option.oid] += 1
            total_takers = total_takers+1
            if displayp:
                display(quid_dict, oid_dict, counts)
    ##### sanity check #####
    for (quest, opts) in counts_dict.iteritems():
        q=quid_dict[quest]
        num_ans=sum(opts.values())
        if q.qtype==qtypes["radio"] or q.qtype==qtypes["dropdown"]: 
            # radio buttons have once choice each
            assert(num_ans==num_takers)
        elif q.qtype==qtypes["check"]:
            assert(num_ans>=num_takers and num_ans<=num_takers*len(q.options))
        else:
            raise Exception("Unsupported question type: %s" % [k for (k, v) in qtypes.iteritems() if v==q.qtype][0])
    return [quid_dict, oid_dict, counts_dict]


if __name__=="__main__":
    import sys
    if len(sys.argv)> 1 and sys.argv[1]=="display":
        import numpy as np
        import matplotlib.pyplot as plt
        displayp=True
    launch()
