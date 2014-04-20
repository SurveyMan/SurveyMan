(ns qc.report
    (:import (qc QC QCMetrics)
             (survey Question Survey))
    (:use qc.analyses)
    )

(def validResponses (atom nil))
(def botResponses (atom nil))
(def breakoffQuestions (atom nil))
(def orderBiases (atom nil))
(def variants (atom nil))
(def staticMaxEntropy (atom 0.0))
(def avgPathLength (atom 0.0))
(def maxPathLength (atom 0))
(def minPathLength (atom 0))
(def correlations (atom nil))
(def correlationThreshhold (atom 0.5))

(defn expectedCorrelation
    [^Survey survey ^Question q1 ^Question q2]
    (some?
        (map #(and (contains? q1 (set %)) (contains? q2 (set %)))
              (vals (.correlationMap survey)))
    )
)

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
    [^Survey survey]
    (printf "Total number of classified bots : %d\n" (count @botResponses))
    (printf "Bot classification threshold: %f\n")
    (printf "Correlations with a coefficient above %f" @correlationThreshhold)
    (doseq [{[^Question q1 ct1] :q1&ct [^Question q2 ct2] :q2&ct {coeff :coeff val :val :as corr} :corr} @correlations]
        (when (and (expectedCorrelation survey q1 q2) (<= val @correlationThreshhold))
            (printf "Did not detect expected correlation between %s (%s) and %s (%s)"
                    (.toString q1) (.quid q1)
                    (.toString q2) (.quid q2)))
        (when (> val @correlationThreshhold)
            (printf "Question 1: %s (%s)\t Question 2: %s (%s)\ncoeffcient type : %s\nexpected?\n%sother data : %s"
                    (.toString q1) (.quid q1)
                    (.toString q2) (.quid q2)
                    coeff
                    (expectedCorrelation survey q1 q2)
                    corr)
            )
        )
)