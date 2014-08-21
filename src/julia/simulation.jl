module Simulation

using AdversaryDetection, Utilities, SurveyObjects, BugDetection, Distributions, DataFrames, StatsBase
importall AdversaryDetection, Utilities, SurveyObjects, BugDetection, DataFrames, StatsBase
export typePreference, profile, makeClusters, sample

type typePreference
    pref::Dict{Question, (Option, Float64)}
end

alpha = 0.05

# simulator
function profile(s::Survey)
    prob = q -> 1.0 / length(q.options)
    return {q => (pick(q.options), 1 - ((1-prob(q)) * rand())) for q in getAllQuestions(s)}
end
    
function makeClusters(s::Survey, n::Int)
    return [profile(s) for _=1:n]
end

function makeSample(s::Survey, numClusters::Int, sampleSize::Int, percBots::Float64)
    return makeSample(s, numClusters, sampleSize, percBots, 0.0)
end

function makeSample(s::Survey, numClusters::Int, sampleSize::Int, percBots::Float64, breakoffPrior::Float64)
    clusters = makeClusters(s, numClusters)
    numBots = int(sampleSize * percBots)
    bots = [{q.id => (q, pick(q.options)) for q in s.questions} for _=1:numBots]
    selectForProfile = (profile, q) -> ((o, p) = profile[q] ; rand() < p ? o : pick(filter(oo -> oo!=o, q.options)))
    lastPos = length(s.questions)
    profiles = [(pick(clusters), rand() < breakoffPrior ? pick(s.questions[1:(lastPos-1)]).pos : lastPos)
                for _=1:(sampleSize - numBots)]
    nots = [{q.id => (q, selectForProfile(p, q)) for q in s.questions[1:lastQ]} for (p, lastQ) in profiles]
    return bots, nots
end

function breakoff_expectation(s::Survey)
    eps = 0.1 # prob that someone will breakoff in this edu.umass.cs.surveyman.survey
    responses = makeSample(s, 1, 150, 0.0, eps)
    numQs = length(s.questions)
    @printf("Total broken off: %f\nExpected broken off:%f\n", length(filter(r -> length(r) < numQs)), 0.1 * length(responses))
    # UNFINISHED
end

function makeSurvey(numQuestions::Int, numOptions::Int)
    return Survey([Question(gensym(),[Option(gensym(), i) for i=1:numOptions], j, true, false, false) for j=1:numQuestions])
end



function test_bots()
    s = makeSurvey(20, 5)
    profiles = makeClusters(s, 1)
    bots, nots = sample(s, profiles, 150, 0.2)
    # Test bot detection
    for test_bug in [test_order_bias, test_variant_bias, test_breakoff, test_correlation]
        test_bug(s, bots, nots)
    end
end

function inExpectedCorrelations(qid1, qid2, expectedCorrelations)
    for set in expectedCorrelations
        if qid1 in set && qid2 in set
            return true
        end
    end 
    return false
end

function test_correlation(s, bots, nots)
    # first nom_nom
    # first make sure we don't have any pre-existing correlations; generate a bunch of bots
    botData1 = fill(0.0, 3, 100)
    botData2 = fill(0.0, 3, 100)
    for i=1:100
        allBots = [{q.id => (q, pick(q.options)) for q in s.questions} for _=1:(i*100)]
        tp, tn, fp, fn = correlation_proc(s, allBots, (q1, q2) -> !q1.ordered || !q2.ordered, test_nom_nom_correlation, 0.4, [(q.id,) for q in s.questions])
        botData1[i,1] = (i*100)
        botData1[i,2] = fp / (fp + tn)
        botData1[i,3] = tp / (tp + fn)
        newS = Survey([Question(gensym(), [Option(gensym(), i) for i=1:length(s.questions[1].options)], j, true, true) for j=1:length(s.questions)])
        allBots = [{q.id => (q, pick(q.options)) for q in newS.questions} for _=1:(i*100)]
        tp, tn, fp, fn = correlation_proc(newS, allBots, (q1, q2) -> q1.ordered && q2.ordered, test_ord_ord_correlation, 0.4, [(q.id,) for q in newS.questions])
        botData2[i,1] = (i*100)
        botData2[i,2] = fp / (fp + tn)
        botData2[i,3] = tp / (tp + fn)
    end
    DataFrames.writetable("allBotsCorrelationNom.csv", DataFrames.DataFrame(botData1))
    DataFrames.writetable("allBotsCorrelationOrd.csV", DataFrames.DataFrame(botData2))
