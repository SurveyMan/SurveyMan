from loadHITs import *
from scipy.stats import spearmanr
import matplotlib.pyplot as plt
import numpy as np

gitDir = '/Users/etosch/dev/SurveyMan-public/'
source = gitDir+'data/SMLF5.csv' #sys.argv[1] 
hitDir = '/Users/etosch/Desktop/phonology/' #sys.argv[2] 

survey = get_survey(source)
responses = load_from_dir(hitDir, survey)
for response in responses:
    ans = response['Answers']
    for (_, (txt, _, pos)) in ans.items():
        if 'definitely' in txt.otext:
            assert( pos=='0' or pos=='3')
        if 'probably' in txt.otext:
            assert( pos=='1' or pos=='2')
responses_by_id = [{ q.quid : (o.oid, a, b) for (q, (o, a, b)) in response['Answers'].items() } for response in responses]

# align the bot responses -- this will be used for both bot analysis and correlation
# get the words used and create a map from quid -> words

word_quid_map = {}
for q in survey.questions:
    otext = q.options[0].otext
    if ' ' in otext:
        (qual, compword) = otext.split(' ')
        if qual=='definitely':
            word_quid_map[q.quid] = compword.split('-')

        
#correlation
def get_corr_for_suffix(suffix):

    retval = {} # tuple of questions that maps to spearmanr

    for q1 in survey.questions:
        
        if q1.quid in word_quid_map:
            (w1, s1) = word_quid_map[q1.quid]
            if s1 != suffix:
                continue
        else: 
            continue

        retval[q1] = {}

        for q2 in survey.questions:
            
            if q2.quid in word_quid_map:
                (w2, s2) = word_quid_map[q2.quid]
                if s2 != suffix:
                    continue
            else: 
                continue

            q1aleft = s1.startswith('a')
            q2aleft = s2.startswith('a')
            obs1 = []
            obs2 = []
            coding = {'definitely' : { True : 1, False : 4 },
                      'probably' : { True : 2, False : 3}}

            for response in responses:
                ans = response['Answers']
                if q1 in ans and q2 in ans:
                    q1resp = ans[q1]
                    q2resp = ans[q2]
                else:
                    continue
                (adj1, chunk1) = q1resp[0].otext.split(' ')
                (adj2, chunk2) = q2resp[0].otext.split(' ')
                (w11, s11) = chunk1.split('-')
                (w22, s22) = chunk2.split('-')
                assert(w1==w11)
                assert(w2==w22)
                obs1.append(coding[adj1][q1aleft])
                obs2.append(coding[adj2][q2aleft])
                
            #print(obs1)
            #print(obs2)
            retval[q1][q2] = spearmanr(obs1, obs2)
        
    return retval

#breakoff analysis
#bad_pos, bad_q = evaluation.identify_breakoff_questions(survey, responses_by_id, 0.05)
#bad_qs = [ q for q in survey.questions if q.quid in [bq['question'] for bq in bad_q]]

# will want to sort the words by end vowel
if __name__ == "__main__":
    # sort the survey questions by their word's last letters
    def keyfn(q):
        if q.quid in word_quid_map: 
            return word_quid_map[q.quid][0][-1]
        else:
            return '0'

    corrs_thon = get_corr_for_suffix('thon')
    corrs_licious = get_corr_for_suffix('licious')

    fig, ax = plt.subplots()

    # thon plot
    ax_thon = plt.subplot(1, 2, 1)
    thon = list(corrs_thon.items())
    thon.sort(key = lambda tupe : keyfn(tupe[0]))
    for (i, (q1 , m)) in enumerate(thon):
        thon[i] = (q1, list(m.items()))
        thon[i][1].sort(key = lambda tupe : keyfn(tupe[0]))

    thon_column_labels = [word_quid_map[q.quid][0] for q in [q for (q, _) in thon]]
    thon_row_labels = thon_column_labels
    thon_data = np.array([[spear for (q2, (spear, p)) in corrs] for (_, corrs) in thon])
    thon_heatmap = ax_thon.pcolor(thon_data, cmap=plt.cm.Blues)
    
    ax_thon.set_xticks(np.arange(thon_data.shape[0])+0.5, minor=False)
    ax_thon.set_yticks(np.arange(thon_data.shape[1])+0.5, minor=False)
    
    ax_thon.invert_yaxis()
    ax_thon.xaxis.tick_top()

    ax_thon.set_xticklabels(thon_row_labels, minor=False, rotation=90)
    ax_thon.set_yticklabels(thon_column_labels, minor=False)

    

    # licious plot
    ax_licious = plt.subplot(1,2,2)
    licious = list(corrs_licious.items())
    licious.sort(key = lambda tupe : keyfn(tupe[0]))
    for (i, (q1 , m)) in enumerate(licious):
        licious[i] = (q1, list(m.items()))
        licious[i][1].sort(key = lambda tupe : keyfn(tupe[0]))

    licious_column_labels = [word_quid_map[q.quid][0] for q in [q for (q, _) in licious]]
    licious_row_labels = licious_column_labels
    licious_data = np.array([[spear for (q2, (spear, p)) in corrs] for (_, corrs) in licious])
    licious_heatmap = ax_licious.pcolor(licious_data, cmap=plt.cm.Blues)
    
    ax_licious.set_xticks(np.arange(licious_data.shape[0])+0.5, minor=False)
    ax_licious.set_yticks(np.arange(licious_data.shape[1])+0.5, minor=False)
    
    ax_licious.invert_yaxis()
    ax_licious.xaxis.tick_top()

    ax_licious.set_xticklabels(licious_row_labels, minor=False, rotation=90)
    ax_licious.set_yticklabels(licious_column_labels, minor=False)
    
    fig.suptitle('-thon, -licious', verticalalignment='bottom')

    #print(thon[0], licious[0])

    plt.show()
