import numpy as np
import matplotlib as mpl
import matplotlib.pylab as plt
import matplotlib.pyplot as pyplot
import csv
import random
import scipy.stats as stat
import matplotlib.backends.backend_pdf as pdf
import os

#returns list of answers (first row is contains question options)
def generateResponses(filename, real, rand):
    answers=[]
    with open(filename, 'r+') as csvfile:
        reader=csv.reader(csvfile, delimiter=',')
        qopts = reader.next()
        answers.append([int(opts) for opts in qopts])
        writer=csv.writer(csvfile, delimiter=',')
        for _ in range(real):
            writer.writerow([1]*6)
            answers.append([1]*6)
        for _ in range(rand):
            row=[random.randint(1,int(num)) for num in qopts]
            writer.writerow(row)
            answers.append(row)
    return answers

def answerFrequency(responses, qoptions):
    answercount=[]
    numr=0
    for(i,r) in enumerate(responses):
        #print r
        if(i==0):
            for opts in qoptions:
                answercount.append([0]*int(opts))
            #print answercount
        else:
            numr+=1
            for (i,answer) in enumerate(r):
                answercount[i][int(answer)-1]+=1
    return answercount

def questionBootstrap(samples, B=10, statistic=answerFrequency, sampler=lambda x : [x[random.randint(1,len(x)-1)]for _ in range(len(x))]):
    n=len(samples)-1
    qoptions=samples[0]
    bootstrap_samples=[statistic(resample, qoptions) for resample in [sampler(samples) for _ in range(B)]]
    print bootstrap_samples
    bootstrap_samples=np.squeeze(np.asarray(bootstrap_samples))
    print bootstrap_samples
    avg_opt_counts=[[0]*int(opts) for opts in qoptions]
    for resamplecounts in bootstrap_samples:
        for (i,qcounts) in enumerate(resamplecounts):
            for (j, optcounts) in enumerate(qcounts):
                avg_opt_counts[i][j]+=optcounts
    #print avg_opt_counts
    for (i,qcounts) in enumerate(avg_opt_counts):
        for (j, optcounts) in enumerate(qcounts):
            avg_opt_counts[i][j]=(1.0*optcounts)/(B)
    return avg_opt_counts
                

def clearCSV(filename):
    with open(filename, 'r') as csvfile:
        reader=csv.reader(csvfile, delimiter=',')
        options=reader.next()
        csvfile.close()
    os.remove(filename)
    with open(filename, 'w') as csvfile:
        writer=csv.writer(csvfile, delimiter=',')
        writer.writerow(options)
        

def answerPMF(filename):
    answerPMF=answerFrequency(filename)
    print answerPMF
    numr=sum(answerPMF[0])
    print numr
    for(i, r) in enumerate(answerPMF):
        for(j, c) in enumerate(r):
            answerPMF[i][j]=float(c)/numr
    return answerPMF

def answerPMFGraph(qanswers, qtext):
    figure=plt.figure()
    #figure.set_title(qtext)
    #figure.
    axis=figure.add_subplot(111)
    axis.hist(qanswers, bins=range(int(max(qanswers))), normed=1, histtype='bar', align='mid', rwidth=1)
    axis.set_xlabel('Options')
    axis.set_xlim(0,max(qanswers))
    axis.set_ylabel('Probability')
    axis.set_ylim(0, 1)
    axis.set_title(qtext)
    return figure


def correlationHeatmap(qanswers):
    #print len(qanswers)
    correlation_matrix = []
    print correlation_matrix
    for (i,q1) in enumerate(qanswers):
        correlation_matrix.append([])
        for (j,q2) in enumerate(qanswers):
            correlation = stat.spearmanr(q1, q2)[0]
            #print "Question 1: "+str(q1)+" Question 2: "+str(q2)+" Correlation: "+str(correlation)+" index: "+str(i)+","+str(j)
            correlation_matrix[i].append(correlation)
    #print correlation_matrix
    correlation_matrix=np.array(correlation_matrix)
    print correlation_matrix
    figure, ax = pyplot.subplots()
    heatmap=ax.pcolor(correlation_matrix, cmap=pyplot.cm.bwr, vmin=-1, vmax=1)
    ax.set_xticks(np.arange(correlation_matrix.shape[0])+0.5, minor=False)
    ax.set_yticks(np.arange(correlation_matrix.shape[1])+0.5, minor=False)
    ax.set_xticklabels([i for i in range(1,len(qanswers)+1)], minor=False)
    ax.set_yticklabels([i for i in range(1,len(qanswers)+1)], minor=False)
    ax.set_title("Question Correlations")
    #ax2=make_axes_gridpec(ax)
    #mpl.colorbar.ColorbarBase(ax2, cmap=pyplot.cm.bwr)
    pyplot.savefig('heatmap.png')    
        

if __name__=='__main__':
    answers=generateResponses('results.csv',80,20)
    qanswers=questionBootstrap(answers)
    correlationHeatmap(qanswers)
    #answers_per_q=[]
    #pp=pdf.PdfPages('QuestionPMF.pdf')
    #plt.show()
    #for i in range(len(answers[0])):
    #    qanswers=[]
    #    for a in answers:
    #        qanswers.append(float(a[i]))
    #    print qanswers
    #    answers_per_q.append(qanswers)
    #    pp.savefig(answerPMFGraph(qanswers, 'Question '+str(i+1)))
    #pp.close()
    #print '\n'
    #print answerPMF('results.csv')
    #print '\n'
    #correlationHeatmap(answers_per_q)
    #print "done"
    clearCSV('results.csv')
