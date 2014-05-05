(ns qc.metrics
    (:gen-class
        :name qc.Metrics
        :implements [qc.IQCMetrics]
        )
    (:import (interstitial IQuestionResponse ISurveyResponse))
    (:import (qc QC IQCMetrics Interpreter PathMetric RandomRespondent RandomRespondent$AdversaryType)
             (survey Question Component Block Block$BranchParadigm Survey))
    (:require [clojure.math.numeric-tower :as math]
              [incanter.stats])
    )

(def alpha (atom 0.05))
(def bootstrap-reps (atom 1000))
;; these should be read in from a config file and be the same as those in Library
(def FEDMINWAGE 7.25)
(def timePerQuestionInSeconds 10)

(defn getRandomSurveyResponses
    [survey n]
    (clojure.core/repeatedly n #(RandomRespondent. survey RandomRespondent$AdversaryType/UNIFORM))
    )

(defn makeFrequencies
    [responses]
    (reduce #(merge-with (fn [m1 m2] (merge-with + m1 m2)) %1 %2)
            (for [^ISurveyResponse sr responses]
                (apply merge (for [^IQuestionResponse qr (.getResponses sr)]
                                 {(.quid (.getQuestion qr)) (apply merge (for [^Component c (.getOpts qr)]
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
    [^IQCMetrics _ ^Survey s responses]
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
    [^IQCMetrics _ ^Survey s]
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
    [^IQCMetrics _ ^Survey s]
    (pathLength s PathMetric/MIN))

(defn -maximumPathLength
    [^IQCMetrics _ ^Survey s]
    (pathLength s PathMetric/MAX))

(defn -averagePathLength
    [^IQCMetrics _ ^Survey s]
    (let [n 5000]
        (/ (reduce + (map #(count (.getResponses (.response %))) (getRandomSurveyResponses s n))) n)
        )
    )

(defn -getBasePay
    [^IQCMetrics _ ^Survey s]
    (* (-minimumPathLength s) timePerQuestionInSeconds (/ FEDMINWAGE 3600))
    )

(defn getEntropyForResponse
    [^ISurveyResponse sr probabilities]
    (reduce + (flatten (->> (.getResponses sr)
                            (map #(map (fn [^Component c]
                                           ((probabilities (.quid (.getQuestion ^IQuestionResponse %)))
                                            (.getCid c)))
                                       (.getOpts %))
                                 )
                            )
                       )
            )
    )

(defn calculate-entropies
    [responses probabilities]
    (map #(getEntropyForResponse % probabilities) responses)
    )


(defn -calculateBonus [^IQCMetrics _ ^ISurveyResponse sr ^QC qc]
    "For now this is very simple -- just pay $0.01 more for each question answered for valid responses"
    (if (.contains (.validResponses qc) sr)
        (* 0.01 (count (.getResponses sr)))
        0.0)
    )

(defn -entropyClassification
    [^IQCMetrics _ ^ISurveyResponse s responses]
    ;; find outliers in the empirical entropy
    (let [probabilities (makeProbabilities s (makeFrequencies responses))
          ents (calculate-entropies responses probabilities)
          thisEnt (getEntropyForResponse s probabilities)
          bs-sample (incanter.stats/bootstrap ents incanter.stats/mean)
          p-val (incanter.stats/quantile bs-sample :probs [(- 1 @alpha)])
         ]
        (> thisEnt p-val)
        )
    )