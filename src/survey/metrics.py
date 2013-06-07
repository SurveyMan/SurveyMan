from questionnaire import *
import numpy as np
import matplotlib.pyplot as ppl
import matplotlib.cm as cm


# similarity/difference measures

class Metric:
    
    @staticmethod
    def similarity(survey_responses):
        assert(survey_responses.__class__.__name__=='list' and survey_responses[0].__class__.__name__=='SurveyResponse')
        pass


class Kernal(Metric):

    @staticmethod
    def similarity(survey_responses):
        Metric.similarity(survey_responses)
        # row index indicates the question index
        # column index indicates my difference with everyone else
        srs = [s.sorted() for s in survey_responses]
        similarities = []
        for survey_response in srs:
            sim_matrix = [[]]*len(survey_responses)
            for (i, (question, option_list)) in enumerate(survey_response):
                # if freetext, edit distance?
                diff_fn = { qtypes["radio"] : \
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
        return similarities

    @staticmethod
    def SRmetric(survey_responses):
        similarities = Kernal.similarity(survey_responses)
        sum_differences = []
        for sim_matrix in similarities:
            sum_differences.append(sim_matrix.sum().tolist())
        return sum_differences
        

    @staticmethod
    def analysis(similarities):
        #assert(similarities.__class__.__name__=='list' and similarities[0].__class__.__name__=='matrix')

        q_differences=[]
        
        #in each survey response, sum up differences between me and everyone else for each individual question
        for (i, sim_matrix) in enumerate(similarities):
            q_differences.append(sim_matrix.sum(axis=0).tolist())
            #individual_differences = sim_matrix.sum(axis=0).tolist()
            #if i == 0:
                #diff_by_question = [[]*len(individual_differences)]
            #for (j, d) in enumerate(individual_differences):
                #diff_by_question[j].append(d)
            
        for q in q_differences:
            print q

        #question_lines = [[]*len(q_differences[0])]
        respondents = []
        differences = []

        #for r in range(0, len(q_differences[0])):
            #for c in range(0, len(q_differences)):
                #question_lines[

        for (i,r) in enumerate(q_differences):
            
            for d in r[0]:
                respondents.append(i+1)
                differences.append(d)
                
                #question_lines[j].append(d[0])

        x=random.randint(1,6)
        y=random.randint(0,4)
        z=random.randint(0,360)
                
        
        ppl.scatter(respondents, differences)
        
        ppl.xlabel("Respondents")
        ppl.ylabel("Difference")
    
        #ppl.plot(respondents, question_lines[0], 'ro', respondents, question_lines[1], 'bs', respondents, question_lines[2], 'g^')
        #ppl.axis([0,max(respondents)+1,0,max(differences)+1])

        ppl.show()

    @staticmethod
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

        similarities = []
        similarities = Kernal.similarity([r1,r2,r3])

        for m in similarities:
            print m

        print "\r\n"

        #Kernal.analysis(similarities)

        tot_differences = Kernal.SRmetric([r1,r2,r3])
        print tot_differences

if __name__=="__main__":
    Kernal.test()
