module BugDetection

using SurveyObjects, HypothesisTests
importall HypothesisTests

export orderBias, variantBias, questionBreakoff, positionBreakoff

function orderBias(s::Survey, responses, q1::Question, q2::Question, alpha::Float64)
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

end
