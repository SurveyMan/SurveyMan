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
plot_dict = idDict('AxesSubplot')

displayp = False

def is_lazy(responses):
    # this is a stupid way of doing things; should look at this more
    # would like to do something involving entropy that's sensitive to location
    # an even simpler start would be to throw out responses that have a run
    # whose likelihood lies outside a 95% confidence interval
    return all([oindices==responses[0] for oindices in [o for (q, o) in responses]])

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
        absolute_ordering = sorted(quid_dict[q.quid].options, key=lambda opt : opt.oid)
        if q.qtype==qtypes["radio"] or q.qtype==qtypes["dropdown"] :
            for (i, oid) in enumerate([opt.oid for opt in absolute_ordering]) :
                if oid==opts[0].oid:
                    return i
            assert 1==2, "oid %s not found" % opts[0].oid
        elif q.qtype==qtypes["check"] : 
            index = int("".join([{True : '1', False : '0'}[opt in opts] for opt in absolute_ordering]),2)
            assert index > 0 and index <= pow(2, len(q.options)), "option ids %s not found" % [o.oid for o in opts]
            return index
        else : 
            raise Exception("Question type not found")
    def display_updated_image(quid):
        fig = plt.figure(1)
        sub = plot_dict[quid]
        sub.cla()
        ct = sum(freq_dict[quid])
        pdf = [f / ct for f in freq_dict[quid]]
        for (x,y) in enumerate(pdf):
            sub.vlines(x, 0, y)
        sub.set_xlabel('%s' % q.qtext)
        sub.set_ylabel('%s' % ('percent'))
        # the 'M' will be a 'D' if we ever have something continuous, e.g. scrolling bar thing
        sub.set_title('P%sF of %s' % ('M', 'something'))
        plt.ion()
        plt.show()
    def update_pdf(q, opts):
        n = sum(freq_dict[q.quid])
        updateindex = __get_absolute_index_value(q, opts)
        freq_dict[q.quid]=[freq + {updateindex : 1.0}.get(oindex, 0.0) for (oindex, freq) in enumerate(freq_dict[q.quid])]
        #return true if successful
        return n+1==sum(freq_dict[q.quid])
    assert update_pdf(q, opts)
    display_updated_image(q.quid)

def launch():
    (num_takers, total_takers) = (100, 0)
    (qs, agent_list) =  ([q1, q2, q3, q4], [CollegeStudent() for _ in range(num_takers)])
    survey = Survey(qs)
    def initial_freq_dict(q):
        if q.qtype==qtypes["radio"] or q.qtype==qtypes["dropdown"]:
            return [0 for _ in q.options]
        elif q.qtype==qtypes["check"]:
            return [0 for _ in range(pow(2, len(q.options)))]
    ##### initialize dictionaries #####
    for (i, q) in enumerate(qs, 1):
        quid_dict[q.quid] = q
        for o in q.options:
            size = len(oid_dict)
            oid_dict[o.oid] = o
            if not counts_dict.get(q.quid, None):
                counts_dict[q.quid] = idDict('int')
            counts_dict[q.quid][o.oid] = 0
        freq_dict[q.quid] = initial_freq_dict(q)
        if displayp:
            from  matplotlib.pyplot import subplot,figure
            fig = figure(1)
            fig.subplots_adjust(hspace=2.0, wspace=2.0)
            sqrt = int(pow(len(qs), 0.5))
            sub = fig.add_subplot(int(str(sqrt*sqrt+1)+str(sqrt)+str(i)))
            plot_dict[q.quid] = sub
    ##### where the work is done #####
    while (total_takers < num_takers):
        survey.shuffle()
        # get one taker's responses
        responses = agent_list[total_takers].take_survey(survey) 
        if not ignore(responses):
            for (question, option_list) in responses:
                for option in option_list:
                    counts_dict[question.quid][option.oid] += 1
                if displayp:
                    display(question, option_list)
            total_takers += 1
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
    return [quid_dict, oid_dict, counts_dict, freq_dict, plot_dict]


if __name__=="__main__":
    import sys
    if len(sys.argv)> 1 and sys.argv[1]=="display":
        displayp=True
    launch()
    print "done"
    while True:
        pass
