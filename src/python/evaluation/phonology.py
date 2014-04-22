from __init__ import *
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

def make_subplot(ax, data, column_labels, row_labels, title):

    heatmap = ax.pcolor(data, cmap=colormap)
    
    ax.set_xticks(np.arange(data.shape[0])+0.5, minor=False)
    ax.set_yticks(np.arange(data.shape[1])+0.5, minor=False)
    
    ax.invert_yaxis()
    ax.xaxis.tick_top()

    ax.set_xticklabels(row_labels, minor=False, rotation=90)
    ax.set_yticklabels(column_labels, minor=False)

    ax.set_ylim([len(row_labels), 0])
    ax.set_xlim([0, len(column_labels)])
    
    ax.set_xlabel(title)

    ax.tick_params(labelsize=6)



def get_data(correlation_data):
    data = list(correlation_data.items())
    data.sort(key = lambda tupe : keyfn(tupe[0]))
    for (i, (q1 , m)) in enumerate(data):
        data[i] = (q1, list(m.items()))
        data[i][1].sort(key = lambda tupe : keyfn(tupe[0]))
    return data



# will want to sort the words by end vowel
if __name__ == "__main__":
    # sort the survey questions by their word's last letters
    def keyfn(q):
        if q.quid in word_quid_map: 
            return word_quid_map[q.quid][0][::-1]
        else:
            return '0'

    #gitDir = '/Users/etosch/dev/SurveyMan-public/'
    gitDir = os.getcwd()
    source = sys.argv[1] 
    hitDir = sys.argv[2] 

    colormap = plt.cm.cool

    survey = get_survey(source)
    print hitDir, hitDir.endswith("3")
    responses, times = load_from_dir(hitDir, survey, hitDir.endswith("3"))
    print("Total number of responses", len(responses))
    print("Total number of unique respondents", len(set([r['WorkerId'] for r in responses])))

    word_quid_map = {}
    for q in survey.questions:
        otext = q.options[0].otext
        if ' ' in otext:
            (qual, compword) = otext.split(' ')
            assert(qual=='definitely')
            word_quid_map[q.quid] = compword.split('-')

    # remove non-native english speakers
    q_native_speaker = [q for q in survey.questions if 7 in q.sourceRows][0]
    o_native_speaker = [o for o in q_native_speaker.options if o.otext == 'Yes'][0]
    responses = [r for r in responses if q_native_speaker in r['Answers'] and r['Answers'][q_native_speaker][0] == o_native_speaker]
    print("Total number of native speaker responses:", len(responses))

    # data for plotting with minimal filters
    prelim_thon = get_data(get_corr_for_suffix('thon', [ r['Answers'] for r in responses]))
     # make_subplot(plt.subplot(1,2,1) \
    #              , np.array([[spear for (q2, (spear, p)) in corrs] for (_, corrs) in prelim_thon]) \
    #              , [word_quid_map[q.quid][0] for q in [qq for (qq, _) in prelim_thon]] \
    #              , [word_quid_map[qqq.quid][0] for (qqq, _) in prelim_thon[1][1]] \
    #              , "")

    prelim_licious = get_data(get_corr_for_suffix('licious', [ r['Answers'] for r in responses]))
    # make_subplot(plt.subplot(1,2,2) \
    #              , np.array([[spear for (q2, (spear, p)) in corrs] for (_, corrs) in prelim_licious]) \
    #              , [word_quid_map[q.quid][0] for q in [qq for (qq, _) in prelim_licious]] \
    #              , [word_quid_map[qqq.quid][0] for (qqq, _) in prelim_licious[1][1]] \
    #              , "")

    #plt.show()
    # fig = plt.gcf()
    # fig.set_size_inches(8,6)
    # plt.savefig("correlation1", dpi=100, pad_inches=0.5)

    # remove repeaters
    workers = [r['WorkerId'] for r in responses]
    unique_workers = [r['WorkerId'] for r in responses if len([s for s in workers if s == r['WorkerId']])==1]
    responses = [r for r in responses if r['WorkerId'] in unique_workers]
    print("Total number of unique native English speaking respondents:", len(responses))
        
    print("Entropy before removing bots:", evaluation.entropy(survey,[r['Answers'] for r in responses]))

    # previous bot classification is too aggressive
    classifications = evaluation.bot_lazy_responses_entropy(survey, [r['Answers'] for r in responses] , 0.05, [r['WorkerId'] for r in responses])
    #classifications = evaluation.bot_lazy_responses_unordered(survey, [r['Answers'] for r in responses] , 0.1, [r['WorkerId'] for r in responses])
    responses = [ (r, workerid) for (r, isBot, workerid) in classifications if not isBot ]
    botsorlazies = [ (r, workerid) for (r, isBot, workerid) in classifications if isBot ]
    print("Total number of non-(bots or lazies):", len(responses))
    print("Entropy after removing bots:", evaluation.entropy(survey,[r for (r,_) in responses]))
    # amazonreviews = [ evaluation.amazon(r[1]) for r in botsorlazies ]
    # print("% bots having no amazon reviews:", (len([foo for foo in amazonreviews if not foo]) * 1.0) / len(amazonreviews))
    # amazonreviews = [ evaluation.amazon(r[1]) for r in responses ]
    # print("% non bots or lazies having no amazon reviews:", (len([foo for foo in amazonreviews if foo]) * 1.0) / len(amazonreviews))

    schwa_final = ["antenna", "banana", "korea", "china", "cuba","drama"]
    vowel_final = ["placebo", "spaghetti", "miami", "chili", "mayo", "hero"]
    stp, sfn, vtp, vfn = 0,0,0,0
    thresh = 0.6
    # thon plot
    #responses = [r[1] for r in responses]
    thon = get_data(get_corr_for_suffix('thon', [r[1] for (r,_) in responses]))
    thon_spear = np.array([[spear for (q2, (spear, p)) in corrs] for (_, corrs) in thon])
    print thon[0]
    thon_col_labels = [word_quid_map[q.quid][0] for q in [q for (q, _) in thon]]
    thon_row_labels = [word_quid_map[q.quid][0] for (q, _) in thon[1][1]]
    print("Correlations for -(a?)thon")
    for (i, entry) in enumerate(thon_spear):
        for (j,rho) in enumerate(entry):
            word1 = thon_col_labels[i]
            word2 = thon_row_labels[j]
            if abs(rho) > thresh:
                if word1 is word2 : 
                    assert(rho==1.0)
                    continue
                if word1 in schwa_final and word2 in schwa_final :
                    stp += 1
                elif word1 in vowel_final and word2 in vowel_final :
                    vtp +=1
                else :
                    print rho, word1, word2
            elif word1 in schwa_final and word2 in schwa_final :
                sfn += 1
            elif word1 in vowel_final and word2 in vowel_final :
                vfn +=1
    print stp, " out of ", stp + sfn, " schwa correlations correctly detected"
    print vtp, " out of ", vtp + vfn, " vowel correlations correctly detected"
    make_subplot(plt.subplot(1, 2, 1, aspect='equal')
                 , thon_spear
                 , thon_col_labels
                 , thon_row_labels
                 , "-(a?)thon")

    # licious plot
    licious = get_data(get_corr_for_suffix('licious', [r for (r,_) in responses]))
    licious_spear = np.array([[spear for (q2, (spear, p)) in corrs] for (_, corrs) in licious])
    licious_col_labels = [word_quid_map[q.quid][0] for q in [q for (q, _) in licious]]
    licious_row_labels = [word_quid_map[q.quid][0] for (q, _) in licious[1][1]]
    stp, sfn, vtp, vfn = 0,0,0,0
    print("Correlations for -(a?)licious")
    for (i, entry) in enumerate(licious_spear):
        for (j,rho) in enumerate(entry):
            word1 = licious_col_labels[i]
            word2 = licious_row_labels[j]
            if abs(rho) > thresh:
                if word1 is word2 : 
                    continue
                if word1 in schwa_final and word2 in schwa_final :
                    stp += 1
                elif word1 in vowel_final and word2 in vowel_final :
                    vtp +=1
                else :
                    print rho, word1, word2
            elif word1 in schwa_final and word2 in schwa_final :
                sfn += 1
            elif word1 in vowel_final and word2 in vowel_final :
                vfn +=1
    print stp, " out of ", stp + sfn, " schwa correlations correctly detected"
    print vtp, " out of ", vtp + vfn, " vowel correlations correctly detected"
    make_subplot(plt.subplot(1,2,2, aspect='equal')
                 , licious_spear
                 , licious_col_labels
                 , licious_row_labels
                 , "-(a?)licious")
    

    #plt.show()
    plt.savefig("correlation_" + hitDir.split('/')[-1])

    #breakoff analysis
    #bad_pos, bad_q = evaluation.identify_breakoff_questions(survey, [{ q.quid : (o.oid, a, b) for (q, (o, a, b)) in response.items() } for response in responses], 0.05)
    bad_pos, bad_q = evaluation.identify_breakoff_questions(survey, responses, 0.05)
    bad_qs = [ q for q in survey.questions if q.quid in [bq['question'] for bq in bad_q]]
    print "Position (Length) & Count\\ \hline\\"
    for (k,v) in sorted(bad_pos.items(), key = lambda tupe : tupe[1]):
        print "%s&%d\\" % (str(word_quid_map[k.quid]), text, v)
    print "\hline\\\nQuestion & Count\\ \hline\\"
    for (k,v) in sorted(bad_q.items(), key = lambda tupe : tupe[1]):
        print "%s&%d\\" % (str(word_quid_map[k.quid]), text, v)
