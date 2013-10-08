import csv
import matplotlib
import matplotlib.pyplot as pyplot
import datetime
import math


reader = csv.reader(open("/Users/etosch/dev/SurveyMan-public/data/crowdflower_sentiment.csv", "rU"))
header = True
unique_raters = set()
num_judgements = 0
unique_questions = set()
# group responses by who answered them
responses = {} 
for row in reader:
    if header:
        header = False
    else:
        raterid = row[3]
        questionid = row[0]
        unique_raters.add(raterid)
        unique_questions.add(questionid)
        num_judgements += 1
        record = {'questionid' : questionid}
        record['created_at']=datetime.datetime.strptime(row[1], '%x %H:%M')
        record['started_at']=datetime.datetime.strptime(row[2], '%x %H:%M')
        record['country']=row[4]
        record['region']=row[5]
        record['city']=row[6]
        record['answer']=row[7]
        record['tweet_text']=row[8]
        if responses.has_key(raterid):
            responses[raterid].append(record)
        else: responses[raterid] = [record]
        
print "Num raters: %d, Num Questions: %d, Num Judgements: %d" % (len(unique_raters), len(unique_questions), num_judgements)

# create a frequency map of the number of people who answered a given number of responses:
response_fmap = {}
for workerid in responses.keys():
    num_q_answered = len(responses[workerid])
    if response_fmap.has_key(num_q_answered):
        response_fmap[num_q_answered]+=1
    else : response_fmap[num_q_answered] = 1
print "Number of number of questions answered: ", len(response_fmap)

# get a sense of how many questions each respondent answered:
# make sure that keys and values are properly aligned
pyplot.scatter(response_fmap.keys(), response_fmap.values())
pyplot.xlabel('Number of questions answered by a single respondent')
pyplot.ylabel('Number of respondents answering that number of questions')
pyplot.savefig('response_frequency')

# create a frequency map of the number of respondents who answered a particular question
# also, do workers answer the same question multiple times?
question_worker_map = {}
for workerid in responses.keys():
    for task in responses[workerid]:
        qid = task['questionid']
        if question_worker_map.has_key(qid):
            question_worker_map[qid].append(workerid)
        else :
            question_worker_map[qid] = [workerid]

question_fmap_total = {}
question_fmap_unique = {}
for question in question_worker_map.keys():
    r = question_worker_map[question]
    total_responses = len(r)
    unique_responses = len(set(r))
    if question_fmap_total.has_key(total_responses):
        question_fmap_total[total_responses]+=1
    else:
        question_fmap_total[total_responses]=1
    if question_fmap_unique.has_key(unique_responses):
        question_fmap_unique[unique_responses]+=1
    else:
        question_fmap_unique[unique_responses]=1

fig, ax = pyplot.subplots(1)    
ax.scatter(question_fmap_total.keys(), question_fmap_total.values(), c='b', alpha=0.5)
ax.scatter(question_fmap_unique.keys(), question_fmap_unique.values(), c='r', alpha=0.5)
pyplot.xlabel('Number of responses to a question')
pyplot.ylabel('Number of questions having this number of responses')
pyplot.show()

# looks like these are entirely the same, but if there are small differences, they won't show up due to the scale. let's make sure:
for key in set(question_fmap_total.keys()+question_fmap_unique.keys()):
    if question_fmap_total.has_key(key):
        if question_fmap_unique.has_key(key):
            total_responses = question_fmap_total[key]
            unique_responses = question_fmap_unique[key]
            if total_responses != unique_responses:
                print key, total_responses, unique_responses, total_responses-unique_responses
        else:
            print "Not found in question_fmap_unique", key
    else:
        print "Not found in question_fmap_total", key
            
# looks like repeaters might actually be a problem, though a small one            

# order questions by time start_time; this will be like the order the end-user actually saw them in
# note : i'm not sure if either of the datetime fields actually refers to the annotator's start time

for respondent_id in responses.keys():
    responses[respondent_id] = sorted(responses[respondent_id], key=lambda v : v['started_at'])

# calculate frequencies for each question; not sure to do about repeats for now...
question_answer_freq = {}
for response_id in responses.keys():
    for response in responses[response_id]:
        quid = response['questionid']
        ans = response['answer']
        if not question_answer_freq.has_key(quid):
            question_answer_freq[quid]={'0' : 0, '1' : 0, '2' : 0, '3' : 0, '4' : 0}
        question_answer_freq[quid][ans]+=1

# have the empirical distribution over all the data
# some of this will include bad feedback

# first see if we can find any random respondents
# we have the empirical probability of each rating for each question
# calculate the joint probability for each respondent
# since path lengths are different, we also need to consider the probability that someone got this far
# 15 tweets per page, let's bucket them (they'll all be grouped together with the same timestamp)
pages_submitted_frequency = {}
num_pages = 1
pages_submitted_frequency[num_pages] = 0
for responses_completed in sorted(response_fmap):
    if responses_completed > num_pages*15:
        num_pages += 1
        pages_submitted_frequency[num_pages] = 0
    pages_submitted_frequency[num_pages] += response_fmap[responses_completed]

pyplot.scatter(pages_submitted_frequency.keys(), pages_submitted_frequency.values())
pyplot.xlabel('Number of pages submitted')
pyplot.ylabel('Number of respondents submitting after viewing this many pages')
pyplot.savefig('pagesviewed')

# naive likelihood - don't take into account position at all
# also doesn't care about the number of responses made
def naive_likelihood(response_list):
    prob = 0.0
    for response in response_list:
        quid = response['questionid']
        ans = response['answer']
        prob = prob + math.log(question_answer_freq[quid][ans]/(sum(question_answer_freq[quid].values())*1.0))
    return prob*-1.0
    

likelihoods = [(response_id, naive_likelihood(responses[response_id])) for response_id in responses.keys()]

# look for outliers in the likelihoods    
# assume normal distribution? plot likelihoods
pyplot.hist([pair[1] for pair in likelihoods], bins=100)
pyplot.savefig("likelihoods")
mean = sum([pair[1] for pair in likelihoods])/(len(likelihoods)*1.0)
sd = pow(sum([pow(pair[1]-mean, 2) for pair in likelihoods])/len(likelihoods), 0.5)
# print out response ids having outlier likelihoods
potential_outliers = []
for (response_id, likelihood) in likelihoods:
    if abs(likelihood-mean) > 2*sd:
        potential_outliers.append((response_id, likelihood))
        print response_id

# load in gold standard answers 
gold_standard = []
header = True
for row in csv.reader(open("/Users/etosch/dev/SurveyMan-public/data/reference_30_.csv", "rU")):
    if header:
        header = False
    else:
        gold_standard.append((row[0], row[1]))

# find out how our outliers did on the gold standard data
def assess(population):
    evaluation_outliers = []
    for respondent_id in population:
        num_correct = 0
        num_incorrect = 0
        num_unanswered = 0
        for (questionid, answer) in gold_standard:
            response = [r for r in responses[respondent_id] if r['questionid']==questionid]
            if len(response)==0:
                num_unanswered+=1
            elif response[0]['answer']==answer:
                num_correct+=1
            else:
                num_incorrect+=1
        print respondent_id, num_correct, num_incorrect, num_unanswered
        evaluation_outliers.append((respondent_id, num_correct, num_incorrect, num_unanswered))
    return evaluation_outliers

evaluation_outliers = assess([p[0] for p in potential_outliers])
evaluation_good_guys = assess([r for r in responses.keys() if r not in [p[0] for p in potential_outliers]])

# DEFINITELY NEED TO CONTROL FOR LENGTH



# want a visual representation of overlap
