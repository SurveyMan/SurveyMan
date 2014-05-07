(ns qc.metrics
    (:gen-class
        :name qc.Metrics
        :implements [qc.IQCMetrics]
        )
    (:import (interstitial IQuestionResponse ISurveyResponse OptTuple)
             (survey Block$BranchParadigm)
             (java.util Collections))
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

(defn log2
    [x]
    (/ (Math/log x) (Math/log 2.0))
    )

(defn getRandomSurveyResponses
    [survey n]
    (clojure.core/repeatedly n #(RandomRespondent. survey RandomRespondent$AdversaryType/UNIFORM))
    )

(defn make-frequencies
    [responses]
    (reduce #(merge-with (fn [m1 m2] (merge-with + m1 m2)) %1 %2)
            (for [^ISurveyResponse sr responses]
                (apply merge (for [^IQuestionResponse qr (.getResponses sr)]
                                 {(.quid (.getQuestion qr)) (apply merge (for [^Component c (map #(.c ^OptTuple %) (.getOpts qr))]
                                                                   {(.getCid c) 1}
                                                                   )
                                                         )
                                  }
                                 )
                       )
                )
            )
    )

(defn make-probabilities
    [^Survey s frequencies]
    (apply merge (for [^Question q (.questions s)]
                     (let [quid (.quid q)
                           ct (reduce + (vals (frequencies (.quid q) 0)))]
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

(defn get-path
    [^ISurveyResponse r]
    (set (map #(.block (.getQuestion ^IQuestionResponse %))
              (.getResponses r)))
    )

(defn make-frequencies-for-paths
    [paths responses]
    (reduce #(merge-with concat %1 %2) (for [^ISurveyResponse r responses]
                                       (apply merge (for [path (seq paths)]
                                                        (if (clojure.set/subset? (set path) (set (get-path r)))
                                                            {path (list r)}
                                                            {}
                                                            )
                                                        )
                                              )
                                       )
            )
    )

(defn get-dag
    [blockList]
    "Takes in a list of Blocks; returns a list of lists of Blocks."
    (if (empty? blockList)
        '(())
        (let [^Block this-block (first blockList)]
            (if-let [branchMap (and (.branchQ this-block) (.branchMap (.branchQ this-block)))]
                (let [dests (set (vals branchMap))
                      blists (map (fn [^Block b] (drop-while #(not= % b) blockList)) (seq dests))]
                    (map #(flatten (cons this-block %)) (map get-dag blists))
                    )
                (map #(flatten (cons this-block %)) (get-dag (rest blockList)))
                )
            )
        )
    )

(defn get-paths
    "Returns paths through **blocks** in the survey. Top level randomized blocks are all listed last"
    [^Survey survey]
    (let [partitioned-blocks (Interpreter/partitionBlocks survey)
          top-level-randomizable-blocks (Block/sort (.get partitioned-blocks true))
          nonrandomizable-blocks (Block/sort (.get partitioned-blocks false))
          dag (get-dag nonrandomizable-blocks)
          ]
        (assert (coll? (first dag)) (type (first dag)))
        (assert (= Block (type (ffirst dag))) (type (ffirst dag)))
        (map #(concat % top-level-randomizable-blocks) dag)
        )
    )

(defn get-variants
    [^Question q]
    (if (= (.branchParadigm (.block q)) Block$BranchParadigm/ALL)
        (.questions (.block q))
        (list q)
        )
    )

(defn get-equivalent-answer-variants
    "Returns equivalent answer options (a list of survey.Component)"
    [^Question q ^Component c]
    (let [variants (get-variants q)
          offset (- (.getSourceRow q) (.getSourceRow c))
          ]
        (flatten (for [^Question variant variants]
                     (filter #(= (- (.getSourceRow variant) (.getSourceRow %)) offset) (vals (.options variant)))
                     )
                 )
        )
    )

(defn survey-response-contains-answer
    [^ISurveyResponse sr ^Component c]
    (contains? (set (flatten (map (fn [^IQuestionResponse qr] (map #(.c %) (.getOpts qr))) (.getResponses sr))))
               c
               )
    )

(defn -surveyEntropy
    [^IQCMetrics _ ^Survey s responses]
    (let [paths (set (get-paths s))
          path-map (make-frequencies-for-paths paths responses) ;; map from paths to survey responses
          total-responses (count responses)
          ]
        (->> (for [^Question q (.questions s)]
                (for [^Component c (vals (.options q))]
                    (for [path paths]
                        (let [variants (get-equivalent-answer-variants q c) ;; all valid answers (survey.Component) in the answer set
                              responses-this-path (get path-map path) ;; list of survey responses
                              ans-this-path (map (fn [^Component cc]
                                                     (filter #(survey-response-contains-answer % cc) responses-this-path))
                                                 variants)
                              p (/ (count ans-this-path) total-responses)
                          ]
                            (* p (log2 p))))))
            (flatten)
            (reduce +)
            (* -1.0))))

(defn get-questions
    [blockList]
    (if (empty? blockList)
        '()
        (flatten (map (fn [^Block b]
                          (if  (= (.branchParadigm b) Block$BranchParadigm/ALL)
                              (take 1 (shuffle (.questions b))) ;;all options should have the same arity
                              (concat (.questions b) (get-questions (.subBlocks b)))
                              )
                          )
                      blockList)
                 )
        )
    )

(defn max-entropy-one-question
    [^Question q]
    (->> (count (.options q))
         (#(if (= 0 %) 0 (/ 1 %)))
         (#(if (= 0 %) 0 (log2 %)))
         (math/abs)
         )
    )

(defn max-entropy-qlist
    [qlist]
    (reduce + (map max-entropy-one-question qlist)))

(defn get-max-path-for-entropy
    "Returns the path with the highest entropy."
    [bLists]
    (assert (seq? (first bLists)))
    (let [entropies (map max-entropy-qlist (map get-questions bLists))
          pairs (map vector entropies bLists)
          max-ent (apply max entropies)]
        ((first (filter #(= (% 0) max-ent) pairs)) 1)
        )
    )

(defn -getMaxPossibleEntropy
    [^IQCMetrics _ ^Survey s]
    (->> (get-max-path-for-entropy (get-paths s))
         (get-questions)
         (max-entropy-qlist)
         )
    )

(defn pathLength
    [^Survey survey ^PathMetric metric]
    ;;get size of all top level randomizable blocks
    (let [paths (get-paths survey)]
        (cond (= metric PathMetric/MAX) (apply max (map count paths))
              (= metric PathMetric/MIN) (apply min (map count paths))
              :else (throw (Exception. (str "Unknown path metric " metric)))
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
    (let [probabilities (make-probabilities s (make-frequencies responses))
          ents (calculate-entropies responses probabilities)
          thisEnt (getEntropyForResponse s probabilities)
          bs-sample (incanter.stats/bootstrap ents incanter.stats/mean)
          p-val (incanter.stats/quantile bs-sample :probs [(- 1 @alpha)])
         ]
        (> thisEnt p-val)
        )
    )