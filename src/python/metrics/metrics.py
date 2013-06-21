import path
from survey.objects import *
import numpy as np
import matplotlib.pyplot as ppl
import matplotlib.cm as cm
import math


def bootstrap(samples, statistic=np.mean, B=100, alpha=0.05, sampler=lambda x : np.random.choice(x, size=len(x))):
    #print "in bootstrap"
    n = len(samples)
    bootstrap_samples = np.sort([statistic(bss) for bss in [sampler(samples) for _ in range(B)]])
    #print "done generating samples"

    bootstrap.mean = np.mean(bootstrap_samples)
    #print "done computing mean"
    bootstrap.se = np.std(bootstrap_samples)
    bootstrap.ci = (bootstrap_samples[int((alpha/2.0)*B)], bootstrap_samples[int((1-alpha/2)*B)])

    def isOutlier(val, lower=bootstrap.ci[0], upper=bootstrap.ci[1]):
        return val < lower or val > upper

    def returnOutliers():
        return [out for out in bootstrap_samples if isOutlier(out)]

    def displayHistogram(numbins=10):
        binsize =(bootstrap_samples[-1] - bootstrap_samples[0]) / numbins
        print "binsize", binsize
        binmarkers = [bootstrap_samples[0] + (binsize * i) for i in range(numbins+1)]
        print "binmarkers", binmarkers
        frequencies = [len([s for s in bootstrap_samples if a <= s < b]) for (a, b) in zip(binmarkers[:-1], binmarkers[1:])]
        print "frequencies", frequencies
        print "samples", bootstrap_samples
        ppl.hist(bootstrap_samples
                 , bins={True : numbins, False : len(set(bootstrap_samples))}[numbins<len(set(bootstrap_samples))]
                 , normed=False
                 , histtype='bar')
        ppl.show()

    bootstrap.isOutlier = isOutlier
    bootstrap.displayHistogram = displayHistogram
    bootstrap.returnOutliers = returnOutliers


# similarity/difference measures

def kernal(survey_responses):
    # row index indicates the question index
    # column index indicates my difference with everyone else

    srs = [s.sorted() for s in survey_responses]
    similarities = []

    for survey_response in srs:

        sim_matrix = [[]]*len(survey_responses)

        for (i, (question, option_list)) in enumerate(survey_response):
            # if freetext, edit distance?
            diff_fn = { 
                qtypes["radio"] : \
                  { True : lambda you : {True : 0, False : 1}[option_list[0].oid == you[0].oid]
                    , False : lambda you : abs(option_list[0].oindex - you[0].oindex) / (1.0 * len(question.options)) 
                  }
                , qtypes["check"] : \
                  { True : lambda you : {True : 0, False : 1}[all([a.oid==b.oid for (a, b) in zip(option_list, you)])]
                    , False : lambda you : abs(sum([pow(2, o.oindex) for o in option_list])
                                               - sum([pow(2, o.oindex) for o in you]))
                    / (pow(2, len(question.options)) - 1.0)        
                  }
                , qtypes["dropdown"] : \
                  { True : lambda you : {True : 0, False : 1}[option_list[0].oid == you[0].oid]
                    , False : lambda you : abs(option_list[0].oindex - you[0].oindex) / (1.0 * len(question.options)) 
                  }
            }[question.qtype][question.ok2shuffle]

            for (j, (_, your_response_to_this_question)) in enumerate([r[i] for r in srs]):
                sim_matrix[j] = sim_matrix[j] + [diff_fn(your_response_to_this_question)]

        similarities.append(np.matrix(sim_matrix))

    return [np.squeeze(np.asarray(np.mean(s, axis=0))) for s in similarities]

#compute the entropy of a survey
def surveyentropy(survey_responses):
    #get a list of histograms of responses for each question
    response_matrix=responsematrix(survey_responses)
    response_matrix=response_matrix.transpose()
    q_hists=[buildhistogram(np.squeeze(np.asarray(q))) for q in response_matrix]
    tot_entropy=0
    for (probs, bins, patches) in q_hists:
        for p in probs:
            if(p>0):
                tot_entropy+=p*math.log(p) 
    return -tot_entropy

#compute entropy of each survey response
def questionentropy(question):
    entropy=0
    (probs, bins, patches)=buildhistogram(question)
    for p in probs:
        if(p>0):
            entropy+=p*math.log(p) 
    return -entropy

