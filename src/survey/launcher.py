from questionnaire import *
from agents import *
from UserDict import UserDict

__doc__ = "Maybe put some of this modules comments in here."

# quid_dict = {QUID, question text}
# For each question, create a new entry in the database that looks like this:
# oid_dict = {OID, option object}
# counts = {quid, {oid 1:# of respondants, oid 2:# of respondants, oid 3:# of respondants}, ...}
class idDict(UserDict):
    def __init__(self, valtype):
        self.str_valtype=valtype
        self.data={}
        self.__add = self.__add_fn(valtype)
    def __add_fn(self,str_valtype):
        self.str_valtype
        def __add_aux(uid, val):
            print uid.__class__.__name__, val.__class__.__name__
            assert uid.__class__.__name__=='UUID', "uid is of type %s; should be UUID" % type(uid).__name__
            assert val.__class__.__name__==str_valtype, "val is of type %s; should be %s" % (type(val).__name__, str_valtype)
            self.data[uid]=val
        return __add_aux
    def __setitem__(self, k, v):
        self.__add(k, v)
    def __getitem__(self, k):
        return self.data[k]


quid_dict = idDict('Question')
oid_dict = idDict('Option')
counts_dict = idDict('idDict')
freq_dict = idDict('list')
plot_dict = idDict('LineCollection')

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
def display(q, opts):
    import numpy as np
    import matplotlib.pyplot as plt
    def __get_absolute_index_value(q, opts):
        absolute_ordering = sorted(quid_dict[q.quid].options, key=oid)
        if q.qtype==qtypes["radio"] or q.qtype==qtypes["dropdown"] :
            for (i, oid) in enumerate([opt.oid for opt in absolute_ordering]) :
                if oid==opts[0].oid:
                    return i
            assert 1==2, "oid %s not found" % opts[0].oid
        elif q.qtype==qtypes["checkbox"] : 
            index = int("".join([{True : '1', False : '0'}[opt in opts] for opt in absolute_ordering]),2)
            assert index > 0 and index <= pow(2, len(q.options)), "option ids %s not found" % [o.oid for o in opts]
            return index
        else : 
            raise Exception("Question type not found")
    def display_updated_image(quid):
        plot = plot_dict(quid)
        plt.cla()
        for (x,y) in enumerate(freq_dict[quid]):
            plt.vlines(x, 0, y)
        plt.xlabel('%s' % q.text)
        plt.ylabel('%s' % ('percent'))
        # the 'M' will be a 'D' if we ever have something continuous
        plt.title('P%F of %s' % ('M', 'something'))
        plt.legend()
        plt.ion()
        plt.show()
    def update_pdf(quid, opts):
        n = sum(pdf_dict[quid].values())
        updateindex = __get_absolute_index_value(quid, opts)
        pdf_dict[quid]=[(freq + {updateindex : 1.0}.get(oindex, 0.0)) / (n+1.0) for (freq, oindex) in freq_dict[quid]]
        #return true if successful
        return n+1==sum(pdf_dict[quid].values())
    def stop_condition():
        # this is hard-coded for now
        # we can play with this for testing purposes, but 
        # eventually the laucher will handle convergence
        # and this function will just check to see if we have reached the
        # end of the list yet (or end of the generator or stream or whatever)
        total_processed > 10
    update_pdf(q.quid, opts)
    display_updated_image(q.quid)

def launch():
    (num_takers, total_takers) = (100, 0)
    (qs, agent_list) =  ([q1, q2, q3, q4], [CollegeStudent() for _ in range(num_takers)])
    survey = Survey(qs)
    ##### initialize dictionaries #####
    print len(quid_dict.keys()), len(oid_dict.keys()), len(counts_dict.keys()), len(freq_dict.keys()), len(plot_dict.keys())
    for (i, q) in enumerate(qs, 1):
        quid_dict[q.quid] = q
        for o in q.options:
            assert len(set([o.oid for o in q.options]))==len(q.options)
            oid_dict[o.oid] = o
            if not counts_dict.get(q.quid, None):
                counts_dict[q.quid] = idDict('int')
            counts_dict[q.quid][o.oid] = 0
        freq_dict[q.quid] = []
        if displayp:
            from  matplotlib.pyplot import figure
            plot_dict[q.quid] = figure(i)
        print i, len(quid_dict.keys()), len(oid_dict.keys()), len(counts_dict.keys()), len(freq_dict.keys()), len(plot_dict.keys())
    ##### where the work is done #####
    while (total_takers < num_takers):
        survey.shuffle()
        # get one taker's responses
        responses = agent_list[total_takers].take_survey(survey) 
        if not ignore(responses):
            for (question, option_list) in responses:
                for option in option_list:
                    print counts_dict[question.quid]
                    counts_dict[question.quid][option.oid] += 1
            total_takers += 1
            if displayp:
                display(quid_dict, oid_dict, counts)
    ##### sanity check #####
    for (quest, opts) in counts_dict.iteritems():
        q=quid_dict[quest]
        num_ans=sum(opts.values())
        if q.qtype==qtypes["radio"] or q.qtype==qtypes["dropdown"]: 
            # radio buttons have once choice each
            assert num_ans==num_takers, "num_ans=%d num_takers=%d for question:\n %s" % (num_ans, num_takers, q)
        elif q.qtype==qtypes["check"]:
            assert num_ans>=num_takers and num_ans<=num_takers*len(q.options), "for qtype check, num_ans was %d for %d options" % (num_ans, len(q.options))
        else:
            raise Exception("Unsupported question type: %s" % [k for (k, v) in qtypes.iteritems() if v==q.qtype][0])
    return [quid_dict, oid_dict, counts_dict]


if __name__=="__main__":
    import sys
    if len(sys.argv)> 1 and sys.argv[1]=="display":
        displayp=True
    launch()