#    p = Winston.colormap("jet", 64)
#    q = imagesc(allBots)
            #Winston.savefig("randomCorrelation")
    # see what happens with our profiles
    #defaultResponses = fill(0.0,length(s.questions), length(s.questions))
    #proc([bots, nots], defaultResponses,[(q.id, q.id) for q in s.questions])
#    q = imagesc(defaultResponses)
    #@printf("Generating correlations between %s and %s\n", q1.id, q2.id)
    # come up with a mapping between options
    #corrMap = zip(shuffle(q1.options), shuffle(q2.options))
    # reassign answers
    ## for r in nots
    ##     for (o1,o2) in corrMap
    ##         (_,o) = r[q1.id]
    ##         if o1.id==o.id
    ##             r[q2.id] = (q2, o2)
    ##         end
    ##     end
    ## end
    ## newResponses = fill(0.0, length(s.questions), length(s.questions))
    ## proc([bots,nots],newResponses,[(q1.id,q2.id), [(q.id, q.id) for q in s.questions]])
    ## q = imagesc(newResponses)
    ##         display(q)

end

function test_ord_ord_correlation(dataTable, qi, qj)
    rho = correlationSpearman(dataTable)
    rho, string(qi.id, " and ", qj.id, " are correlated with rho=", rho)
end

function test_nom_nom_correlation(dataTable, qi, qj)
    chi, V = correlationCramer(dataTable)
    df = min(length(qi.options), length(qj.options)) - 1
    V, string(qi.id, " and ", qj.id, " are correlated with V=", V, ", phi=", sqrt(chi/sum(dataTable)), ", phisq=", chi/sum(dataTable))
end

function correlation_proc(s, responses, condition, proc, thresh, expectedCorrelations)

    q1, q2 = pickDistinct(s.questions, 2)
    tp = 0.0; tn = 0.0 ; fp = 0.0; fn = 0.0

    freqMap = computeFrequencyMap(s, responses)

    for i=1:length(s.questions)
        for j=1:length(s.questions)

            qi = s.questions[i]
            qj = s.questions[j]

            if !condition(qi, qj)
                continue
            end
            
            optsi = {o.id => k for (k,o) in enumerate(qi.options)}
            optsj = {o.id => k for (k,o) in enumerate(qj.options)}

            dataTable = fill(0,length(qi.options),length(qj.options))

            for r in responses
                ansi = r[qi.id][2].id
                ansj = r[qj.id][2].id
                dataTable[ optsi[ansi], optsj[ansj] ] += 1
            end
            assert(sum(dataTable)==length(responses))
            stat, msg = proc(dataTable, qi, qj)

            if (inExpectedCorrelations(qi.id, qj.id, expectedCorrelations))
                if (stat > thresh)
                    tp += 1.0 #; println(msg)
                else
                    fn += 1.0
                end
            elseif (stat > thresh)
                fp += 1.0 #; println(msg)
            else
                tn += 1.0 
            end
        end
    end

    ## @printf("tp: %f\ttn: %f\tfp: %f\tfn: %f\nprecision: %f\nrecall: %f\naccuracy: %f\n"
    ##         , tp, tn, fp, fn
    ##         , tp / (tp + fp)
    ##         , tp / (tp + fn)
    ##         , (tp + tn)/(tp + tn + fp + fn)
    ##         )
    
    return tp, tn, fp, fn
end
    
