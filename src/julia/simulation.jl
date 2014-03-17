module Simulation

using AdversaryDetection, Utilities, SurveyObjects, BugDetection
importall AdversaryDetection, Utilities, SurveyObjects, BugDetection

type typePreference
    pref::Dict{Question, (Option, Float64)}
end

# simulator
function profile(s::Survey)
    prob = q -> 1.0 / length(q.options)
    {q => (pick(q.options), prob(q) * (1 * rand())) for q in s.questions}
    end
    
function makeClusters(s::Survey, n::Int)
    return [profile(s) for _=1:n]
end

function sample(s::Survey, clusters::Array, size::Int, percBots::Float64)
    numBots = int(size * percBots)
    bots = [{q => pick(q.options) for q in s.questions} for _=1:numBots]
    selectForProfile = (profile, q) -> ((o, p) = profile[q] ; rand() < p ? o : pick(filter(oo -> oo!=o, q.options)))
    profiles = [pick(clusters) for _=1:(size - numBots)]
    nots = [{q => selectForProfile(p, q) for q in s.questions} for p in profiles]
    return bots, nots
end
    
function test()
    s = Survey([Question(gensym(),[Option(gensym(), i) for i=1:5], j, true, false) for j=1:20])
    profiles = makeClusters(s, 1)
    bots, nots = sample(s, profiles, 100, 0.2)
    # Test bot detection
    for test_bug in [test_bots, test_order_bias, test_variant_bias, test_breakoff]
        test_bug(s, bots, nots)
    end
end

function test_order_bias(s, bots, nots)
    # assign a question order to each of the responses
    for r in [bots, nots]
        ordering = shuffle([i for i=1:length(s.questions)])
        for (i,q) in zip(ordering, keys(r))
            r[q].pos = i
        end
    end
    # select a pair of questions to introduce bias
    q1, q2 = pick(s.questions, 2)
    assert(q1.id!=q2.id)
    freqs = computeEmpiricalDistributions(s, nots)
    o = pick(q2.options)
    for r in nots
        if r[q1].pos < r[q2].pos && rand() > 0.5
            r[q2] = o
        end
    end
    println("\t\tInitialFrequencies\tNew Frequencies")
    freq2 = computeEmpiricalDistributions(s, nots)
    for (o, f) in freqs[q2]
        @printf("%s\t%f\t%f \n", o,f,freq2[q2][o])
    end    
    freqs = computeEmpiricalDistributions(s, nots)
    # loop through all questions and see if we can detect it
    fp = 0.0
    fn = 0.0
    for a in s.questions
        for b in s.questions
            ans = orderBias(s, [bots,nots], a, b, 0.05)
            if haskey(ans, q1) && haskey(ans, q2)
                if ans[q1]
                    fp += 1
                elseif !ans[q2]
                    fn += 1
                end
            elseif or(collect(values(ans)))
                fp += 1
            end
        end
    end
    @printf("perc. fp : %f \t perc. fn : %f", fp / length(s.questions)^2, fn / length(s.questions)^2)
    # do this first just on the real responses, then on real and bots?
end

function test_variant_bias(s, bots, nots)
end

function test_breakoff(s, bots, nots)
end

function test_bots(s, bots, nots)
    for classifierMethod in [maxLogLikelihoodClassifier, maxEntOutlierClassifier]
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
        @printf("%f%% bots classified as humans\n", (fn / length(bots)))
        @printf("%f%% humans classified as bots\n", (fp / length(nots)))
    end
end

println(test())




    
    
end
