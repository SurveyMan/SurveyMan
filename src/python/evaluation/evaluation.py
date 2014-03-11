import urllib, httplib
import csv, os, sys
import numpy as np
import matplotlib.pyplot as pyplot
import math
from survey.objects import *
# first evaluate bot detection
# want to compare expected number of catch questions, percent bots, ability to catch
# maybe want to vary by the number of profiles (corresponds to clusters)

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
        classifications.append((response, any([t[1] for t in this_classification]), None))
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

# run_this()

# s1 = Survey([Question("", [Option("") for _ in range(5)], qtypes["radio"], shuffle=True) for _ in range(10)])
# profiles = make_breakoff_profiles(s1, 1)
# bots, nots = generate_samples(s1, profiles, 100, 0.1)
# by_pos = breakoff_frequency_by_position(s1, bots+nots)
# print by_pos, sum(by_pos.values())
# by_question = breakoff_frequency_by_question(s1, bots+nots)
# print by_question, sum(by_question.values())

# bad_pos, bad_q = identify_breakoff_questions(s1, bots+nots, 0.1)
# print bad_pos
# print bad_q
