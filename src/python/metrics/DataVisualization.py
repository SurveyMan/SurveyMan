import numpy as np
import matplotlib.pylab as plt
import csv
import random
import scipy.stats as stat
import matplotlib.backends.backend_pdf as pdf

#returns number of responses generated
def generateResponses(filename, real, rand):
    answers=[]
    with open(filename, 'r+') as csvfile:
        reader=csv.reader(csvfile, delimiter=',')
        qopts = reader.next()
        writer=csv.writer(csvfile, delimiter=',')
        for _ in range(real):
            writer.writerow([1]*6)
            answers.append([1]*6)
        for _ in range(rand):
            row=[random.randint(1,int(num)) for num in qopts]
            writer.writerow(row)
            answers.append(row)
    return answers

def answerFrequency(filename):
    with open(filename, 'r') as csvfile:
        questions, answercount=[],[]
        reader=csv.reader(csvfile, delimiter=',')
        numr=0
        for(i,r) in enumerate(reader):
            if(i==0):
                questions = ['Question '+str(i+1) for i in range(len(r))]
                for q in r:
                    answercount.append([0]*int(q))
            else:
                numr+=1
                for (i,answer) in enumerate(r):
                    answercount[i][int(answer)-1]+=1
    return answercount

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
    x=np.linspace(0,max(qanswers),100)
    density=stat.kde.gaussian_kde(qanswers)
    figure=plt.figure()
    #figure.set_title(qtext)
    #figure.
    axis=figure.add_subplot(111)
    axis.plot(x,density(x))
    return figure
        
if __name__=='__main__':
    answers=generateResponses('results.csv',80,20)
    pp=pdf.PdfPages('QuestionPMF.pdf')
    plt.show()
    for i in range(len(answers[0])):
        qanswers=[]
        for a in answers:
            qanswers.append(float(a[i]))
        pp.savefig(answerPMFGraph(qanswers, 'Question '+str(i)))
    pp.close()        
        
    print answerPMF('results.csv')
    print "done"
