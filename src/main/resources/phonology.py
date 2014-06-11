import csv, os, sys
from scipy.stats import spearmanr
import matplotlib.pyplot as plt
import numpy as np
from uuid import uuid1
import random
import math
import httplib


def frequency(survey, responses):
    """ responses needs to be a single list"""
    freqs = {q : {o : 0 for o in q.options} for q in survey.questions}
    for response in responses:
        for q in response.keys():
            o = response[q][0]
            freqs[q][o] += 1
    return freqs

def empirical_prob(fmap):
    probs = {q : {o : 0 for o in list(fmap[q].keys())} for q in list(fmap.keys())}
    for q in list(fmap.keys()):
        total = sum(fmap[q].values()) # should be equal to the total number of respondents if we don't permit breakoff
        for o in list(fmap[quid].keys()):
            probs[q][o] = float(fmap[q][o]) / float(total)
    return probs

def log_likelihood(response, pmap):
    likelihood = 0.0
    for q in list(response.keys()):
        o = response[q]
        likelihood -= math.log(pmap[q][o])
    return likelihood

def make_bootstrap_interval(survey, responses, alpha, parametric=True):
    B = 2000
    pmap = empirical_prob(frequency(survey, responses))
    log_likelihoods = [log_likelihood(r, pmap) for r in responses]
    bootstrap_sample = [sorted(np.random.choice(log_likelihoods, len(responses), replace=True)) for _ in range(B)]
    if parametric:
        bs_mean = np.average([np.average(samp) for samp in bootstrap_sample])
        bs_std = np.std([np.average(samp) for samp in bootstrap_sample])
        return (bs_mean - 2*bs_std, bs_mean + 2*bs_std)
    else:
        aindex = int(math.floor((alpha / 2.0)*len(responses)))
        bindex = int(math.floor((1.0 - (alpha / 2.0))*len(responses)))
        return (np.average([s[aindex] for s in bootstrap_sample]), np.average([s[bindex] for s in bootstrap_sample]))
    

def get_least_popular_options(survey, responses, diff):
    fmap = frequency(survey, responses)
    least_popular = {}
    for q in list(fmap.keys()):
        optfreqs = list(fmap[q].items())
        optfreqs = sorted(optfreqs, key = lambda t : t[1])
        for (i, j) in [(k, k+1) for k in range(len(optfreqs)-1)]:
            if optfreqs[i][1] < optfreqs[j][1]*diff:
                least_popular[q] = optfreqs[:j]
                break
    print("Number of questions with least popular options : %d" % len([opts for opts in list(least_popular.values()) if len(opts)!=0]))
    return least_popular

def get_mu(survey, least_popular_options):
    expectation = 0
    for q in survey.questions:
        if q in least_popular_options:
            expectation += float(len(least_popular_options[q])) / float(len(q.options))
    return expectation

#print "Expected number of least popular questions a bot should answer: %d" % get_mu(s1, get_least_popular_options(s1, bots+nots, delta))

def num_least_popular(response, lpo):
    n = 0
    for q in lpo.keys():
        if q not in response:
            # in case this person didn't answer this question
            continue
        opt = response[q][0]
        if opt in [o[0] for o in lpo[q]]:
            n += 1
    return n

def bot_lazy_responses_unordered(survey, responses, delta, diff):
    lpo = get_least_popular_options(survey, responses, diff)
    mu = get_mu(survey, lpo)
    alpha = pow(math.e, (- delta * mu) / (2 + delta))
    print("Expect %f least popular answers for a bot; bots will answer fewer than this with probability %f" % (mu, alpha))
    classifications = []
    for response in responses:
        n = num_least_popular(response, lpo)
        classifications.append((response, n >= round(mu), n))        
    return classifications

