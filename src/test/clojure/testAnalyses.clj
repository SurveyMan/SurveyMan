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

(def responseLookup (atom {}))

(pmap (fn [[filename sep]]
          (let [^Survey survey (makeSurvey filename sep)
                responses (generateNRandomResponses survey)]
              (swap! responseLookup assoc survey responses)
              )
          )
      tests)

(deftest test-random-responses
    (println 'test-random-responses)
    (doseq [responses (vals @responseLookup)]
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

(deftest test-answer-map
    (println 'test-answer-map)
    (doseq [responses (vals @responseLookup)]
        (let [ansMap (qc.analyses/make-ans-map responses)]
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
    (println 'test-ordered)
    (doseq [survey (keys @responseLookup)]
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

(deftest test-align-by-srid
    (println 'test-align-by-srid)
    (doseq [[survey responses] (seq @responseLookup)]
        (doseq [^Question q1 (.questions survey) ^Question q2 (.questions survey)]
            (print ".")
            (let [ansMap (qc.analyses/make-ans-map responses)
                  [ans1 ans2] (qc.analyses/align-by-srid (ansMap q1) (ansMap q2))]
                (is (every? identity (map #(= (:srid %1) (:srid %2)) ans1 ans2)))
                (is (= (count ans1) (count ans2)))
                )
            )
        )
    (printf "\n") (flush)
    )

(deftest test-correlation
    (println 'test-correlation)
    (doseq [[survey responses] (seq @responseLookup)]
        (let [correlations (qc.analyses/correlation responses survey)]
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
        (printf "\nNumber false correlations for %s: %d\n" (.sourceName survey) @falseCorrelations)
        (printf "\tTotal comparisons : %d\n" @totalTested)
        (flush)
        (reset! totalTested 0)
        (reset! falseCorrelations 0)
    )
)

(deftest test-orderBias
    (println 'test-orderBias)
    (doseq [[survey responses] (seq @responseLookup)]
        (let [ob (qc.analyses/orderBias responses survey)]
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
        (printf "Number false order bias for %s : %d\n" (.sourceName survey) @falseOrderBias)
        (printf "\tTotal comparisons : %d\n" @totalTested)
        (flush)
        (reset! falseOrderBias 0)
        (reset! totalTested 0)
        )
    )

(deftest test-variantBias
    (println 'test-variantBias)
    (doseq [[survey responses] (seq @responseLookup)]
        (let [variantsList (flatten (qc.analyses/wordingBias responses survey))]
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
            (printf "Number false variant bias for %s : %d\n" (.sourceName survey) @falseOrderBias)
            (printf "\tTotal comparisons : %d\n" @totalTested)
            (flush)
            (reset! falseOrderBias 0)
            (reset! totalTested 0)
            )
        )
    )

(deftest test-max-entropy
  (println 'test-max-entropy)
  (doseq [[survey responses] (seq @responseLookup)]
    (println (.sourceName survey))
    (is (>= (.getMaxPossibleEntropy ^IQCMetrics qcMetrics survey)
            (.surveyEntropy ^IQCMetrics qcMetrics survey responses)))
      )
  )