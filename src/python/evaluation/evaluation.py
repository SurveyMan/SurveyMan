import urllib, httplib
import csv, os, sys
import numpy as np
import matplotlib.pyplot as pyplot
import math
from survey.objects import *
# first evaluate bot detection
# want to compare expected number of catch questions, percent bots, ability to catch
# maybe want to vary by the number of profiles (corresponds to clusters)

def bias(q1, q2):
    pass

def entropy(survey, responses):
    emp_prob = empirical_prob(frequency(survey, responses))
    ent = 0.0
    for q in emp_prob.keys():
        for p in emp_prob[q].values():
            if p > 0:
                ent += p * (math.log(p) / math.log(2))
    return -ent



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
        for o in list(fmap[q].keys()):
            if total == 0:
                probs[q][o] = 0.0
            else:
                probs[q][o] = float(fmap[q][o]) / float(total)
    return probs

def log_likelihood(response, pmap):
    likelihood = 0.0
    for q in list(response.keys()):
        o = response[q][0]
        likelihood -= math.log(pmap[q][o])
    return likelihood

def ind_entropy(response, pmap):
    ent = 0.0
    for q in list(response.keys()):
        o = response[q][0]
        ent -= pmap[q][o] * math.log(pmap[q][o]) / math.log(2)
    return ent

def make_bootstrap_interval(survey, responses, alpha, method, stat=np.average, parametric=True):
    B = 2000
    pmap = empirical_prob(frequency(survey, responses))
    #stats = [method(r, pmap) for r in responses]
    bootstrap_sample = [np.random.choice(responses, len(responses), replace=True) for _ in range(B)]
    bootstrap_stat = [[method(r,pmap) for r in s] for s in bootstrap_sample]
    data = sorted([stat(bss) for bss in bootstrap_stat])
    if parametric:
        bs_mean = np.average([np.average(samp) for samp in bootstrap_stat])
        bs_std = np.std([np.average(samp) for samp in bootstrap_stat])
        return (bs_mean - 2*bs_std, bs_mean + 2*bs_std)
    else:
        aindex = int(math.floor((alpha / 2.0)*len(responses)))
        bindex = int(math.floor((1.0 - (alpha / 2.0))*len(responses)))
        return (data[aindex], data[bindex])
    

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

def bot_lazy_responses_entropy(survey, responses, alpha, worker_ids):
    emp_prob = empirical_prob(frequency(survey, responses))
    lo, hi = make_bootstrap_interval(survey, responses, alpha, ind_entropy, parametric=False)
    print "entropy bounds: " , hi, lo
    classifications = []
    for response in responses:
        ent = ind_entropy(response, emp_prob)
        print ent, len(response), ent > hi 
        classifications.append((response, ent > hi, ent))
    return classifications


def detect_variants(q1, q2, responses):
    pass
    

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
        stuff_to_print = {0: "", 1 : "", 2 : "", 3 : "", 4 : 0}
        for (m, questions) in stages.items():
            if m > 1 and len(questions) > 1:
                #print([(j, m-j-1) for j in range(int(math.floor(m/2)))])
                for (hi, lo) in [(j, m - j - 1) for j in range(int(math.floor(m/2)))]:
                    hict = len([opos for (_, (_, _, opos)) in [(q, tupe) for (q, tupe) in response.items() if q in questions] if int(opos) == hi])
                    #print("Number at stage %d, position %d: %d" % (m, hi, hict))
                    loct = len([opos for (_, (_, _, opos)) in [(q, tupe) for (q, tupe) in response.items() if q in questions] if int(opos) == lo])
                    #print("Number at stage %d, position %d: %d" % (m, lo, loct))
                    n = hict + loct
                    if n == 0:
                        continue
                    mu = 0.5 * n
                    delta = math.sqrt((3 * math.log(alpha)) / (- mu))
                    x = {True : hict, False : loct}[hict > loct]
                    b = (1 + delta) * mu
                    c = x >= (1 + delta) * mu
                    if x >= b:
                        if hict > loct:
                            stuff_to_print[hi] = "%d >= %f" % (x,b)
                            stuff_to_print[lo] = str(loct)
                        else :
                            stuff_to_print[hi] = str(hict)
                            stuff_to_print[lo] = "%d >= %f" % (x,b)
                    else :
                        stuff_to_print[hi] = str(hict)
                        stuff_to_print[lo] = str(loct)
                    stuff_to_print[4] += hict + loct                        
                    #print("If %d >= %f : Bot? %s, workerid: %s, amazon reviews?: %s\n" % (x, b, c, workerid, amazon(workerid)))
                    this_classification.append((response, x >= (1 + delta) * mu, n))
        if any([t[1] for t in this_classification]):
            print "<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%d</td>" % (stuff_to_print[0], stuff_to_print[1], stuff_to_print[2], stuff_to_print[3], stuff_to_print[4])
        # policy for deciding bots for each question length? what's
        # the probability of making an incorrect classification?
        # will probably want a discount factor over these things
        # for now, just return that it's a bot if one is true
        classifications.append((response, any([t[1] for t in this_classification]), workerid))
    return classifications

def amazon(workerid):
    return True
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
    #print(len(profiles))
    for profile in profiles:
        offensive = np.random.choice(list(profile.keys()), 1, replace=True)[0]
        for quid in list(profile.keys()):
            # i have some marginal probability of abandoning at every question, but let my probability of abandoning at a particular question be very high
            oid, prob = profile[quid]
            oprob = random.random()
         
            if quid==offensive:
                print(offensive, oprob , vprob)
         
    print(len(profiles))
    return profiles

def breakoff_frequency_by_question(survey, responses):
    breakoff = {q.quid : 0 for q in survey.questions}
    for response in responses:
        questions_by_index = sorted(list(response.items()), key = lambda x : int(x[1][1]))
        breakoff[questions_by_index[-1][0].quid] += 1
    return breakoff

def breakoff_frequency_by_position(survey, responses):
    # 0-indexed
    breakoff = {i : 0 for i in range(len(survey.questions))}
    for response in responses:
        breakoff[len(response)-1] += 1
    return breakoff


def get_interval(samp, alpha, norm=False):
    B = 2000
    bootstrap_sample = [sorted(np.random.choice(samp, len(samp), replace=True)) for _ in range(B)]
    bootstrap_means = [np.average(samp) for samp in bootstrap_sample]
    bootstrap_mean = np.average(bootstrap_means)
    if norm :
        bootstrap_std = np.std(bootstrap_means)
        a, b = bootstrap_mean - 2.0*bootstrap_std, bootstrap_mean + 2.0*bootstrap_std
    else :
        hi = int(math.ceil((1-alpha)*len(bootstrap_means)))
        lo = int(math.floor(alpha*len(bootstrap_means)))
        a = sorted(bootstrap_means)[lo]
        b = sorted(bootstrap_means)[hi]
    return (a, b)
    
def identify_breakoff_questions(survey, responses, alpha):
    fmap1 = breakoff_frequency_by_position(survey, responses)
    fmap2 = breakoff_frequency_by_question(survey, responses)
    return (fmap1, fmap2)

