# Datatypes -- these should be moved to another file
type Option
    id::Symbol
    pos::Int
end

type Question
    id::Symbol
    options::Array{Option,1}
    pos::Int
    exclusive::Bool
    ordered::Bool
end

type Survey
    questions::Array{Question,1}
end

type Preference
    pref::Dict{Question, (Option, Float64)}
end


# util functions
function H(msg::Array)
    - sum(map(p -> p * log2(p), msg))
end

function LL(probs::Array)
    - sum(map(log, probs))
end

function pick(l::Array)
    pick(l, 1)[1]
end

function pick(l::Array, n::Int)
    [l[abs(rand(Int) % length(l)) + 1] for _=1:length(l)]
end

function bootstrapSample(l::Array)
    B = 2000
    [pick(l, length(l)) for _=1:B]
end

function CIR(l::Array, s::Function, alpha::Float64)
    sstat = sort(map(s, bootstrapSample(l)))
    index = ceil(length(l) * (1 - alpha))
    sstat[index]
end

function CIL(l::Array, s::Function, alpha::Float64)
    sstat = sort(map(s, bootstrapSample(l)))
    index = ceil(length(l) * alpha)
    sstat[index]
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

    
function computeEmpiricalDistributions(s::Survey, responses)
    freqMap = {q => {o => 0 for o in q.options} for q in s.questions}
    for b in responses
        for q in keys(b)
            freqMap[q][b[q]] += 1
        end
    end
    {q => {o => freqMap[q][o]/sum(values(freqMap[q])) for o in keys(freqMap[q])} for q in keys(freqMap)}
end

function LPOClassifier{T}(s::Survey, bots::Array{T}, nots::Array{T}, delta::Float64, diff::Float64)
end

function maxEntOutlierClassifier(s::Survey, data::Array, alpha::Float64)
    # this doesn't seem well-founded, but I'll compute it anyway
    # it seems like an abuse of the notion of entropy and seems like it would suffer from the
    # same issues as computing the likelihood
    distrs = computeEmpiricalDistributions(s,data)
    ents = [H([distrs[q][b[q]] for q in keys(b)]) for b in data]
    thresh = CIL(ents, samp -> sum(samp) / length(samp), alpha)
    qoMap -> (h = H([distrs[q][qoMap[q]] for q in keys(qoMap)]) ; (h, h < thresh)), thresh
end

function maxLogLikelihoodClassifier{T}(s::Survey, data::Array, alpha::Float64)
    distrs = computeEmpiricalDistributions(s,data)
    lls = [LL([distrs[q][d[q]] for q in keys(d)]) for d in data]
    thresh = CIL(ents, samp -> sum(samp) / length(samp), alpha)
    qoMap -> (h = LL([distrs[q][qoMap[q]] for q in keys(qoMap)]) ; (h, h < thresh)), thresh
end

function biasClassifier{T}(s::Survey, bots::Array{T}, nots::Array{T}, alpha::Float64)
end
      
function test()
    opt1 = Option(gensym(), 0)
    opt2 = Option(gensym(), 1)
    q1 = Question(gensym(), [opt1, opt2], 0, true, false)
    q2 = Question(gensym(), [opt1, opt2], 1, true, false)
    s = Survey([Question(gensym(), [Option(gensym(), i) for i=1:5], j, true, false) for j=1:20])
    prefs = profile(s)
    profiles = makeClusters(s, 1)
    bots, nots = sample(s, profiles, 100, 0.2)
    for classifierMethod in [maxEntOutlierClassifier, maxLogLikelihoodClassifier]
        classifier, threshhold = classifierMethod(s,[bots,nots],0.05)
        @printf("Scores below %f indicate bots\n", threshhold)
        fn = 0.0
        fp = 0.0
        for (score, class) in map(classifier, bots)
            println(score, " ", class)
            if ! class
                fn += 1
            end
        end
        println("--------------------")
        for (score, class) in map(classifier, nots)
            println(score, " ", class)
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