def bot_lazy_responses_ordered(survey, responses, alpha, workerids):
    # create mapping of total number of options
    stages = {}
    for question in survey.questions:
        m = len(question.options)
        if m not in stages:
            stages[m] = []
        stages[m].append(question)
    classifications = []
    for (i, response) in enumerate(responses):
        workerid = workerids[i]
        this_classification = []
        for (m, questions) in stages.items():
            if m > 1 and len(questions) > 1:
                print([(j, m-j-1) for j in range(int(math.floor(m/2)))])
                for (hi, lo) in [(j, m - j - 1) for j in range(int(math.floor(m/2)))]:
                    hict = len([opos for (_, (_, _, opos)) in [(q, tupe) for (q, tupe) in response.items() if q in questions] if int(opos) == hi])
                    print("Number at stage %d, position %d: %d" % (m, hi, hict))
                    loct = len([opos for (_, (_, _, opos)) in [(q, tupe) for (q, tupe) in response.items() if q in questions] if int(opos) == lo])
                    print("Number at stage %d, position %d: %d" % (m, lo, loct))
                    n = hict + loct
                    if n == 0:
                        continue
                    mu = 0.5 * n
                    delta = math.sqrt((3 * math.log(alpha)) / (- mu))
                    x = {True : hict, False : loct}[hict > loct]
                    b = (1 + delta) * mu
                    c = x >= (1 + delta) * mu
                    print("If %d >= %f : Bot? %s, workerid: %s, amazon reviews?: %s\n" % (x, b, c, workerid, amazon(workerid)))
                    this_classification.append((response, x >= (1 + delta) * mu, n))
        # policy for deciding bots for each question length? what's
        # the probability of making an incorrect classification?
        # will probably want a discount factor over these things
        # for now, just return that it's a bot if one is true
        classifications.append((response, any([t[1] for t in this_classification]), workerid))
    return classifications

def amazon(workerid):
    h = httplib.HTTPConnection('www.amazon.com')
    h.request('GET', '/gp/pdp/profile/' + workerid)
    r = h.getresponse()
    return r.status != 404

def get_disagreeing_correlations(classifications, responses):
    # flag respondents who disagree on any of the strongly correlated
    # answers
    classifications = []
 


def make_plot(data, title, filename):
    false_neg = [np.average([a[0] for a in stuff]) for stuff in data]
    false_neg_min = np.array([min([a[0] for a in stuff]) for stuff in data])
    false_neg_max = np.array([max([a[0] for a in stuff]) for stuff in data])
    false_pos = [np.average([a[1] for a in stuff]) for stuff in data]
    false_pos_min = np.array([min([a[1] for a in stuff]) for stuff in data])
    false_pos_max = np.array([max([a[1] for a in stuff]) for stuff in data])
    xaxis = [stuff[0][2] for stuff in data]
    fig, ax = pyplot.subplots()
    ax.set_title(title)
    ax.errorbar(xaxis, false_pos, yerr=[false_pos - false_pos_min, false_pos_max - false_pos], c='y', fmt='o', label='Humans identified as bots')
    ax.errorbar(xaxis, false_neg, yerr=[false_neg - false_neg_min, false_neg_max - false_neg], c='r', fmt='o', label='Bots identified as humans')
    ax.legend(numpoints=1)
    pyplot.axis([0,1.0,0,1.2])
    pyplot.ylabel("Percent misclassified")
    pyplot.xlabel("Percent bots in the population")
    #pyplot.show()
    fig.savefig(filename)

# def now_with_my_thing(clusters):
#     n=10
#     m=5
#     s1 = Survey([Question("", [Option("") for _ in range(m)], qtypes["radio"], shuffle=True) for _ in range(n)])
#     data = []
#     for i in range(1,10):
#         sub_data = []
#         for j in range(100):
#             bots, nots = sample(s1, make_profiles(s1, clusters), 100, i/10.0)
#             classifications = classify2(s1, bots, nots, 1.0, 0.75)
#             (false_negative, false_positive) = analyze_classifications(classifications)
#             sub_data.append((float(false_negative) / float(len(bots)) , float(false_positive) / float(len(nots)), i/10.0))
#         data.append(sub_data)
#     return data

