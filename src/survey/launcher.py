from questionnaire import *
from agents import *

# def make_questions():
#     q1 = Question("What is your age?"
#                   , ["< 18", "18-34", "35-64", "> 65"]
#                   , qtypes["radio"])              

#     q2 = Question("What is your political affiliation?"
#                   , ["Democrat", "Republican", "Indepedent"]
#                   , qtypes["radio"]
#                   , shuffle=True)

#     q3 = Question("Which issues do you care about the most?"
#                    , ["Gun control", "Reproductive Rights", "The Economy", "Foreign Relations"]
#                    , qtypes["check"]
#                    ,shuffle=True)

#     q4 = Question("What is your year of birth?"
#                    , [x+1910 for x in range(80)]
#                    , qtypes["dropdown"])
    
#     return [q1, q2, q3, q4]

def is_lazy(responses):
    # this is a stupid way of doing things; should look at this more
    # would like to do something involving entropy that's sensitive to location
    # an even simpler start would be to throw out responses that have a run
    # whose likelihood lies outside a 95% confidence interval
    all([oindices==responses[0] for \
         oindices in \
         [o for (q, o) in responses]])

def remove_bots(responses):
    # we will eventually mark questions that need to be consistent.
    # consistency logic should be baked in to the app
    pass

def ignore(responses):
    # throw out bad responses
    return is_lazy(responses) # or remove_bots(responses)

    
# Database is of the form:
# counts = {quid, {oid 1:# of respondants, oid 2:# of respondants, oid 3:# of respondants}, ...}
def display(quid_to_question, oid_to_option, database):
    return

def launch():
    num_takers = 100
    total_takers = 0
    qs = [q1, q2, q3, q4]
    survey = Survey(qs)
    counts = []
    # quid_dict = {QUID, question text}
    # For each question, create a new entry in the database that looks like this:
    # oid_dict = {OID, option object}
    # counts = {quid, {oid 1:# of respondants, oid 2:# of respondants, oid 3:# of respondants}, ...}
    quid_dict = {}
    oid_dict = {}
    counts = {}
    for question in qs:
        quid_dict[question.quid] = question
        for option in question.options:
            oid_dict[option.oid] = option
        counts[question.quid] = {option.oid : 0 for option in question.options}
    #print quid_dict
    #print oid_dict
    #print counts
    # Create a list of 100 CollegeStudents
    agent_list = []
    for i in range(num_takers):
        agent_list.append(CollegeStudent())
    while (total_takers < num_takers):
        survey.shuffle()
        responses = agent_list[total_takers].take_survey(survey) # get back list of (quid, [options])
        if not ignore(responses):
            for response in responses:
                opt_counts = counts[response[0].quid]
                for option in response[1]:
                    opt_counts[option.oid] = opt_counts[option.oid] + 1 # add 1 to each of the options chosen
            total_takers = total_takers+1
            # print counts
            # display(quid_dict, oid_dict, counts)
    for (quest, opts) in counts.iteritems():
        q=quid_dict[quest]
        num_ans=sum(opts.values())
        if q.qtype==qtypes["radio"] or q.qtype==qtypes["dropdown"]: 
            # radio buttons have once choice each
            assert(num_ans==num_takers)
        elif q.qtype==qtypes["check"]:
            assert(num_ans>=num_takers and num_ans<=num_takers*len(q.options))
        else:
            print "Unsupported question type: %s" % [k for (k, v) in qtypes.iteritems() if v==q.qtype][0]
            assert(1==2)
    return [quid_dict, oid_dict, counts]


if __name__=="__main__":
    launch()
