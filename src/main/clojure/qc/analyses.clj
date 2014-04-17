;; dynamic analyses for SurveyMan
(ns qc.analyses
    (:gen-class
        :name qc.analyses
        :methods [#^{:static true} [getCorrelations [java.util.List survey.Survey] java.util.List]])
    (:import (java.util List))
    (:import (survey Survey Question Component SurveyResponse SurveyResponse$QuestionResponse))
    (:require [incanter core stats]
              [clojure.math.numeric-tower :as math])
)

(defn- make-ans-map
    "Takes each question and returns a map from questions to a list of question responses.
     The survey response id is attached as metadata."
    [surveyResponses]
    (let [answers (for [^SurveyResponse sr surveyResponses]
                      (apply merge (for [^SurveyResponse$QuestionResponse qr (.responses sr)]
                                       {(.q qr) [(.srid sr) (.opts qr)]})))]
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

(defn correlation
    [surveyResponses ^Survey survey]
    (let [ansMap (make-ans-map surveyResponses)]
        (for [^Question q1 (.questions survey)
              ^Question q2 (.questions survey)]
            (let [ans1 (ansMap q1)
                  ans2 (ansMap q2)]
                ;; make sure order is retained
                (assert (every? identity (map #(= (%1 0) (%2 0)) ans1 ans2)))
                { :q1&ct [q1 (count ans1)]
                  :q2&ct [q2 (count ans2)]
                  :corr (if (and (.exclusive q1) (.exclusive q2))
                            (letfn [(getOrderings [q]  (apply hash-map (range 1 (count (.getOptListByIndex q))) (.getOptListByIndex q)))]
                                (if (and (.ordered q1) (.ordered q2))
                                    (let [r1 (getOrderings q1)
                                          r2 (getOrderings q2)]
                                        {:coeff 'rho
                                         :val (incanter.stats/spearmans-rho (map #(get r1 %) ans1) (map #(get r2 %) ans2))}
                                    )
                                    (let [tab (->> (for [opt1 (.getOptListByIndex q1) opt2 (.getOptListByIndex q2)]
                                                        ;; count the number of people who answer both opt1 and opt2
                                                        (let [answeredOpt1 (set (map #(% 0) (flatten (filter #(= (% 1) opt1) (ansMap q1)))))
                                                              answeredOpt2 (set (map #(% 0) (flatten (filter #(= (% 1) opt2) (ansMap q2)))))]
                                                                  (count (clojure.set/intersection answeredOpt1 answeredOpt2))))
                                                   (partition (count (.getOptListByIndex q1)))
                                                   (incanter.core/matrix))
                                          {X-sq :X-sq :as data} (incanter.stats/chisq-test :table tab)
                                          N (reduce + (flatten tab))
                                          k (min (tab :cols) (tab :rows))]
                                        (merge data {:coeff 'V
                                                     :val (math/sqrt (/ X-sq (* N (dec k))))})
                                    )
                                )
                            )
                         )
                  }
                )
            )
        )
    )

(defn -getCorrelations
    [surveyResponses survey]
    (correlation surveyResponses survey)
)



