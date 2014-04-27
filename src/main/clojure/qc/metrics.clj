(ns qc.metrics
    (:gen-class
        :name qc.Metrics
        :implements [qc.QCMetrics]
        )
    (:import (qc QCMetrics PathMetric RandomRespondent RandomRespondent$AdversaryType)
             (survey Survey SurveyResponse Question SurveyResponse$QuestionResponse Component Block
                     Block$BranchParadigm)
             (system Interpreter Library))
    (:require [clojure.math.numeric-tower :as math])
    )

(def alpha (atom 0.05))
(def bootstrap-reps (atom 1000))


(defn getRandomSurveyResponses
    [survey n]
    (clojure.core/repeatedly n #(RandomRespondent. survey RandomRespondent$AdversaryType/UNIFORM))
    )

(defn makeFrequencies
    [responses]
    (reduce #(merge-with (fn [m1 m2] (merge-with + m1 m2)) %1 %2)
            (for [^SurveyResponse sr responses]
                (apply merge (for [^SurveyResponse$QuestionResponse qr (.responses sr)]
                                 {(.quid (.q qr)) (apply merge (for [^Component c (.opts qr)]
                                                                   {(.getCid c) 1}
                                                                   )
                                                         )
                                  }
                                 )
                       )
                )
            )
    )

(defn makeProbabilities
    [^Survey s frequencies]
    (apply merge (for [^Question q (.questions s)]
                     (let [quid (.quid q)
                           ct (frequencies (.quid q) 0)]
                         {quid (apply merge (for [^String cid (keys (.options q))]
                                                {cid (let [freq ((frequencies quid {cid 0}) cid 0)]
                                                         (if (= ct 0) 0.0 (/ freq ct)))
                                                 }
                                                )
                                      )
                          }
                         )
                     )
           )
    )

(defn -surveyEntropy
    [^QCMetrics _ ^Survey s responses]
    (let [f (makeFrequencies responses)
          p (makeProbabilities s f)]
        (->> (vals p)
             (map vals)
             (flatten)
             (map #(* % (/ (Math/log %) (Math/log 2.0))))
             (reduce +)
             (* -1.0)
             )
        )
    )

(defn -getMaxPossibleEntropy
    [^QCMetrics _ ^Survey s]
    (->> (map #(count (.options %)) (.questions s))
         (map #(if (= 0 %) 0 (/ 1 %)))
         (map #(if (= 0 %) 0 (* % (/ (Math/log %) (Math/log 2)))))
         (reduce +)
         (* -1.0)
         )
    )

(defn getDests [^Block thisBlock]
    (if-let [branchQ (.branchQ thisBlock)]
        (-> (vals (.branchMap branchQ))
            (#(if (first %) (Block/sort %) %))))
    )

(defn pathLength
    [^Survey survey ^PathMetric metric]
    ;;get size of all top level randomizable blocks
    (let [partitionedBlocks (Interpreter/partitionBlocks survey)
          size (reduce + 0 (map #(.dynamicQuestionCount ^Block %) (.get partitionedBlocks true)))
          ;;find the max path through the nonrandomizable blocks
          blocks (Block/sort (.get partitionedBlocks false))
          ]
        (loop [^Block bs blocks
               ^Block branchDest nil
               ct size]
            (if (empty? bs)
                ct
                (let [^Block thisBlock (first bs) ctThisBlock (.dynamicQuestionCount thisBlock)]
                    (cond (and branchDest (not= thisBlock branchDest)) (recur (rest bs) branchDest ct)
                          (or (and branchDest (= thisBlock branchDest))
                              (.branchQ thisBlock))
                              (let [dests (getDests thisBlock)]
                                  (cond (= metric PathMetric/MAX) (recur (rest bs) (first dests) (+ ct ctThisBlock))
                                        (= metric PathMetric/MIN) (recur (rest bs) (last dests) (+ ct ctThisBlock))
                                        :else (throw (Exception. (str "Unknown path metric " metric)))
                                        )
                                  )
                          :else (recur (rest bs) branchDest (+ ct ctThisBlock))
                          )
                    )
                )
            )
        )
    )


(defn -minimumPathLength
    [^QCMetrics _ ^Survey s]
    (pathLength s PathMetric/MIN))

(defn -maximumPathLength
    [^QCMetrics _ ^Survey s]
    (pathLength s PathMetric/MAX))

(defn -averagePathLength
    [^QCMetrics _ ^Survey s]
    (let [n 5000]
        (/ (reduce + (map #(count (.responses (.response %))) (getRandomSurveyResponses s n))) n)
        )
    )

(defn -getBasePay
    [^QCMetrics _ ^Survey s]
    (* (-minimumPathLength s) Library/timePerQuestionInSeconds (/ Library/FEDMINWAGE 3600))
    )

(defn getEntropyForResponse
    [^SurveyResponse sr probabilities]
    (reduce + (flatten (->> (.responses sr)
                            (map #(map (fn [^Component c]
                                           ((probabilities (.quid (.q ^SurveyResponse$QuestionResponse %)))
                                            (.getCid c)))
                                       (.opts %))
                                 )
                            )
                       )
            )
    )

(defn calculate-entropies
    [responses probabilities]
    (map #(getEntropyForResponse % probabilities) responses)
    )

(defn bootstrap-sample
    [data]
    
    )

(defn mean
    [stuff]
    (/ (reduce + stuff) (count stuff)))

(defn is-outlier
    [^SurveyResponse s bs-sample statistic]
    )

(defn -entropyClassification
    [^QCMetrics _ ^SurveyResponse s responses]
    ;; find outliers in the empirical entropy
    (let [probabilities (makeProbabilities s (makeFrequencies responses))
          ents (calculate-entropies responses probabilities)
          bs-sample (bootstrap-sample ents)]
        (is-outlier s bs-sample mean )
        )
    true)