def run_this():
    make_plot(now_with_my_thing(1), "Bots answer >= expected min questions", "balls_n_bins_1_cluster.png")
    make_plot(generate_bots_v_humans(classify_humans_as_outliers, 1), "Humans are outliers; 1 cluster of humans", "humans_outliers_1_cluster.png")
    make_plot(generate_bots_v_humans(classify_bots_as_outliers, 1), "Bots are outliers; 1 cluster of humans", "bots_outliers_1_cluster.png")

# now simulate breakoff 
# hypothesis : when breakoff is permitted, people will stop around a clustered point according to their personal utility function
# we believe that this point follows a normal distribution. 
# if we marginalize for position, we should be left with the probability of abandonment for a particular question

# take a set of profiles, let one of the questions be offensive. assign a level of offense for each profile. 
# since the cost of abandonment for web surveys is very low, numerous factors impact the positional preferences for abadonment.
# model the behavior we believe happens on mechanical turk: bots will abandon the survey after the first question, whereas 
# people will abandon upon seeing an offensive question or when the survey has exceeded their tolerance.
# people don't know how long the survey is; this will impact their calculus for abandonment. we expect to see different behavior in 
# the case where progress bars are represented. however, we believe that statistically the behavior will remain the same - it just may
# increase the user's tolerance for length
def make_breakoff_profiles(s, n):
    profiles = make_profiles(s,n)
    print(len(profiles))
    for profile in profiles:
        offensive = np.random.choice(list(profile.keys()), 1, replace=True)[0]
        for quid in list(profile.keys()):
            # i have some marginal probability of abandoning at every question, but let my probability of abandoning at a particular question be very high
            oid, prob = profile[quid]
            oprob = random.random()
            vprob = random.random()*0.1 
            if quid==offensive:
                print(offensive, oprob , vprob)
            profile[quid] = {'opt' : (oid, prob), 'breakoff' : quid == offensive and oprob or vprob}
    print(len(profiles))
    return profiles

def generate_samples(s, profile_list, size, percent_bots):
    print(len(profile_list), size, percent_bots)
    num_bots = int(math.floor(size * percent_bots))
    num_people = size - num_bots
    bot_responses = []
    not_responses = []
    for _ in range(num_bots):
        # bots always break off at the first question
        firstQ = np.random.choice([q for q in s.questions], 1, replace=True)[0]
        bot_responses.append({firstQ.quid : (np.random.choice(firstQ.options, 1, replace=True)[0].oid, 0)})
    for _ in range(num_people):
        # sample from the profile list ; profiles can have repeats
        profile = np.random.choice(profile_list, 1, replace=True)[0]
        response = {}
        for (i, q) in enumerate(np.random.choice(s.questions, len(s.questions), replace=False)):
            # see if we should grab the preferred response
            (oid, likelihood) = profile[q.quid]['opt']
            if random.random() < likelihood:
                response[q.quid] = (oid, i)
            else:
                # randomly sample from the remaining options
                other_opts = [o for o in q.options if o.oid != oid]
                response[q.quid] = (np.random.choice(other_opts, 1, replace=True)[0].oid, i)
            if random.random() < profile[q.quid]['breakoff']:
                break
        not_responses.append(response)
    print(len(bot_responses), len(not_responses))
    return (bot_responses, not_responses)

def breakoff_frequency_by_question(survey, responses):
    breakoff = {q.quid : 0 for q in survey.questions}
    for response in responses:
        questions_by_index = sorted(list(response.items()), key = lambda x : int(x[1][1]))
        assert(int(questions_by_index[-1][1][1])+1==len(questions_by_index))
        breakoff[questions_by_index[-1][0]] += 1
    print(breakoff)
    return breakoff

def breakoff_frequency_by_position(survey, responses):
    # 0-indexed
    breakoff = {i : 0 for i in range(len(survey.questions))}
    for response in responses:
        breakoff[len(response)-1] += 1
    print(breakoff)
    return breakoff


