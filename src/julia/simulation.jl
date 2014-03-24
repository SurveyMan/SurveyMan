module Simulation

using AdversaryDetection, Utilities, SurveyObjects, BugDetection, Distributions, Winston
importall AdversaryDetection, Utilities, SurveyObjects, BugDetection

#acc = Plotly.PlotlyAccount("etosch", "xa1h3jisop")

type typePreference
    pref::Dict{Question, (Option, Float64)}
end


# simulator
function profile(s::Survey)
    prob = q -> 1.0 / length(q.options)
    {q => (pick(q.options), 1 - ((1-prob(q)) * rand())) for q in s.questions}
    end
    
function makeClusters(s::Survey, n::Int)
    return [profile(s) for _=1:n]
end

function sample(s::Survey, clusters::Array, size::Int, percBots::Float64)
    numBots = int(size * percBots)
    bots = [{q.id => (q, pick(q.options)) for q in s.questions} for _=1:numBots]
    selectForProfile = (profile, q) -> ((o, p) = profile[q] ; rand() < p ? o : pick(filter(oo -> oo!=o, q.options)))
    profiles = [pick(clusters) for _=1:(size - numBots)]
    nots = [{q.id => (q, selectForProfile(p, q)) for q in s.questions} for p in profiles]
    return bots, nots
end
    
function test()
    s = Survey([Question(gensym(),[Option(gensym(), i) for i=1:5], j, true, false) for j=1:20])
    profiles = makeClusters(s, 1)
    bots, nots = sample(s, profiles, 150, 0.2)
    # Test bot detection
    for test_bug in [test_bots, test_order_bias, test_variant_bias, test_breakoff, test_correlation]
        test_bug(s, bots, nots)
    end
end

function test_correlation(s, bots, nots)
    # setting alpha to 
    alpha = 0.05
    # first make sure we don't have any pre-existing correlations
    freqMap = computeFrequencyMap(s, [{q.id => (q, pick(q.options)) for q in s.questions} for _=1:5000])
    plotData = fill(0.0,length(s.questions), length(s.questions))
    for i=1:length(s.questions)
        for j=1:length(s.questions)
            optsi = {o.id => k for (k,o) in enumerate(s.questions[i].options)}
            optsj = {o.id => k for (k,o) in enumerate(s.questions[j].options)}
            dataTable = fill(0,length(optsi),length(optsj))
            for r in [bots,nots]
                ansi = r[s.questions[i].id][2].id
                ansj = r[s.questions[j].id][2].id
                dataTable[ optsi[ansi], optsj[ansj] ] += 1
            end
            assert(sum(dataTable)==length(bots)+length(nots))
            chi, V = correlationCramer(dataTable)
            df = min(length(optsi), length(optsj)) - 1
            plotData[i,j] = V
            if (V > 0.3)
                @printf("%s and %s are correlated with V= %f, phi=%f phisq=%f\n", s.questions[i].id, s.questions[j].id, V, sqrt(chi/sum(dataTable)), chi/sum(dataTable))
            end
        end
    end
#    data = ["Cramer's V" => plotData, "type" => "heatmap"]
#    arg1 = ["z" => rand(10,10)]
#    arg2 = ["style" => ["type" => "heatmap"]]
#    response = Plotly.plot(acc,arg1,arg2)
    p = Winston.colormap("jet", 64)
    q = imagesc(plotData)
            Winston.saveFig("randomCorrelation")
            display(q)
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
    q1, q2 = pick(s.questions, 2)
    while true
        if q1!=q2
            break
        else
            q1, q2 = pick(s.questions, 2)
        end
    end
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
        println("--------------------")
        for (score, class) in map(classifier, nots)
            #println(score, " ", class)
            if class
                fp += 1
            end
        end
        println("--------------------")
        @printf("%f%% bots classified as humans\n", (fn / length(bots))*100)
        @printf("%f%% humans classified as bots\n", (fp / length(nots))*100)
    end
end

println(test())




    
    
end
