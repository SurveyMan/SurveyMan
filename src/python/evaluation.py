import csv, os, sys
import numpy as np
import matplotlib.pyplot as pyplot
import math
from survey.objects import *
# first evaluate bot detection
# want to compare expected number of catch questions, percent bots, ability to catch
# maybe want to vary by the number of profiles (corresponds to clusters)


def profile(s):
    """ Takes in a survey and returns a profile of a respondent. A profile of a respondent is a map of questions to preferred answer. A respondent with this profile will answer the given question with some probability in the range (1/m, 1), where m is the number of options. The respondent will choose any of the other responses with equal probability."""
    preferences = {}
    for q in s.questions:
        preference = np.random.choice(q.options, 1, replace=True)[0]
        equal_prob = 1.0 / len(q.options)
        prob =  equal_prob + (random.random() * (1 - equal_prob))
        preferences[q.quid] = (preference.oid , prob)
    return preferences

def make_profiles(s, n):
    return [profile(s) for _ in range(n)]

def sample(s, profile_list, size, percent_bots):
    num_bots = int(math.floor(size * percent_bots))
    num_people = size - num_bots
    bot_responses = []
    not_responses = []
    for _ in range(num_bots):
        bot_responses.append({q.quid : np.random.choice(q.options, 1, replace=True)[0].oid for q in s.questions})
    for _ in range(num_people):
        # sample from the profile list ; profiles can have repeats
        profile = np.random.choice(profile_list, 1, replace=True)[0]
        response = {}
        for q in s.questions:
            # see if we should grab the preferred response
            (oid, likelihood) = profile[q.quid]
            if random.random() < likelihood:
                response[q.quid] = oid
            else:
                # randomly sample from the remaining options
                other_opts = [o for o in q.options if o.oid != oid]
                response[q.quid] = np.random.choice(other_opts, 1, replace=True)[0].oid
        not_responses.append(response)
    return (bot_responses, not_responses)

def frequency(survey, responses):
    """ responses needs to be a single list"""
    freqs = {q.quid : {o.oid : 0 for o in q.options} for q in survey.questions}
    for response in responses:
        for quid in list(response.keys()):
            oid = response[quid]
            freqs[quid][oid] += 1
    return freqs

def empirical_prob(fmap):
    probs = {quid : {oid : 0 for oid in list(fmap[quid].keys())} for quid in list(fmap.keys())}
    for quid in list(fmap.keys()):
        total = sum(fmap[quid].values()) # should be equal to the total number of respondents if we don't permit breakoff
        for oid in list(fmap[quid].keys()):
            probs[quid][oid] = float(fmap[quid][oid]) / float(total)
    return probs

def log_likelihood(response, pmap):
    likelihood = 0.0
    for quid in list(response.keys()):
        oid = response[quid]
        likelihood -= math.log(pmap[quid][oid])
    return likelihood

def make_bootstrap_interval(survey, responses, pmap, alpha):
    # this one is over all responses
    B = 2000
    pmap = empirical_prob(frequency(survey, responses))
    log_likelihoods = [log_likelihood(r, pmap) for r in responses]
    bootstrap_sample = [sorted(np.random.choice(log_likelihoods, len(responses), replace=True)) for _ in range(B)]
    #print len(bootstrap_sample[0])
    #aindex = int(math.floor((alpha / 2.0)*len(responses)))
    #bindex = int(math.floor((1.0 - (alpha / 2.0))*len(responses)))
    #return (np.average([s[aindex] for s in bootstrap_sample]), np.average([s[bindex] for s in bootstrap_sample]))
    bs_mean = np.average([np.average(samp) for samp in bootstrap_sample])
    bs_std = np.std([np.average(samp) for samp in bootstrap_sample])
    return (bs_mean - 2*bs_std, bs_mean + 2*bs_std)


def analyze_classifications(classifications):
    false_negatives = 0
    false_positives = 0
    for (isbot, classified_as_bot, ll) in classifications:
        if isbot and not classified_as_bot:
            false_negatives += 1
        if not isbot and classified_as_bot:
            false_positives += 1
#        print (isbot, classified_as_bot, ll)
#    print "Bots misclassified as humans : %d" % false_negatives
#    print "Humans misclassified as bots : %d " % false_positives
    return (false_negatives, false_positives)

    