def get_interval(samp, alpha):
    B = 2000
    bootstrap_sample = [sorted(np.random.choice(samp, len(samp), replace=True)) for _ in range(B)]
    bootstrap_means = [np.average(samp) for samp in bootstrap_sample]
    bootstrap_mean = np.average(bootstrap_means)
    bootstrap_std = np.std(bootstrap_means)
    return (bootstrap_mean - 2.0*bootstrap_std, bootstrap_mean + 2.0*bootstrap_std)
    


def identify_breakoff_questions(survey, responses, alpha):
    fmap1 = breakoff_frequency_by_position(survey, responses)
    fmap2 = breakoff_frequency_by_question(survey, responses)
    bad_positions = []
    bad_questions = []
    positions = [thing[1] for thing in list(fmap1.items()) if thing[0]!=len(survey.questions)-1] # counts at position
    questions = list(fmap2.values()) # counts at question
    _, b = get_interval(positions, alpha*2)
    _, d = get_interval(questions, alpha*2)
    print("Statistically Significant Breakoff at total #/responses >", b)
    print("Statistically Significant Breakoff when #/final responses for a question is >", d)
    for position in list(fmap1.keys()):
        if position != len(survey.questions)-1:
            if fmap1[position] > b:
                bad_positions.append({"position" : position, "score" : fmap1[position]})
    for questionid in list(fmap2.keys()):
        if fmap2[questionid] > d:
            bad_questions.append({"question" : questionid, "score" : fmap2[questionid]})
    return (bad_positions, bad_questions)

qtypes = {"freetext" : 0 , "radio" : 1 , "check" : 2 , "dropdown" : 3}

class idDict(dict):

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
        return sorted([(question, sorted(opt_list, key = lambda opt : opt.oid)) for (question, opt_list) in self.response], key = lambda q__ : q__[0].quid)

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

    def get_question(self, quid):
        for q in self.questions:
            if q.quid == quid:
                return q
        raise ValueError(str('No question with id', quid))

    def jsonize(self):
        return [q.jsonize() for q in self.questions]
        
    def shuffle(self):
        random.shuffle(self.questions)
        for question in self.questions:
            if (question.ok2shuffle):
                random.shuffle(question.options)
                question.reset_oindices()
                
    def show_question(self, q):
        print(q)
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

    def __init__(self, qtext, options, qtype, shuffle=True):
        assert(qtype >= 0 and qtype < len(qtypes))
        self.quid = uuid1()
        self.qtext = qtext
        self.options = []
        optloc = 0
        for option in options:
            # opt = Option(option)
            # opt.oindex = optloc
            option.oindex = optloc
            optloc += 1
            # self.options.append(opt)
            self.options.append(option)
        assert(all([isinstance(o, Option) for o in self.options]))
        self.ok2shuffle = shuffle
        self.qtype=qtype
        self.qindex=-1
        self.branchTo=None

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

# positions of the headers
QUESTION, OPTIONS, RESOURCE, BLOCK, EXCLUSIVE, RANDOMIZE, FREETEXT, ORDERED, BRANCH, CORRELATE = [None]*10
trues = ['true', 't', 'y', 'yes', '1']
falses = ['false', 'f', 'n', 'no', '0']

with open("src/main/resources/spec.properties") as fp:
    default_headers = {}
    for line in fp.readlines():
        try:
            (h, htype) = line.split("=")
        except ValueError:
            continue
        default_headers[h.strip()] = htype.strip()

def pos(lst, item, default=-1):
    try:
        return lst.index(item)
    except ValueError:
        return default

