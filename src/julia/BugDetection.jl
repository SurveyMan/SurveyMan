module BugDetection

using SurveyObjects, HypothesisTests, NHST, Distributions, StatsBase
importall HypothesisTests, NHST, Distributions, StatsBase

export orderBiasUnordered, variantBias, questionBreakoff, positionBreakoff, selectWhere, correlationCramer, chiSquareStat, correlationSpearman

function klDivergence{S<:Real, T<:Real}(P::Vector{S}, Q::Vector{T})
    assert(length(P)==length(Q))
    distrPQ = sum(map(i -> log(P[i]/Q[i])*P[i], [i for i=1:length(P)]))
    distrQP = sum(map(i -> log(Q[i]/P[i])*Q[i], [i for i=1:length(P)]))
    (distrPQ, distrQP)
end

function selectWhere(f, responses)
    retval=[]
    for response in responses
        for (_,tupe1) in response
            for (_,tupe2) in response
                if f(tupe1, tupe2)
                    retval = [response, retval]
                end
            end
        end
    end
    return unique(retval)
end

function chiSquareStat(dataTable)
    r, c = size(dataTable)
    total = sum(dataTable)
    E = *([sum(dataTable[i,:])/total for i=1:r], transpose([sum(dataTable[:,i]) for i=1:c]))
    sum(map((a,b) -> a / b, [x^2 for x in dataTable - E], collect(E)))
end

function orderBiasOrdered(s::Survey, responses, q1::Question, q2::Question, alpha::Float64)
    q1q2 = computeEmpiricalDistributions(s, filter(r -> r[q1].pos < r[q2].pos, responses))
    q2q1 = computeEmpiricalDistributions(s, filter(r -> r[q1].pos > r[q2].pos, responses))
    toInts = m -> map(i -> convert(Float64, i), collect(values(m)))
    # types; they're holding me back again
    a::Array{Float64,1} = toInts(q1q2[q1])
    b::Array{Float64,1} = toInts(q2q1[q1])
    c::Array{Float64,1} = toInts(q1q2[q2])
    d::Array{Float64,1} = toInts(q2q1[q2])
    {q1 => pvalue(MannWhitneyUTest(a,b)) < alpha,
     q2 => pvalue(MannWhitneyUTest(c,d)) < alpha}
end
                                                  
function orderBiasUnordered(s::Survey, responses, q1::Question, q2::Question, alpha::Float64)
    q1first = selectWhere((ans1, ans2) -> ans1[1].id==q1.id && ans2[1].id==q2.id && ans1[1].pos < ans2[1].pos, responses)
    q2first = selectWhere((ans1, ans2) -> ans1[1].id==q1.id && ans2[1].id==q2.id && ans1[1].pos > ans2[1].pos, responses)
    q1q2freq = computeFrequencyMap(s, q1first)
    q2q1freq = computeFrequencyMap(s, q2first)
    assert(length(q1first)+length(q2first)==length(responses))
    P1 = [] ; Q1 = [] ; P2 = [] ; Q2 = []
    keyset1 = [o.id for o in q1.options]
    keyset2 = [o.id for o in q2.options]
    for k in keyset1
        P1 = [q2q1freq[q1.id][k], P1]
        Q1 = [q1q2freq[q1.id][k], Q1]
    end
    for k in keyset2
        P2 = [q1q2freq[q2.id][k], P2]
        Q2 = [q2q1freq[q2.id][k], Q2]
    end
   {q1.id => weakChiSquare(P1,Q1) < alpha && weakChiSquare(Q1, P1) < alpha
    , q2.id => weakChiSquare(P2,Q2) < alpha && weakChiSquare(Q2,P2) < alpha}
    ## {q1.id =>reduce((a,b)->a&&b, map(p -> p < alpha/2,  klDivergence(P1,Q1)))
    ##  , q2.id => reduce((a,b)->a&&b, map(p -> p < alpha/2, klDivergence(P2,Q2)))}
end


function variantBias(s::Survey, responses, variants::Array{Question,1}, alpha::Float64)
    # do a pair-wise comparison of all variants, rather than kruskal-wallis
    ids = {o => i for (i,o) in enumerate(variants[i].options)}
    data = {q => [ids[o] for o in map(r -> r[q], responses)] for q in variants}
    {col => {row => pvalue(MannWhitneyUTest(data[col], data[row])) < alpha for row in variants} for col in variants}
end

function questionBreakoff(s::Survey, responses, alpha::Float64)
    # this doesn't handle paths right now
    brokenoff = filter(r -> length(r) < length(s.questions), responses)
    breakFreq = {q => length(filter(r -> r[q].pos == length(r))) for q in s.questions}
    {q => CIR(collect(values(breakFreq)), mean, alpha) for q in s.questions}
end

function positionBreakoff(s::Survey, responses, alpha::Float64)
    #currently doesn't handle paths
    breakFreq = {i => length(filter(r -> length(r) == i, responses)) for i=1:(length(responses)-1)}
    {i => CIR(collect(values(breakFreq)), mean, alpha) for i=1:length(responses)-1}
end

function correlationCramer(dataTable)
    chi = chiSquareStat(dataTable)
    N = sum(dataTable)
    k = minimum(size(dataTable))
    chi, sqrt(chi / (N*(k - 1)))
end

function correlationSpearman(dataTable)
    StatsBase.corspearman(dataTable)
end

    end
