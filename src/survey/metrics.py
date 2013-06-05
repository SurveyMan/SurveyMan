from questionnaire import *
import numpy as np

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

        print  Kernal.similarity([r1,r2,r3])

                             

if __name__=="__main__":
    Kernal.test()