def get_least_popular_options(survey, responses, diff):
    fmap = frequency(survey, responses)
    least_popular = {}
    for quid in list(fmap.keys()):
        optfreqs = list(fmap[quid].items())
        optfreqs = sorted(optfreqs, key = lambda t : t[1])
        #print [freqs[1] for freqs in optfreqs]
        for (i, j) in [(k, k+1) for k in range(len(optfreqs)-1)]:
            if optfreqs[i][1] < optfreqs[j][1]*diff:
                least_popular[quid] = optfreqs[:j]
                break
    print("Number of questions with least popular options : %d" % len([opts for opts in list(least_popular.values()) if len(opts)!=0]))
    return least_popular

#delta = 0.75
# for (q, opts) in get_least_popular_options(s1, bots+nots, delta).items():
#    print q, len(opts)

def get_mu(survey, least_popular_options):
    expectation = 0
    for q in survey.questions:
        if q.quid in least_popular_options:
            expectation += float(len(least_popular_options[q.quid])) / float(len(q.options))
    return expectation

#print "Expected number of least popular questions a bot should answer: %d" % get_mu(s1, get_least_popular_options(s1, bots+nots, delta))

def num_least_popular(response, lpo):
    n = 0
    for quid in list(lpo.keys()):
        opt = response[quid]
        if opt in [o[0] for o in lpo[quid]]:
            n += 1
    return n

def classify2(survey, bots, nots, delta, diff):
    lpo = get_least_popular_options(survey, bots+nots, diff)
    mu = get_mu(survey, lpo)
    alpha = pow(math.e, (- delta * mu) / (2 + delta))
    print("Expect %f least popular answers for a bot; bots will answer fewer than this with probability %f" % (mu, alpha))
    classifications = []
    for response in bots:
        n = num_least_popular(response, lpo)
        classifications.append((True, n >= round(mu), n))
    for response in nots:
        n = num_least_popular(response, lpo)
        classifications.append((False, n >= round(mu), n))
    return classifications

#analyze_classifications(classify2(s1, bots, nots, 1, 0.75))
def classify_bots_as_outliers(survey, bots, nots, alpha):
    pmap = empirical_prob(frequency(survey, bots+nots))
    (a, b) = make_bootstrap_interval(survey, bots+nots, pmap, alpha)
    #print "%2.0f percent confidence interval : (%f, %f)" % ((1.0 - alpha)*100, a,b)
    classifications = []
    for response in bots:
        ll = log_likelihood(response, pmap)
        classifications.append((True, ll < a or ll > b, ll))
    for response in nots:
        ll = log_likelihood(response, pmap)
        classifications.append((False, ll < a or ll > b, ll))        
    return classifications

def classify_humans_as_outliers(survey, bots, nots, alpha):
    pmap = empirical_prob(frequency(survey, bots+nots))
    (a, b) = make_bootstrap_interval(survey, bots+nots, pmap, alpha)
    #print "%2.0f percent confidence interval : (%f, %f)" % ((1.0 - alpha)*100, a,b)
    classifications = []
    for response in bots:
        ll = log_likelihood(response, pmap)
        classifications.append((True, ll > a and ll < b, ll))
    for response in nots:
        ll = log_likelihood(response, pmap)
        classifications.append((False, ll > a and ll < b, ll))        
    return classifications

# make graphs
def generate_bots_v_humans(bot_classifier, clusters):
    n=10
    m=5
    s1 = Survey([Question("", [Option("") for _ in range(m)], qtypes["radio"], shuffle=True) for _ in range(n)])
    data = []
    for i in range(1,10):
        sub_data = []
        for j in range(100):
            bots, nots = sample(s1, make_profiles(s1, clusters), 100, i/10.0)
            classifications = bot_classifier(s1, bots, nots, 0.05)
            (false_negative, false_positive) = analyze_classifications(classifications)
            sub_data.append((float(false_negative) / float(len(bots)) , float(false_positive) / float(len(nots)), i/10.0))
        data.append(sub_data)
    return data


# from John
def csv_to_map(path):
    data = {}
    excl = {}
    with open(path) as fp:
        question = -1
        options = -1
        exclusive = -1
        qid = -1
        qstr = None
        for idx, line in enumerate(csv.reader(fp, delimiter=',', quotechar='"')):
            cols = [col.strip('"').strip().lower() for col in line]
            if idx == 0:
                question = cols.index('question')
                options = cols.index('options')
                exclusive = cols.index('exclusive')
                continue
            if cols[question]:
                qid = idx
                qstr = 'q_%d_%d' % (qid+1,question+1)
                
                # look at first exclusive thingy for this question
                is_exclusive = True
                if exclusive != -1:
                    if cols[exclusive] == 'false':
                        is_exclusive = False
                excl[qstr] = is_exclusive
                data[qstr] = []
            data[qstr] += ['comp_%d_%d' % (idx+1, options+1)]
    tupled = {}
    for k in list(data.keys()):
        tupled[k] = (excl[k], data[k])
    return {stuff[0] : stuff[1][1] for stuff in list(tupled.items()) if stuff[1][1]}