def set_header_positions(ordered_headers):
    global QUESTION, OPTIONS, RESOURCE, BLOCK, EXCLUSIVE, RANDOMIZE, FREETEXT, ORDERED, BRANCH, CORRELATE
    QUESTION = pos(ordered_headers, 'QUESTION')
    OPTIONS = pos(ordered_headers, 'OPTIONS')
    RESOURCE = pos(ordered_headers, 'RESOURCE')
    BLOCK = pos(ordered_headers, 'BLOCK')
    EXCLUSIVE = pos(ordered_headers, 'EXCLUSIVE')
    RANDOMIZE = pos(ordered_headers, 'RANDOMIZE')
    FREETEXT = pos(ordered_headers, 'FREETEXT')
    ORDERED = pos(ordered_headers, 'ORDERED')
    BRANCH = pos(ordered_headers, 'BRANCH')
    CORRELATE = pos(ordered_headers, 'CORRELATE')
    if QUESTION == -1 or OPTIONS == -1 :
        raise ValueError('Survey must contain at least QUESTION and OPTIONS columns')

def get_qtype(row):
    if FREETEXT != -1 and row[FREETEXT].lower() in trues:
        return qtypes['freetext']
    elif EXCLUSIVE != -1:
        exclusive = row[EXCLUSIVE].lower()
        if exclusive == "" or exclusive in trues:
            return qtypes['radio']
        elif exclusive in falses:
            return qtypes['check']
        else:
            raise ValueError('Unrecognized value in the EXCLUSIVE column: ' + row[EXCLUSIVE])
    else:
        return qtypes['check']

def parse(filename):
    reader = csv.reader(open(filename, 'rU'))
    header = True
    questions = []
    question = Question(None, [], 0)    
    r = 1
    # CSV entries are 1-indexed
    for row in reader:
        if header:
            ordered_headers = [s.upper() for s in row]
            set_header_positions(ordered_headers)
            header = False
        else:
            q = row[QUESTION]
            opt = Option(row[OPTIONS])
            opt.sourceCellId = (r, OPTIONS+1)
            if q == "": #or q == question.qtext:
                question.options.append(opt)
                question.sourceRows.append(r)
            else:
                if question.qtext:
                    questions.append(question)
                question = Question(q, [opt], get_qtype(row))
                question.sourceCellId = (r, QUESTION+1)
                question.sourceRows = [r]
                if RANDOMIZE != -1 and row[RANDOMIZE] in falses:
                    question.ok2shuffle = False
        r += 1
    print(r, "rows processed in", filename)
    # clean up and add the last question
    questions.append(question)
    return Survey(questions)

universal_headers = ['HitId','HitTitle','Annotation','AssignmentId','WorkerId','Status','AcceptTime','SubmitTime']

def get_survey(source_csv):
    return parse(source_csv)


def load_from_dir (dirname, survey):
    # model responses as lists, rather than SurveyResponse objects, as
    # in the evaluation namespace

    qrows_lookup = {}
    orows_lookup = {}
    for question in survey.questions:
        for row in question.sourceRows:
            assert(row not in qrows_lookup)
            qrows_lookup[row] = question
        for o in question.options:
            (row, _) = o.sourceCellId
            assert(row not in orows_lookup)
            orows_lookup[row] = o

    header = True
    responses = []
    for filename in os.listdir(dirname):
        if 'csv' not in filename:
            continue
        reader = csv.reader(open(dirname+"/"+filename, "rU"))
        # the user response - should have entries for each question
        response = {}
        for row in reader:
            if header:
                headers = row
                header = False
            else:
                response['WorkerId'] = row[headers.index('WorkerId')]
                response['AssignmentId'] = row[headers.index('AssignmentId')]
                response['Answers'] = {}
                answers = row[len(universal_headers):]
                # add actual answers - will be a list of ids
                for ans in answers:
                    try:
                        (joid, qpos, opos) = ans.split(';')
                    except ValueError:
                        continue
                    (_, r, c) = joid.split("_")
                    q = qrows_lookup[int(r)]
                    o = orows_lookup[int(r)]

                    # The asserts here caused some weirdness - o changed.
                    #print(o, o.otext)
                    #assert(o.oid in [o.oid for o in q.options])
                    #assert(q not in response['Answers'])
                    #print(o, o.otext, qpos, opos, ans)

                    if 'definitely' in o.otext:
                        assert( opos=='0' or opos=='3' )
                    if 'probably' in o.otext:
                        assert( opos=='1' or opos=='2')
                    response['Answers'][q] = (o, qpos, opos)
        if len(response) == 0:
            continue
        responses.append(response)
        header = True
    return responses

