import metrics
import numpy as np
import math

def bootstrap(samples, statistic=metrics.surveyentropy, B=100, alpha=0.05):
    #print "in bootstrap"
    n = len(samples)
    exclusion_list=[]
    resamples=[]
    #resample from list of SRs, keep a list for each resample of which SRs aren't included
    for i in range(B):
        temp = np.random.choice(samples, n, replace=True)
        notin=[]
        for j, r in enumerate(samples):
            if r not in set(temp):
                notin.append(j)
        exclusion_list.append(notin)
        resamples.append(temp)
    #print "EXCLUSION LIST"
    #print exclusion_list
    #calculate entropy of each resample, keep track of entropies for surveys lacking each SR
    bootstrap_samples=[]
    per_respondent_entropy = [[] for _ in samples]
    for i, bss in enumerate(resamples):
        entropy=statistic(bss)
        bootstrap_samples.append(entropy)
        for j,r in enumerate(samples):
            if j in exclusion_list[i]:
                per_respondent_entropy[j].append(entropy)
    
    bootstrap_samples = np.sort(bootstrap_samples)
    #print "done generating samples"

    bootstrap.mean = np.mean(bootstrap_samples)
    #print "done computing mean"
    bootstrap.se = np.std(bootstrap_samples)
    bootstrap.ci = (bootstrap_samples[int((alpha/2.0)*B)], bootstrap_samples[int((1-alpha/2)*B)])

    #generate list of outlying responses

    def returnOutliers():
        threshold = 5 #Not sure why the threshold value is 5, just copying from Emery's metric
        outliers = []
        for i, e in enumerate(per_respondent_entropy):
            #print e
            avg_entropy=np.mean(e)
            std_dev=np.std(e)
            #Welch's t test
            t = (bootstrap.mean - avg_entropy) / math.sqrt((bootstrap.se*bootstrap.se)/B+ (std_dev*std_dev)/len(e))
            if t>threshold:
                outliers.append(samples[i])
        return outliers

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


    bootstrap.displayHistogram = displayHistogram
    bootstrap.returnOutliers = returnOutliers
