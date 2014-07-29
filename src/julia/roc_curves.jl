# these are the simulated ROC curves
# no actual data needs to be imported
# plot TP vs FP as the % bad actors increases

using SurveyObjects
importall SurveyObjects

# phonology has 98 question in total
# 96 are 4-option likert
# one is a 2-option exclusive and always first
# one a freetext and always last
phonology = Survey([Block(symbol("1"), [Question(gensym(), [Option(gensym(), 0), Option(gensym(), 1)], true, false, false)],
                          [], false, None),
                    Block(symbol("2"), [Question(gensym(), [Option(gensym(), i) for i=0:3], j, true, true, false) for j=0:95],
                          [], false, None),
                    Block(symbol("3"), [Question(gensym(), [], false, false, true)],
                          [], false, None)])

# prototypicality has 4*16 + 1 questions
# 16 blocks of 4 questions each -- exclusive, ordered
# one block has one question with 3-options, exclusive, unordered
prototypicality = Survey([Block(symbol("1"), [],
                                [Block(symbol(string(i)),
                                       [Question(gensym(), [Option(gensym(), j) for j=0:3], (i*4)+k, true, true, false) for k=0:3],
                                       [], true, NEXT) for i=0:15]
                                true, None),
                          Block(symbol("2"), [Question(gensym(), [Option(gensym(), j) for j=0:3], 64)])])


                          
# first, select some set of "true answers". 
ex1_valid_response_phonology = {q.id => pick(q.options) for q in getAllQuestions(phonology)}
ex1_valid_response_prototypicality = {q.id => pick(q.options) for q in getAllQuestions(prototypicality)}

# next, make a function that takes a single answer set and generates our population on the basis of parameters
function makeStuff(trueAnswers::Dict, percInvalid::Int, sampleSize::Float64)
    numInvalid = int(percBots * sampleSize)
    numValid = sampleSize - numInvalid
    
end



