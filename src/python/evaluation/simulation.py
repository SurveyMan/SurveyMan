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

def emma_classify(survey, bots, nots, delta, diff):
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

def emery_classify(survey, bots, nots, delta):
    pass

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