function test_order_bias(s, bots, nots)
    return
    # assign a question order to each of the responses
    for r in [bots, nots]
        ordering = shuffle([i for i=1:length(s.questions)])
        for (i,qid) in zip(ordering, keys(r))
            (q, o) = r[qid]
            newQ = Question(qid, q.options, i, q.ordered, q.exclusive)
            r[newQ.id] = (newQ, o)
            delete!(r, q)
        end
    end
    # select a pair of questions to introduce bias
    # change frequency
    oldFreqs = computeFrequencyMap(s, nots)
    o = pick(q2.options)
    for r in nots
        if r[q1.id][1].pos < r[q2.id][1].pos
            r[q2.id] = (r[q2.id][1], o)
        end
        #r[q2] = o
    end
    @printf("Create bias in %s when %s preceeds it\n\tInitialFreq\tNew Freq\n", q2.id, q1.id)
    newFreqs = computeFrequencyMap(s, nots)
    for (o, f) in oldFreqs[q2.id]
        @printf("%s\t%f\t%f \n", o,f,newFreqs[q2.id][o])
    end
    getFreqs = (a,b) ->
        computeFrequencyMap(s, selectWhere((ans1, ans2) ->
            ans1[1].id==a && ans2[1].id==b && ans1[1].pos < ans2[1].pos, [bots,nots]))
    # loop through all questions and see if we can detect it
    fp = 0 ; fn = 0 ; tp = 0 ; tn = 0
    for i=1:length(s.questions)
        for j=i+1:length(s.questions)
            # result returned from orderBias is a map of question to classification;
            # the key tells is if the distribution for the key's answers is different if the other
            # key precedes it
            ((a,c),(b,d)) = orderBiasUnordered(s,[bots,nots],s.questions[i],s.questions[j], 0.05)
            if (c && b==q1.id && a==q2.id) || (d && a==q1.id && b==q2.id)
                # check for true positive
                tp = tp + 1
            elseif (!c && b==q1.id && a==q2.id) || (!d && a==q1.id && b==q2.id)
                # check false negative
                fn = fn + 1
            elseif c || d
                # check for false positive
                fp = fp + 1
                if c
                    @printf("Bias in question %s when %s precedes it\n", b,a)
                    for (o,f) in getFreqs(a,b)[b]
                        @printf("%s\t%f\t%f\n", o, f, getFreqs(b,a)[b][o])
                    end
                else
                    @printf("Bias in question %s when %s precedes it \n", a,b)
                    for (o,f) in getFreqs(b,a)[a]
                        @printf("%s\t%f\t%f\n", o, f, getFreqs(a,b)[a][o])
                    end
                end
            else
                tn = tn + 1
            end
        end
    end
    @printf("tp: %f\tfp : %f \ttn: %f\tfn : %f\n", tp, fp, tn, fn)
    # do this first just on the real responses, then on real and bots?
end

function test_variant_bias(s, bots, nots)
    
end

function test_breakoff(s, bots, nots)
end

function test_bots(s, bots, nots)
    for classifierMethod in [maxLogLikelihoodClassifier, maxEntOutlierClassifier]
        println(typeof(maxLogLikelihoodClassifier))
        classifier, threshhold = classifierMethod(s,[bots,nots],0.05)
        @printf("Scores below %f indicate bots\n", threshhold)
        fn = 0.0
        fp = 0.0
        for (score, class) in map(classifier, bots)
            #println(score, " ", class)
            if ! class
                fn += 1
            end
        end
        #println("--------------------")
        for (score, class) in map(classifier, nots)
            #println(score, " ", class)
            if class
                fp += 1
            end
        end
        #println("--------------------")
        @printf("%f%% bots classified as humans\n", (fn / length(bots))*100)
        @printf("%f%% humans classified as bots\n", (fp / length(nots))*100)
    end
end

for i=1:10
    let s = makeSurvey(16, 4)
        let r = makeSample(s, 1, 150, i*0.1)
            test_bots(s, r[1], r[2])
        end
    end
end
        
          
    
    
end
