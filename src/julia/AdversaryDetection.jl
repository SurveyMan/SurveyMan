module AdversaryDetection

using SurveyObjects, Utilities
importall SurveyObjects, Utilities

export LPOClassifier, maxEntOutlierClassifier, maxLogLikelihoodClassifier, biasClassifier

function LPOClassifier(s::Survey, data::Array, delta::Float64, diff::Float64)
    
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

function maxLogLikelihoodClassifier(s::Survey, data::Array, alpha::Float64)
    distrs = computeEmpiricalDistributions(s,data)
    lls = [LL([distrs[q][d[q]] for q in keys(d)]) for d in data]
    thresh = CIL(lls, samp -> sum(samp) / length(samp), alpha)
    qoMap -> (h = LL([distrs[q][qoMap[q]] for q in keys(qoMap)]) ; (h, h < thresh)), thresh
end

function biasClassifier(s::Survey, data::Array, alpha::Float64)
    
end

end