#correlation
def get_corr_for_suffix(suffix, responses):

    retval = {} # tuple of questions that maps to spearmanr

    for q1 in survey.questions:
        
        if q1.quid in word_quid_map:
            (w1, s1) = word_quid_map[q1.quid]
            if s1 != suffix:
                continue
        else: 
            continue

        retval[q1] = {}

        for q2 in survey.questions:
            
            if q2.quid in word_quid_map:
                (w2, s2) = word_quid_map[q2.quid]
                if s2 != suffix:
                    continue
            else: 
                continue

            q1aleft = s1.startswith('a')
            q2aleft = s2.startswith('a')
            obs1 = []
            obs2 = []
            coding = {'definitely' : { True : 1, False : 4 },
                      'probably' : { True : 2, False : 3}}

            for response in responses:
                if q1 in response and q2 in response:
                    q1resp = response[q1]
                    q2resp = response[q2]
                else:
                    continue
                (adj1, chunk1) = q1resp[0].otext.split(' ')
                (adj2, chunk2) = q2resp[0].otext.split(' ')
                (w11, s11) = chunk1.split('-')
                (w22, s22) = chunk2.split('-')
                assert(w1==w11)
                assert(w2==w22)
                obs1.append(coding[adj1][q1aleft])
                obs2.append(coding[adj2][q2aleft])
                
            retval[q1][q2] = spearmanr(obs1, obs2)
        
    return retval

def make_subplot(ax, data, column_labels, row_labels, title):

    heatmap = ax.pcolor(data, cmap=colormap)
    
    ax.set_xticks(np.arange(data.shape[0])+0.5, minor=False)
    ax.set_yticks(np.arange(data.shape[1])+0.5, minor=False)
    
    ax.invert_yaxis()
    ax.xaxis.tick_top()

    ax.set_xticklabels(row_labels, minor=False, rotation=90)
    ax.set_yticklabels(column_labels, minor=False)

    ax.set_ylim([len(row_labels), 0])
    ax.set_xlim([0, len(column_labels)])
    
    ax.set_xlabel(title)

def get_data(correlation_data):
    data = list(correlation_data.items())
    data.sort(key = lambda tupe : keyfn(tupe[0]))
    for (i, (q1 , m)) in enumerate(data):
        data[i] = (q1, list(m.items()))
        data[i][1].sort(key = lambda tupe : keyfn(tupe[0]))
    return data



