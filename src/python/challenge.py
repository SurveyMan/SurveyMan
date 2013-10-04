import csv
import matplotlib
import matplotlib.pyplot as pyplot
import datetime

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

# check both ways of computing likelihood

for respondent_id in responses.keys():
    ans = 0
    for response in responses[respondent_id]:
        this_qs_fmap = question_answer_freq[response['questionid']]
        this_ans_prob = 


# want a visual representation of overlap