def idmap_to_survey(idmap):
    # structure is id -> (optlist, exclusive?)
    questions = []
    for quid in list(idmap.keys()):
        (radio, opts) = idmap[quid]
        t = radio and qtypes['radio'] or qtypes['check']
        if not radio:
            continue
        options = []
        for oid in opts:
            o = Option("")
            o.oid = oid
            options.append(o)
        q = Question("", options, t)
        q.quid = quid
        questions.append(q)
    return Survey(questions)

def opt_id_lookup(idmap):
    retval = {}
    for quid in list(idmap.keys()):
        (_, opts) = idmap[quid]
        for oid in opts:
            retval[oid] = quid
    return retval

def mturkresults_to_sample(survey, idmap, filename):
    responses_map = {}
    header = True
    lookup = opt_id_lookup(idmap)
    for row in csv.reader(open(filename)):
        if header:
            header = False
        else :
            workerid = row[0]
            print(workerid)
            for response in row[8:]:
                if '|' not in response and response != '':
                    print(optid)
                    optid = response.split(";")[0]
                    print(quid)
                    quid = lookup[optid]
                    responses_map[quid] = response
                    # ignore checkboxes for now
    print({workerid : responses_map})
    return {workerid : responses_map}

def get_mturkresults(survey, idmap, directory):
    responses = {}
    for f in os.listdir(directory):
        if "(" not in f:
            print(f)
            responses.update(mturkresults_to_sample(survey, idmap, directory + "/" + f))
    return responses

#def analyse_mturk():             
# directory = "/Users/etosch/Desktop/ipierotis_results"
# source_csv = "/Users/etosch/dev/SurveyMan-public/data/Ipierotis.csv"
# idmap = csv_to_map(source_csv)
# s2 = idmap_to_survey(idmap)
# responses = get_mturkresults(s2, idmap, directory)


#     pmap = empirical_prob(frequency(s2, samples))
#     print "pmap",  pmap, "done pmap"
#     (a, b) = make_bootstrap_interval(s2, samples, pmap, 0.05)
#     print "interval", a, b
#     classifications = []
#     print "Vanilla bootstrap for outlier detection in the population"
#     for (workerid, response_map) in responses.items():
#         print response_map
#         ll = log_likelihood({q[0]: q[1].split(';')[0] for q in response_map.items()})
#         if ll < a or ll > b:
#             print workerid
# analyse_mturk()


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

def now_with_my_thing(clusters):
    n=10
    m=5
    s1 = Survey([Question("", [Option("") for _ in range(m)], qtypes["radio"], shuffle=True) for _ in range(n)])
    data = []
    for i in range(1,10):
        sub_data = []
        for j in range(100):
            bots, nots = sample(s1, make_profiles(s1, clusters), 100, i/10.0)
            classifications = classify2(s1, bots, nots, 1.0, 0.75)
            (false_negative, false_positive) = analyze_classifications(classifications)
            sub_data.append((float(false_negative) / float(len(bots)) , float(false_positive) / float(len(nots)), i/10.0))
        data.append(sub_data)
    return data

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
        questions_by_index = sorted(list(response.items()), key = lambda x : x[1][1])
        #print "last thing", questions_by_index[-1][0], questions_by_index[-1][1][1], len(questions_by_index)
        breakoff[questions_by_index[-1][0]] += 1
    return breakoff

def breakoff_frequency_by_position(survey, responses):
    breakoff = {i : 0 for i in range(len(survey.questions))}
    for response in responses:
        breakoff[len(response)-1] += 1
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
    positions = [thing[1] for thing in list(fmap1.items()) if thing[0]!=9] # counts at position
    questions = list(fmap2.values()) # counts at question
    _, b = get_interval(positions, alpha*2)
    _, d = get_interval(questions, alpha*2)
    print(b, d)
    for position in list(fmap1.keys()):
        if position != 9:
            if fmap1[position] > b:
                bad_positions.append(position)
    for questionid in list(fmap2.keys()):
        if fmap2[questionid] > d:
            bad_questions.append(questionid)
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