# will want to sort the words by end vowel
if __name__ == "__main__":
    # sort the survey questions by their word's last letters
    def keyfn(q):
        if q.quid in word_quid_map: 
            return word_quid_map[q.quid][0][::-1]
        else:
            return '0'

    #gitDir = '/Users/etosch/dev/SurveyMan-public/'
    gitDir = os.getcwd()
    source = sys.argv[1] 
    hitDir = sys.argv[2] 

    colormap = plt.cm.cool

    survey = get_survey(source)
    responses = load_from_dir(hitDir, survey)
    print("Total number of responses", len(responses))
    print("Total number of unique respondents", len(set([r['WorkerId'] for r in responses])))

    word_quid_map = {}
    for q in survey.questions:
        otext = q.options[0].otext
        if ' ' in otext:
            (qual, compword) = otext.split(' ')
            assert(qual=='definitely')
            word_quid_map[q.quid] = compword.split('-')

    # remove non-native english speakers
    q_native_speaker = [q for q in survey.questions if 7 in q.sourceRows][0]
    o_native_speaker = [o for o in q_native_speaker.options if o.otext == 'Yes'][0]
    responses = [r for r in responses if q_native_speaker in r['Answers'] and r['Answers'][q_native_speaker][0] == o_native_speaker]
    print("Total number of native speaker responses:", len(responses))

    # data for plotting with minimal filters
    prelim_thon = get_data(get_corr_for_suffix('thon', [ r['Answers'] for r in responses]))
    make_subplot(plt.subplot(1,2,1) \
                 , np.array([[spear for (q2, (spear, p)) in corrs] for (_, corrs) in prelim_thon]) \
                 , [word_quid_map[q.quid][0] for q in [qq for (qq, _) in prelim_thon]] \
                 , [word_quid_map[qqq.quid][0] for (qqq, _) in prelim_thon[1][1]] \
                 , "")

    prelim_licious = get_data(get_corr_for_suffix('licious', [ r['Answers'] for r in responses]))
    make_subplot(plt.subplot(1,2,2) \
                 , np.array([[spear for (q2, (spear, p)) in corrs] for (_, corrs) in prelim_licious]) \
                 , [word_quid_map[q.quid][0] for q in [qq for (qq, _) in prelim_licious]] \
                 , [word_quid_map[qqq.quid][0] for (qqq, _) in prelim_licious[1][1]] \
                 , "")

    #plt.show()
    fig = plt.gcf()
    fig.set_size_inches(8,6)
    plt.savefig("correlation1", dpi=100, pad_inches=0.5)

    # remove repeaters
    workers = [r['WorkerId'] for r in responses]
    unique_workers = [r['WorkerId'] for r in responses if len([s for s in workers if s == r['WorkerId']])==1]
    responses = [r for r in responses if r['WorkerId'] in unique_workers]
    print("Total number of unique native English speaking respondents:", len(responses))
        
            
    # previous bot classification is too aggressive
    classifications = bot_lazy_responses_ordered(survey, [r['Answers'] for r in responses] , 0.1, [r['WorkerId'] for r in responses])
    responses = [ (r, workerid) for (r, isBot, workerid) in classifications if not isBot ]
    botsorlazies = [ (r, workerid) for (r, isBot, workerid) in classifications if isBot ]
    print("Total number of non-(bots or lazies):", len(responses))
    amazonreviews = [ amazon(r[1]) for r in botsorlazies ]
    print("% bots having no amazon reviews:", (len([foo for foo in amazonreviews if not foo]) * 1.0) / len(amazonreviews))
    amazonreviews = [ amazon(r[1]) for r in responses ]
    print("% non bots or lazies having no amazon reviews:", (len([foo for foo in amazonreviews if foo]) * 1.0) / len(amazonreviews))

#     # thon plot
#     responses = [r[1] for r in responses]
#     thon = get_data(get_corr_for_suffix('thon', responses))
#     make_subplot(plt.subplot(1, 2, 1)
#                  , np.array([[spear for (q2, (spear, p)) in corrs] for (_, corrs) in thon])
#                  , [word_quid_map[q.quid][0] for q in [q for (q, _) in thon]]
#                  , [word_quid_map[q.quid][0] for (q, _) in thon[1][1]]
#                  , "-(a?)thon")

#     # licious plot
#     licious = get_data(get_corr_for_suffix('licious', responses))
#     make_subplot(plt.subplot(1,2,2)
#                  , np.array([[spear for (q2, (spear, p)) in corrs] for (_, corrs) in licious])
#                  , [word_quid_map[q.quid][0] for q in [q for (q, _) in licious]]
#                  , [word_quid_map[q.quid][0] for (q, _) in licious[1][1]]
#                  , "-(a?)licious")

#     #plt.show()
#     plt.savefig("correlation2")

#     #breakoff analysis
#     bad_pos, bad_q = identify_breakoff_questions(survey, [{ q.quid : (o.oid, a, b) for (q, (o, a, b)) in response.items() } for response in responses], 0.05)
#     bad_qs = [ q for q in survey.questions if q.quid in [bq['question'] for bq in bad_q]]
#     print(bad_pos)
#     for q in bad_q:
#         print(q['score'], word_quid_map[q['question']][0], word_quid_map[q['question']][1])
