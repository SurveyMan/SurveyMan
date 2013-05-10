from questionnaire import *
from UserDict import UserDict
import json, uuid, pickle
import numpy as np

__doc__ = """Execute launcher by calling : python launcher.py arg1=val1 arg2=val2 ...args are optional. Valid args include:
\t- display : a boolean indicating whether a graphical display of relevant statistics should be shown
\t- simulation : a path indicating the file to load that contains data for simulating a survey. See simulation.py for an example
\t- file : a path indicating the file containing a json representation of the survey to load
\t- stop : name of the stop condition to be used. Should either be a stop condition defined in launcher (references held in the global dictionary stop_dict) or one defined in the simulation file.
\t- outdir : The destination directory for computed data. Default is the current directory.
"""

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

    def __getstate__(self):
        return self.data



quid_dict = idDict('Question')
oid_dict = idDict('Option')
counts_dict = idDict('idDict')
freq_dict = idDict('list')
plot_dict = idDict('AxesSubplot')
stop_dict = idDict('function')
participant_dict = idDict('list')

displayp = False


def get_absolute_index_value(q, opts):
    absolute_ordering = sorted(quid_dict[q.quid].options, key = lambda opt : opt.oid)

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


def add_to_participant_dict(responses):
    absolute_ordering = sorted(responses, key = lambda (q, _) : q.quid)    
    pid = uuid1()
    participant_dict[pid] = [get_absolute_index_value(q, opts) for (q, opts) in absolute_ordering]


def is_lazy(responses):
    # this is a stupid way of doing things; should look at this more
    # would like to do something involving entropy that's sensitive to location
    # an even simpler start would be to throw out responses that have a run
    # whose likelihood lies outside a 95% confidence interval
    return all([oindices==responses[0] for oindices in [o for (q, o) in responses]])


def is_outlier(responses):
    # via hamming distance
    # single object is a collection of bitstrings
    # for a single question, we have a collection of options. differences will necessarily be 0 or 1 due to disjointness
    # get total responses as a bitstrings of agreement
    # KEEP BITSTRINGS FOR COMPARISON - should  be n^2 bitstrings total    
    population_bitstrings = []
    for ans1 in participant_dict.values():
        qbitstrings = []
        for ans2 in participant_dict.values():
            # each set of responses is represented by a single bitstring.             
            qbitstrings.append([{True : 0, False : 1}[i==j] for (i, j) in zip(ans1, ans2)])
        population_bitstrings.append(qbitstrings)
    population_hammings = [sum([sum(ans) for ans in part]) for part in population_bitstrings]
    # compare the input responses with all other responses to generate new set of bitstrings for this response's hamming dist
    absolute_ordering = [get_absolute_index_value(q, opts) for (q, opts) in sorted(responses, key = lambda (q, _) : q.quid)]
    these_hammings = sum([sum([{True : 0, False : 1}[i==j] for (i, j) in zip(absolute_ordering, ans)]) for ans in participant_dict.values()])
    return bootstrap(population_hammings)(these_hammings)

def is_bot(responses):
    # we will eventually mark questions that need to be consistent.
    # consistency logic should be baked in to the app
    return False


def ignore(responses):
    lazyp, outlierp, botp = is_lazy(responses), is_outlier(responses), is_bot(responses)
    # need to record whether or not a response should be discarded and discard that response
    # for later inspection
    if outlierp:
        with open((outdir or "") + 'outliers.txt', 'wa') as f:
            f.write((outdir or str)(responses))
    return outlierp


def classify_adversaries():
    pass


def bootstrap(samples, statistic = lambda x : sum(x) / (1.0 * len(x)), B  = 100):
    n = len(samples)
    if n < 5: return lambda x : False
    bootstrap_samples = [statistic([samples[i] for i in np.random.random_integers(0, n-1, size=n)]) for _ in range(B)]
    bootstrap_mean = sum(bootstrap_samples) / (1.0 * len(bootstrap_samples))
    bootstrap_sd = pow(sum([pow(bsstat - bootstrap_mean, 2.0) for bsstat in bootstrap_samples]) / (B - 1), 0.5)
    def retfun(test_value):
        eps = abs(test_value - bootstrap_mean)
        ninetyfive = 2 * bootstrap_sd
        return [eps > ninetyfive, eps, ninetyfive]
    return retfun
    #return [s for s in samples if abs(s - bootstrap_mean) > 2*bootstrap_sd]


# Database is of the form:
# counts = {quid, {oid 1:# of respondants, oid 2:# of respondants, oid 3:# of respondants}, ...}
def display(q, opts):

    import matplotlib.pyplot as plt
    
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
        updateindex = get_absolute_index_value(q, opts)
        freq_dict[q.quid]=[freq + {updateindex : 1.0}.get(oindex, 0.0) for (oindex, freq) in enumerate(freq_dict[q.quid])]
        #return true if successful
        return n+1==sum(freq_dict[q.quid])
    
    assert update_pdf(q, opts)
    display_updated_image(q.quid)
    plt.savefig(plt.figure(1))

def parse(input_file_name):
    f = open(input_file_name, 'r')
    json_obj = json.loads(f.read())
    f.close()
    qlist = [Question("", [""], 0) for _ in json_obj]
    for (i, q) in enumerate(json_obj):
        for (k, v) in q.iteritems():
            if k=='options':
                option_list = [Option(m['otext']) for m in v]
                for (index, option) in enumerate(option_list):
                    if v[index].has_key('oid'):
                        option.__dict__['oid'] = uuid.UUID(v[index]['oid'])
                v = option_list
            elif k=='quid':
                v = uuid.UUID(v)
            qlist[i].__dict__[k]=v    
    return Survey(qlist)


def launch(survey, stop_condition):

    qs = survey.questions

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
            if not counts_dict.has_key(q.quid):
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
    while not stop_condition():
        survey.shuffle()
        # get one taker's responses
        responses = get_response(survey) 
        add_to_participant_dict(responses)
        if not ignore(responses):
            for (question, option_list) in responses:
                for option in option_list:
                    counts_dict[question.quid][option.oid] += 1
                if displayp:
                    display(question, option_list)

    return { "quid_dict" : quid_dict
             ,"oid_dict" : oid_dict
             ,"counts_dict" : counts_dict
             ,"freq_dict" : freq_dict
             ,"plot_dict" : plot_dict 
             ,"participant_dict" : participant_dict }


if __name__=="__main__":
    import sys
    f, survey, stop, outdir, outformat, get_response = [None]*6
    argmap = { "display" : lambda x : "displayp = " + x
               ,"simulation" : lambda x : "execfile('" + x + "')\n"
               ,"file" : lambda x : "f = " + x
               ,"stop" : lambda x : "stop = " + x 
               ,"outdir" : lambda x : "outdir = " + x 
               ,"outformat" : lambda x : "outformat = " + x 
               , "responsefn" : lambda x : "get_response = " + x}
    for arg in sys.argv[1:]:
        k, v = arg.split("=")
        exec(argmap[k](v), globals())
    assert survey or f, "One of simulation or file args must be set"
    if f:
        def get_response(survey):
            raise "Live version not yet implemented"
    for (fname, d) in launch(survey or parse(f), stop or no_outliers).iteritems():
        with open((outdir or "") + fname + ".txt", 'w') as f:
            f.write((outformat or str)(d))
