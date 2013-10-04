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
        print row[1], row[2]
        record = {'questionid' : questionid}
        record['created_at']=datetime.datetime(row[1], '%x %H:%M')
        record['started_at']=datetime.datetime(row[2], '%x %H:%M')
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

# order questions by time

# want a visual representation of overlap
