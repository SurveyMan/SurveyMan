(ns testAnalyses
  (:import (interstitial IQuestionResponse ISurveyResponse OptTuple)
           (qc IQCMetrics))
  (:import (qc RandomRespondent RandomRespondent$AdversaryType)
           (input.csv CSVLexer CSVParser)
           (survey Survey Question Component)
           (org.apache.log4j Logger))
  (:use clojure.test)
  (:use testLog)
  (:require (qc analyses response-util))
  )

(deftest test-random-responses
  (println 'test-random-responses)
  (doseq [responses (vals @response-lookup)]
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
  (doseq [responses (vals @response-lookup)]
    (let [ansMap (qc.response-util/make-ans-map responses)]
      (doseq [k (keys ansMap)]
        (when-not (.freetext k)
          (doseq [^qc.response-util/Response r (ansMap k)]
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
  (doseq [survey (keys @response-lookup)]
    (doseq [q (.questions survey)]
      (when-not (.freetext q)
        (doseq [opt (vals (.options q))]
          (is (= (qc.response-util/getOrdered q opt)
                 (-> (map #(.getCid %) (sort-by #(.getSourceRow %) (vals (.options q))))
                     (.indexOf (.getCid opt))
                     (inc))))
          )
        )
      )
    )
  )

(deftest test-max-entropy
  (println 'test-max-entropy)
  (doseq [[survey responses] (seq @response-lookup)]
    (println (.sourceName survey))
    (is (>= (.getMaxPossibleEntropy ^IQCMetrics qcMetrics survey)
            (.surveyEntropy ^IQCMetrics qcMetrics survey responses)))
      )
  )