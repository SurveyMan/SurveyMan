module Utilities

export H, LL, pick, bootstrapSample, CIR, CIL

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

end
