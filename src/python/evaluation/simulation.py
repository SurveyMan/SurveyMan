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
