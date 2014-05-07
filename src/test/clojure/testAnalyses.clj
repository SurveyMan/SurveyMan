(ns testAnalyses
    (:import (interstitial IQuestionResponse ISurveyResponse OptTuple)
             (qc IQCMetrics))
    (:import (qc RandomRespondent RandomRespondent$AdversaryType)
             (input.csv CSVLexer CSVParser)
             (survey Survey Question Component)
             (org.apache.log4j Logger))
    (:use clojure.test)
    (:use testLog)
    (:require (qc analyses))
    )

(def correlationThreshhold 0.5)
(def alpha 0.05)
(def falseCorrelations (atom 0))
(def totalTested (atom 0))
(def falseOrderBias (atom 0))

(deftest test-random-responses
    (doseq [[filename sep] tests]
        (println "\ntest-random-responses" filename)
        (let [^Survey survey (makeSurvey filename sep)
              responses (generateNRandomResponses survey)]
            (doseq [^ISurveyResponse response responses]
                (doseq [^IQuestionResponse qr (.getResponses response)]
                    (doseq [^OptTuple optTupe (.getOpts qr)]
                        (when-not (or (.isEmpty (.c optTupe))
                                      (.freetext (.getQuestion qr))
                                      (empty? (.options (.getQuestion qr))))
                            (if-let [opts (filter #(not (.isEmpty %)) (vals (.options (.getQuestion qr))))]
                                (is (contains? (set opts) (.c optTupe)))
                                )
                            )
                        )
                    )
                )
            )
        )
    )

(deftest test-answer-map
    (doseq [[filename sep] tests]
        (println "\ntest-answer-map" filename)
        (let [ansMap (->> (makeSurvey filename sep)
                          (generateNRandomResponses)
                          (qc.analyses/make-ans-map))]
            (doseq [k (keys ansMap)]
                (when-not (.freetext k)
                    (doseq [^qc.analyses/Response r (ansMap k)]
                        (let [optSet (set (map #(.getCid %) (.getOptListByIndex k)))]
                            (when-not (empty? optSet)
                                (is (contains? optSet (.getCid (first (:opts r))))))
                            )
                        )
                    )
                )
            )
        )
    )

(deftest test-ordered
    (doseq [[filename sep] tests]
        (println "\ntest-ordered" filename)
        (let [^Survey survey (makeSurvey filename sep)]
            (doseq [q (.questions survey)]
                (when-not (.freetext q)
                    (doseq [opt (vals (.options q))]
                        (is (= (qc.analyses/getOrdered q opt)
                               (-> (map #(.getCid %) (sort-by #(.getSourceRow %) (vals (.options q))))
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
        (println "\ntest-align-by-srid" filename)
        (let [^Survey survey (makeSurvey filename sep)
              responses (generateNRandomResponses survey)
              ]
            (doseq [^Question q1 (.questions survey) ^Question q2 (.questions survey)]
                (let [ansMap (qc.analyses/make-ans-map responses)
                      [ans1 ans2] (qc.analyses/align-by-srid (ansMap q1) (ansMap q2))]
                    (is (every? identity (map #(= (:srid %1) (:srid %2)) ans1 ans2)))
                    (is (= (count ans1) (count ans2)))
                    )
                )
            )
        )
    )

(deftest test-correlation
    (doseq [[filename sep] tests]
        (println "\ntest-correlation:" filename)
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
        )
        (printf "Number false correlations for %s: %d\n" filename @falseCorrelations)
        (printf "\tTotal comparisons : %d\n" @totalTested)
        (reset! totalTested 0)
        (reset! falseCorrelations 0)
    )
)

(deftest test-orderBias
    (doseq [[filename sep] tests]
        (println "\ntest-orderBias" filename)
        (let [^Survey survey (makeSurvey filename sep)
              responses (generateNRandomResponses survey)
              ob (qc.analyses/orderBias responses survey)]
            (doseq [{q1 :q1 q2 :q2 num1 :numq1First num2 :numq2First {stat :stat val :val} :order} ob]
                (when val
                    (when (< (val :p-value) alpha)
                        (.warn LOGGER (format (str "Random respondent generated order bias (%s = %s) between"
                                                   "%s (quid : %s, ct : %s, numOpts : %d and"
                                                   "%s (quid : %s, ct : %s, numOpts : %d\n")
                                              stat val
                                              q1 (.quid q1) num1 (count (.options q1))
                                              q2 (.quid q2) num2 (count (.options q2))))
                        (swap! falseOrderBias inc)
                        )
                    (swap! totalTested inc)
                    )
                )
            )
        (printf "Number false order bias for %s : %d\n" filename @falseOrderBias)
        (printf "\tTotal comparisons : %d\n" @totalTested)
        (reset! falseOrderBias 0)
        (reset! totalTested 0)
        )
    )

(deftest test-variantBias
    (doseq [[filename sep] tests]
        (println "\ntest-variantBias" filename)
        (let [^Survey survey (makeSurvey filename sep)
              responses (generateNRandomResponses survey)
              variantsList (flatten (qc.analyses/wordingBias responses survey))]
            (doseq [variants variantsList]
                (doseq [{q1 :q1 q2 :q2 num1 :numq1First num2 :numq2First {stat :stat val :val} :order} variants]
                    (when val
                        (when (< (val :p-value) alpha)
                            (.warn LOGGER (format (str "Random respondent generated variant bias (%s = %f) between"
                                                       "%s (quid : %s, ct : %s, numOpts : %d and"
                                                       "%s (quid : %s, ct : %s, numOpts : %d\n")
                                                  stat val
                                                  q1 (.quid q1) num1 (count (.options q1))
                                                  q2 (.quid q2) num2 (count (.options q2))))
                            (swap! falseOrderBias inc)
                            )
                        (swap! totalTested inc)
                        )
                    )
                )
            (printf "Number false order bias for %s : %d\n" filename @falseOrderBias)
            (printf "\tTotal comparisons : %d\n" @totalTested)
            (reset! falseOrderBias 0)
            (reset! totalTested 0)
            )
        )
    )

(deftest test-max-entropy
    (doseq [[filename sep] tests]
        (println "\nt test-max-entropy" filename)
        (let [^Survey survey (makeSurvey filename sep)
              responses (generateNRandomResponses survey)
              ]
            (is (> (.getMaxPossibleEntropy ^IQCMetrics qcMetrics survey)
                   (.surveyEntropy ^IQCMetrics qcMetrics survey responses)))
            )
        )
    )
