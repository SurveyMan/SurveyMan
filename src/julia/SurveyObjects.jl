module SurveyObjects

export Option, Question, Survey, computeEmpiricalDistributions

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

function computeEmpiricalDistributions(s::Survey, responses)
    freqMap = {q => {o => 0 for o in q.options} for q in s.questions}
    for b in responses
        for q in keys(b)
            freqMap[q][b[q]] += 1
        end
    end
    {q => {o => (s = sum(values(freqMap[q])) ; s == 0 ? 0 : freqMap[q][o]/s) for o in keys(freqMap[q])} for q in keys(freqMap)}
end

end