#loop through questions of a survey, apply bootstrap with qentropy statistic, return list of confidence intervals (one per question)
def qentropyintervals(survey_responses):
    question_cis=[]
    response_matrix=responsematrix(survey_responses)
    response_matrix=response_matrix.transpose()
    #print response_matrix
    for q in response_matrix:
        bootstrap(np.squeeze(np.asarray(q)),statistic=questionentropy)
        question_cis.append(bootstrap.ci)
    return question_cis
    
#generate a numerical response matrix from list of survey responses
def responsematrix(survey_responses):
    response_matrix=[]
    for survey_response in survey_responses:
        response_matrix.append(survey_response.toNumeric())
    response_matrix=np.matrix(response_matrix)
    return response_matrix

def normalize_responses(response_matrix):
    #normalize response values
    response_matrix = response_matrix.transpose()
    for i,q in enumerate(np.asarray(response_matrix)):
        maxq=max(q)
        for j, a in enumerate(q):
            q[j]=(a*1.0)/(maxq*1.0)
        response_matrix[i]=q
    return np.matrix(response_matrix).transpose()
    

#create a normed histogram for each question in the SurveyRepsonse
def buildhistogram(question):
    q_hist=ppl.hist(question, bins = int(max(question)), normed=True)
    return q_hist

#returns tuple of percentage survey responses classified correctly (real %, fake%)
#call after a call to bootstrap to ensure that it's correctly identifying outliers
def correctlyClassified(survey_responses, statistic=np.mean):
    numReal, numFake, classifiedReal, classifiedFake = 0,0,0,0
    for s in survey_responses:
        print "going through responses"
        if(bootstrap.isOutlier(statistic(s))):
            classifiedFake+=1
        else:
            classifiedReal+=1
        if(s.Real):
            numReal+=1
        else:
            numFake+=1
    return (classifiedReal*1.0/numReal*1.0, classifiedFake*1.0/numFake*1.0)

        

def test():
    q1 = Question("a", [1,2,3], qtypes["radio"])
    q2 = Question("b", [2,3,4,5], qtypes["radio"], shuffle=True)
    q3 = Question("c", [3,4,5,6,7], qtypes["check"])
    q4 = Question("d", [4,5,6,7,8,9], qtypes["check"], shuffle=True)
    q5 = Question("e", [5,6,7,8,9,10,11], qtypes["dropdown"])
    q6 = Question("f", [6,7,8,9,10,11,12,13], qtypes["dropdown"], shuffle=True)
    r1 = SurveyResponse([(q1, q1.options[0:1]), (q2, q2.options[0:1]), (q3, q3.options[0:2])
                         , (q4, q4.options[0:3]), (q5, q5.options[0:1]), (q6, q6.options[0:1])])
    r2 = SurveyResponse([(q1, q1.options[0:1]), (q2, q2.options[1:2]), (q3, q3.options)
                         , (q4, q4.options[1:3]), (q5, q5.options[1:2]), (q6, q6.options[2:3])])
    r3 = SurveyResponse([(q1, q1.options[1:2]), (q2, q2.options[2:3]), (q3, q3.options)
                         , (q4, q4.options[2:]), (q5, q5.options[1:2]), (q6, q6.options[2:3])])
    
    similarities = kernal([r1,r2,r3])

    #print similarities
    
    bootstrap([r1,r2,r3], statistic=surveyentropy)
    print bootstrap.isOutlier(surveyentropy([r1,r2,r3]))

    def perQ(sample):
        # Consider outliers per question
        print "\r\n===OUTLIER ANALYSIS PER QUESTION===\r\n"
        for q in np.matrix(sample).transpose():
            qq = np.squeeze(np.asarray(q))
            bootstrap(qq)
            print "95% confidence interval:", bootstrap.ci
            print "OUTLIERS", set(bootstrap.returnOutliers())
            # bootstrap.displayHistogram()
            

    def perS(sample):
        # Consider outliers over the entire survey
        print "\r\n===OUTLIER ANALYSIS PER SURVEY===\r\n"
        some_primes = [2, 3, 5, 7, 11, 13, 17, 23, 29, 31, 37, 79]
        bootstrap(sample
                  , statistic = lambda x : sum([pow(p, v) for (p, v) in zip(some_primes[:len(x)], x)])
                  , sampler = lambda y : [np.random.choice(np.squeeze(np.asarray(row))) for row in np.matrix(y).transpose()])
        print "95% confidence interval:", bootstrap.ci
        print "OUTLIERS", set(bootstrap.returnOutliers())
        

    test.perQ=perQ
    test.perS=perS


               
if __name__=="__main__":
    test()
    #test.perQ()
    #test.perS()
