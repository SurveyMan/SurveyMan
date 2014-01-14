from loadHITs import *
from scipy.stats import spearmanr
import matplotlib.pyplot as plt
import numpy as np

#correlation
def get_corr_for_suffix(suffix, responses):

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
                if q1 in response and q2 in response:
                    q1resp = response[q1]
                    q2resp = response[q2]
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
            return word_quid_map[q.quid][0][::-1]
        else:
            return '0'

    gitDir = '/Users/etosch/dev/SurveyMan-public/'
    source = gitDir+'data/SMLF5.csv' #sys.argv[1] 
    hitDir = '/Users/etosch/Desktop/phonology2/' #sys.argv[2] 

    survey = get_survey(source)
    responses = load_from_dir(hitDir, survey)
    print("Total number of responses", len(responses))
    print("Total number of unique respondents", len(set([r['WorkerId'] for r in responses])))
    # remove non-native english speakers
    q_native_speaker = [q for q in survey.questions if 7 in q.sourceRows][0]
    o_native_speaker = [o for o in q_native_speaker.options if o.otext == 'Yes'][0]
    responses = [r for r in responses if q_native_speaker in r['Answers'] and r['Answers'][q_native_speaker][0] == o_native_speaker]

    for response in responses:
        ans = response['Answers']
        for (_, (txt, _, pos)) in ans.items():
            if 'definitely' in txt.otext:
                assert( pos=='0' or pos=='3')
            if 'probably' in txt.otext:
                assert( pos=='1' or pos=='2')

    # align the bot responses -- this will be used for both bot analysis and correlation
    # get the words used and create a map from quid -> words

    word_quid_map = {}
    for q in survey.questions:
        otext = q.options[0].otext
        if ' ' in otext:
            (qual, compword) = otext.split(' ')
            assert(qual=='definitely')
            word_quid_map[q.quid] = compword.split('-')
            
    # can definitely remove people who picked the same position every time
    print("Total number of native speaker responses:", len(responses))
    # previous bot classification is too aggressive
    classifications = evaluation.bot_lazy_responses_ordered(survey, [r['Answers'] for r in responses] , 0.05)
    responses = [ r for (r, isBot, _) in classifications if not isBot ]
    print("Total number of non-(bots or lazies):", len(responses))
    
    corrs_thon = get_corr_for_suffix('thon', responses)
    corrs_licious = get_corr_for_suffix('licious', responses)

    fig, ax = plt.subplots()
    colormap = plt.cm.cool

    # thon plot
    ax_thon = plt.subplot(1, 2, 1)
    thon = list(corrs_thon.items())
    thon.sort(key = lambda tupe : keyfn(tupe[0]))
    for (i, (q1 , m)) in enumerate(thon):
        thon[i] = (q1, list(m.items()))
        thon[i][1].sort(key = lambda tupe : keyfn(tupe[0]))

    thon_column_labels = [word_quid_map[q.quid][0] for q in [q for (q, _) in thon]]
    thon_row_labels = [word_quid_map[q.quid][0] for (q, _) in thon[1][1]]
    thon_data = np.array([[spear for (q2, (spear, p)) in corrs] for (_, corrs) in thon])
    thon_heatmap = ax_thon.pcolor(thon_data, cmap=colormap)
    
    ax_thon.set_xticks(np.arange(thon_data.shape[0])+0.5, minor=False)
    ax_thon.set_yticks(np.arange(thon_data.shape[1])+0.5, minor=False)
    
    ax_thon.invert_yaxis()
    ax_thon.xaxis.tick_top()

    ax_thon.set_xticklabels(thon_row_labels, minor=False, rotation=90)
    ax_thon.set_yticklabels(thon_column_labels, minor=False)

    #print(word_quid_map[thon[11][0].quid], thon[11][1])
    ax_thon.set_ylim([len(thon_row_labels), 0])
    ax_thon.set_xlim([0, len(thon_column_labels)])
    
    ax_thon.set_xlabel("-(a?)thon")
    

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
    licious_heatmap = ax_licious.pcolor(licious_data, cmap=colormap)
    
    ax_licious.set_xticks(np.arange(licious_data.shape[0])+0.5, minor=False)
    ax_licious.set_yticks(np.arange(licious_data.shape[1])+0.5, minor=False)
    
    ax_licious.invert_yaxis()
    ax_licious.xaxis.tick_top()

    ax_licious.set_xticklabels(licious_row_labels, minor=False, rotation=90)
    ax_licious.set_yticklabels(licious_column_labels, minor=False)

    ax_licious.set_ylim([len(licious_row_labels), 0])
    ax_licious.set_xlim([0, len(licious_column_labels)])
    
    ax_licious.set_xlabel("-(a?)licious")
    #print(thon[0], licious[0])

    plt.show()
