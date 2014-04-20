(ns testAnalyses
    (:import (qc RandomRespondent RandomRespondent$AdversaryType)
             (csv CSVLexer CSVParser)
             (survey Survey Question Component)
             (org.apache.log4j Logger))
    (:use clojure.test)
    (:require testLog)
    (:require (qc analyses))
    )

(def numResponses 1000)
(def correlationThreshhold 0.5)
(def alpha 0.05)
(def falseCorrelations (atom 0))
(def totalTested (atom 0))
(def falseOrderBias (atom 0))
(def tests (list (first testLog/tests)))
(def LOGGER testLog/LOGGER)

(defn getRandomSurveyResponses
    [survey n]
    (clojure.core/repeatedly n #(RandomRespondent. survey RandomRespondent$AdversaryType/UNIFORM))
)

(defn makeSurvey
    [filename sep]
    (->> (CSVLexer. filename sep)
         (CSVParser.)
         (.parse))
    )

(defn generateNRandomResponses
    [survey]
    (map (fn [^RandomRespondent rr] (.response rr))
         (getRandomSurveyResponses survey numResponses))
    )


(deftest test-answer-map
    (doseq [[filename sep] tests]
        (println "test-answer-map" filename)
        (let [ansMap (->> (makeSurvey filename sep)
                          (generateNRandomResponses)
                          (qc.analyses/make-ans-map))]
            (doseq [k (keys ansMap)]
                (when-not (.freetext k)
                    (doseq [^qc.analyses/Response r (ansMap k)]
                        (is (contains? (set (map #(.getCid %) (.getOptListByIndex k))) (.getCid (first (:opts r)))))
                        )
                    )
                )
            )
        )
    )

(deftest test-ordered
    (doseq [[filename sep] tests]
        (println "test-ordered" filename)
        (let [^Survey survey (makeSurvey filename sep)]
            (doseq [q (.questions survey)]
                (when-not (.freetext q)
                    (doseq [opt (vals (.options q))]
                        (is (= (qc.analyses/getOrdered q opt)
                               (-> (sort-by #(.getSourceRow %) (vals (.options q)))
                                   (.indexOf (.getCid opt))
                                   (inc))))
                        )
                    )
                )
            )
        )
    )

(deftest test-align-by-srid
    (doseq [[filename sep] tests]
        (println "test-align-by-srid" filename)
        (let [^Survey survey (makeSurvey filename sep)]
            ;; is this necessary?
            )
        )
    )

(deftest test-correlation
    (doseq [[filename sep] tests]
        (println "test-correlation:" filename)
        (let [^Survey survey (makeSurvey filename sep)
              responses (generateNRandomResponses survey)
              correlations (qc.analyses/correlation responses survey)]
            (doseq [{[^Question q1 ct1] :q1&ct [^Question q2 ct2] :q2&ct {coeff :coeff val :val} :corr} correlations]
                (when (and coeff val)
                    (if (= q1 q2)
                        (is (= 1.0 val))
                        (when (> val correlationThreshhold)
                            (.warn LOGGER (format (str "Random respondent generated a correlation %s = %f > %f for questions"
                                                        "%s (quid : %s, ct : %d, numOpts : %d) and "
                                                        "%s (quid : %s, ct : %d, numOpts : %d)\n")
                                                  coeff val correlationThreshhold
                                                  q1 (.quid q1) ct1 (count (.options q1))
                                                  q2 (.quid q2) ct2 (count (.options q2))))
                            (swap! falseCorrelations inc)
                        )
                    )
                    (swap! totalTested inc)
                )
            )
            (printf "Number false correlations for %s: %d\n" filename @falseCorrelations)
            (printf "\tTotal comparisons : %d\n" @totalTested)
        )
        (reset! totalTested 0)
        (reset! falseCorrelations 0)
    )
)

(deftest test-orderBias
    (doseq [[filename sep] tests]
        (println filename)
        (let [^Survey survey (makeSurvey filename sep)
              responses (generateNRandomResponses survey)
              ob (qc.analyses/orderBias responses survey)]
            (doseq [{q1 :q1 q2 :q2 num1 :numq1First num2 :numq2First {stat :stat val :val} :order} ob]
                (when (and val (< val alpha))
                    (.warn LOGGER (format (str "Random respondent generated order bias (%s = %f) between"
                                               "%s (quid : %s, ct : %s, numOpts : %d and"
                                               "%s (quid : %s, ct : %s, numOpts : %d\n")
                                          stat val
                                          q1 (.quid q1) num1 (count (.options q1))
                                          q2 (.quid q2) num2 (count (.options q2))))
                    (swap! falseOrderBias inc)
                    )
                )
            )
        (printf "Number false order bias for %s : %d\n" filename @falseOrderBias)
        )
    (reset! falseOrderBias 0)
    )

(deftest test-variantBias
    (doseq [[filename sep] tests]
        (println filename)
        (let [^Survey survey (makeSurvey filename sep)
              responses (generateNRandomResponses survey)
              variantsList (qc.analyses/wordingBias responses survey)]
            )
        )
    )