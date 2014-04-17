(ns qc.report
    (:import (qc QC QCMetrics))
    (:use qc.analyses)
    )

(def validResponses (atom nil))
(def botResponses (atom nil))
(def breakoffQuestions (atom nil))
(def orderBias (atom nil))
(def variants (atom nil))
(def staticMaxEntropy (atom 0.0))
(def avgPathLength (atom 0.0))
(def maxPathLength (atom 0))
(def minPathLength (atom 0))
(def correlations (atom nil))
(def correaltionThreshhold (atom 0.5))

(defn dynamicAnalyses
    [^QC qc]
    (swap! validResponses (.validResponses qc))
    (swap! botResponses (.botResponses qc))
    (swap! correlations (correlations @validResponses (.survey qc)))
)

(defn staticAnalyses
    [^QC qc]
    (swap! staticMaxEntropy (QCMetrics/getMaxPossibleEntropy (.survey qc)))
    (swap! avgPathLength (QCMetrics/averagePathLength (.survey qc)))
    (swap! maxPathLength (QCMetrics/maximumPathLength (.survey qc)))
    (swap! minPathLength (QCMetrics/minimumPathLength (.survey qc)))
)

(defn printStaticAnalyses
    []
    (printf "Average path length: %f\n" @avgPathLength)
    (printf "Max possible bits to represent this survey: %f\n" staticMaxEntropy)
)

(defn printDynamicAnalyses
    []
    (printf "Total number of classified bots : %d\n" (count @botResponses))
    (printf "Bot classification threshold: %f\n")
    (printf "Correlations with a coefficient above %f" @correaltionThreshhold)
    (doseq [{[q1 ct1] :q1&ct [q2 ct2] :q2&ct {coeff :coeff val :val :as corr} :corr} @correlations]
        (if (> val @correaltionThreshhold)
            (printf "Question 1: %s (%s)\t Question 2: %s (%s)\ncoeffcient type : %s\nother data : %s"
                    (.toString q1) (.quid q1)
                    (.toString q2) (.quid q2)
                    coeff corr)
            )
        )
)
