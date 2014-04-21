module SurveyObjects

export Option, Question, Survey, computeEmpiricalDistributions, computeFrequencyMap

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

function computeFrequencyMap(s::Survey, responses)
    freqMap::Dict = {q.id => {o.id => 0 for o in q.options} for q in s.questions}
    for responseMap in responses
        for qid in keys(responseMap)
            (q, o) = responseMap[qid]
            freqMap[qid][o.id] += 1
        end
    end
    freqMap
end

function computeEmpiricalDistributions(fmap::Dict)
    #println(collect(fmap[collect(keys(fmap))[1]]))
   {qid => {oid => (sumCt = sum(values(fmap[qid])) ; sumCt > 0 ? ct/sumCt : 0)
            for (oid,ct) in collect(fmap[qid])}
                for qid in keys(fmap)}
end

end
