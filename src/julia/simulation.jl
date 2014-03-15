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
function H(msg::Vector{Float64})
    - sum(map(p -> p * log2(p), msg))
end

function pick(l::Array)
    l[abs(rand(Int) % length(l)) + 1]
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

function computeEmpiricalDistributions(s::Survey, bots, nots)
    
end

function emma_classify{T}(s::Survey, bots::Array{T}, nots::Array{T}, delta::Float64, diff::Float64)
end

function emery_classify{T}(s::Survey, bots::Array{T}, nots::Array{T}, alpha::Float64)
    distrs = computeEmpiricalDistributions(s,bots,nots)
end

function test()
    opt1 = Option(gensym(), 0)
    opt2 = Option(gensym(), 1)
    q1 = Question(gensym(), [opt1, opt2], 0, true, false)
    q2 = Question(gensym(), [opt1, opt2], 1, true, false)
    s = Survey([q1, q2])
    prefs = profile(s)
    profiles = makeClusters(s, 2)
    samples = sample(s, profiles, 10, 0.5)
    return samples
end


println(test())
