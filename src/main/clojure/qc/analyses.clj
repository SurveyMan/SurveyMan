;; dynamic analyses for SurveyMan
(ns qc.analyses
    (:gen-class
        :name qc.analyses
        :methods [#^{:static true} [getCorrelations [java.util.List survey.Survey] java.util.List]])
    (:import (java.util List)
             (org.apache.log4j Logger)
             (org.apache.commons.math3.stat.inference MannWhitneyUTest)
             )
    (:import (survey Survey Question Component SurveyResponse SurveyResponse$QuestionResponse))
    (:require [incanter core stats]
              [clojure.math.numeric-tower :as math])
)

(def LOGGER (Logger/getLogger (str (ns-name *ns*))))
(def srid 0)
(def opts 1)
(def indexSeen 2)

(defn- make-ans-map
    "Takes each question and returns a map from questions to a list of question responses.
     The survey response id is attached as metadata."
    [surveyResponses]
    (let [answers (for [^SurveyResponse sr surveyResponses]
                      (apply merge (for [^SurveyResponse$QuestionResponse qr (.responses sr)]
                                       {(.q qr) [(.srid sr) (.c (first (.opts qr))) (.indexSeen qr)]})))]
        (loop [answerMaps answers retval {}]
            (if (empty? answerMaps)
                retval
                (recur (rest answerMaps)
                       (reduce #(if (contains? %1 (%2 0))
                                    (assoc %1 (%2 0) (cons (%2 1) (retval (%2 0))))
                                    (assoc %1 (%2 0) (list (%2 1))))
                            retval
                           (seq (first answerMaps))
                       )
                )
            )
        )
    )
)

(defn- convertToOrdered
    [q opt]
    (let [m (into {} (zipmap (.getOptListByIndex q) (range 1 (count (.getOptListByIndex q)))))]
        (assert (contains? m opt) (str m opt))
        (get m opt))
)

(defn- makeContingencyTable
    [^Question q1 ^Question q2 ansMap]
        (->> (for [opt1 (.getOptListByIndex q1) opt2 (.getOptListByIndex q2)]
             ;; count the number of people who answer both opt1 and opt2
             (let [answeredOpt1 (set (map #(% srid) (flatten (filter #(= (% opts) opt1) (ansMap q1)))))
                   answeredOpt2 (set (map #(% srid) (flatten (filter #(= (% opts) opt2) (ansMap q2)))))]
                 (count (clojure.set/intersection answeredOpt1 answeredOpt2))))
         (partition (count (.getOptListByIndex q1)))
         (incanter.core/matrix))
    )

(defn correlation
    [surveyResponses ^Survey survey]
    (let [ansMap (make-ans-map surveyResponses)]
        (for [^Question q1 (.questions survey) ^Question q2 (.questions survey)]
            (let [ans1 (ansMap q1) ans2 (ansMap q2)]
                ;; make sure order is retained
                (assert (every? identity (map #(= (%1 srid) (%2 srid)) ans1 ans2)))
                { :q1&ct [q1 (count ans1)]
                  :q2&ct [q2 (count ans2)]
                  :corr (if (and (.exclusive q1) (.exclusive q2))
                            (try
                                (if (and (.ordered q1) (.ordered q2))
                                    (let [n (min (count ans1) (count ans2))]
                                        { :coeff 'rho
                                          :val (incanter.stats/spearmans-rho (map #(convertToOrdered q1 %) (take n ans1))
                                                                             (map #(convertToOrdered q2 %) (take n ans2)))
                                        }
                                    )
                                    (let [tab (makeContingencyTable q1 q2 ansMap)
                                          {X-sq :X-sq :as data} (incanter.stats/chisq-test :table tab)
                                          N (reduce + (flatten tab))
                                          k (min (tab :cols) (tab :rows))]
                                            (merge data {:coeff 'V
                                                         :val (math/sqrt (/ X-sq (* N (dec k))))
                                                        }
                                            )
                                    )
                                )
                            (catch Exception e
                                (.warn LOGGER (.getMessage e)))))
                  }
            )
        )
    )
)

(defn orderBias
    [surveyResponses ^Survey survey]
    (let [ansMap (make-ans-map surveyResponses)]
        (for [^Question q1 (.questions survey) ^Question q2 (.questions survey)]
            (let [q1ans (ansMap q1)
                  q2ans (ansMap q2)
                  { q1answersq1first true
                    q1answersq2first false} (group-by (fn [q1an]
                                                         (< (q1an indexSeen)
                                                             ((first (filter #(= (q1an srid) (% srid)) q2ans)) indexSeen)))
                                                         q1ans)
                  ]
                  { :q1 q1
                    :q2 q2
                    :numq1First (count q1answersq1first)
                    :numq2First (count q1answersq2first)
                    :order (if (.exclusive q1)
                              (if (.ordered q1)
                                  (let [ mw (MannWhitneyUTest.)
                                         x (map #(convertToOrdered q1 %) q1answersq1first)
                                         y (map #(convertToOrdered q1 %) q1answersq2first)
                                       ]
                                      { :stat 'mann-whitney
                                        :val { :U (.mannWhitneyU mw x y)
                                               :p-value (.mannWhitneyUTest mw x y)
                                             }
                                      }
                                  )
                                  (let [tab (makeContingencyTable q1 q2 ansMap)]
                                      { :stat 'chi-squared
                                        :val (incanter.stats/chisq-test :table tab)
                                      }
                                  )
                              )
                          )
                  }))))

(defn -getCorrelations
    [surveyResponses survey]
    (correlation surveyResponses survey)
)

(defn -main
    [& args]
    ()
    )


