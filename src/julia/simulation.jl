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
        # flipping a coin isn't making a big enough difference
        if r[q1].pos < r[q2].pos && rand() > 0.5
            r[q2] = o
        end
        #r[q2] = o
    end
    @printf("Create bias in %s when %s preceeds it\n\t\tInitialFrequencies\tNew Frequencies\n", q2.id, q1.id)
    freq2 = computeEmpiricalDistributions(s, nots)
    for (o, f) in freqs[q2]
        @printf("%s\t%f\t%f \n", o,f,freq2[q2][o])
    end    
    freqs = computeEmpiricalDistributions(s, nots)
    # loop through all questions and see if we can detect it
    fp = 0.0
    fn = 0.0
    for i=1:length(s.questions)
        a = s.questions[i]
        for j=i:length(s.questions)
            b = s.questions[j]
            ans = orderBias(s, [bots,nots], a, b, 0.05)
            if haskey(ans, q1) && haskey(ans, q2)
                if ans[q1]
                    fp += 1
                    println("False positive:", ans)
                    for (o, f) in freqs[q1]
                        @printf("%s\t%f\n", o,f)
                    end    
                elseif !ans[q2]
                    fn += 1
                    println("False negative:", ans)
                    responses = [bots, nots]
                    q1q2 = computeEmpiricalDistributions(s, filter(r -> r[q1].pos < r[q2].pos, responses))
                    q2q1 = computeEmpiricalDistributions(s, filter(r -> r[q1].pos > r[q2].pos, responses))
                    println(q2q1[q2])
                    for (o, f) in q1q2[q2]
                        @printf("%s\t%f\t%f\n", o,f,q2q1[q2][o])
                    end
                end
            elseif ans[a]
                fp += 1
                @printf("(False positive) Difference in distribution in %s when %s precedes it:\n", a.id, b.id)
                responses = [bots, nots]
                q1q2 = computeEmpiricalDistributions(s, filter(r -> r[a].pos < r[b].pos, responses))
                q2q1 = computeEmpiricalDistributions(s, filter(r -> r[a].pos > r[b].pos, responses))
                for (o, f) in q1q2[a]
                    @printf("%s\t%f\t%f\n", o,f,q2q1[a][o])
                end    
            elseif ans[b]
                fp += 1
                @printf("(False positive) Difference in distribution in %s when %s precedes it:\n", b.id, a.id)
                responses = [bots, nots]
                q1q2 = computeEmpiricalDistributions(s, filter(r -> r[a].pos < r[b].pos, responses))
                q2q1 = computeEmpiricalDistributions(s, filter(r -> r[a].pos > r[b].pos, responses))
                for (o, f) in q1q2[b]
                    @printf("%s\t%f\t%f\n", o,f,q2q1[b][o])
                end    
            end

        end
    end
    @printf("perc. fp : %f \t perc. fn : %f"
            , fp / (length(s.questions)^2 * 0.5 - 1)
            , fn / 1)
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
        @printf("%f%% bots classified as humans\n", (fn / length(bots)))
        @printf("%f%% humans classified as bots\n", (fp / length(nots)))
    end
end

println(test())




    
    
end
