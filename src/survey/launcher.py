from questionnaire import *
#from agents import *

def make_questions():
    q1 = Question("What is your age?"
                  , ["< 18", "18-34", "35-64", "> 65"]
                  , qtypes["radio"])              

    q2 = Question("What is your political affiliation?"
                  , ["Democrat", "Republican", "Indepedent"]
                  , qtypes["radio"]
                  , shuffle=True)

    q3 = Question("Which issues do you care about the most?"
                   , ["Gun control", "Reproductive Rights", "The Economy", "Foreign Relations"]
                   , qtypes["check"]
                   ,shuffle=True)

    q4 = Question("What is your year of birth?"
                   , [x+1910 for x in range(80)]
                   , qtypes["dropdown"])
    
    return [q1, q2, q3, q4]
    
def process(responses):
    # throw out bad responses
    # add counts to 
    return

def launch():
    num_takers = 100
    total_takers = 0
    qs = make_questions()
    survey = Survey(qs)
    counts = []
    for question in qs:
        counts.append([question.quid, ([(str(option),0) for option in question.options])])
    while (total_takers < num_takers):
        survey.shuffle()
        responses = agent.take_survey(survey) # get back list of (quid, [options])
        for (response in responses):
            q_index = qs.index(response[0])
            for (opt in response[1]):
                o_index = qs[q_index].index(opt)
        counts[q_index][1][o_index][1] = counts[q_index][1][o_index][1] + 1
        process(responses)
        display(responses)
        total_takers = total_takers+1
    
    return

if __name__=="__main__":
    launch()