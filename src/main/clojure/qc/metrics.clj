(ns qc.metrics
  (:gen-class
      :name qc.Metrics
      :implements [qc.IQCMetrics]
      )
  (:import (java.util Collections List))
  (:import (input AbstractParser)
           (interstitial IQuestionResponse ISurveyResponse OptTuple Record)
           (qc IQCMetrics Interpreter PathMetric)
           (system SurveyResponse)
           (survey Question Component Block Block$BranchParadigm Survey))
  (:require [clojure.math.numeric-tower :as math]
            [incanter.stats]
            [util :only log2]
            )
  (:use [qc.response-util])
  )

(def alpha (atom 0.05))
(def bootstrap-reps (atom 1000))
(def cutoffs (atom {}))
;; these should be read in from a config file and be the same as those in Library
(def FEDMINWAGE 7.25)
(def timePerQuestionInSeconds 10)
(def survey (atom nil))

(defn get-path
  "Returns the set of enclosing blocks for this survey response."
  [^ISurveyResponse r]
  (set (map #(.block (.getQuestion ^IQuestionResponse %))
          (get-true-responses r)))
  )

(defn make-frequencies-for-paths
  "Returns the counts for each path; see @etosch's blog post on the calculation."
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
  "Takes in a list of Blocks; returns a list of lists of Blocks representing all possible paths through the survey. See @etosch's blog post for more detail."
  [blockList]
  (if (empty? blockList)
    '(())
    (let [^Block this-block (first blockList)]
      (if-let [branchMap (and (.branchQ this-block) (.branchMap (.branchQ this-block)))]
        (let [dests (set (vals branchMap))
              blists (map (fn [^Block b] (drop-while #(not= % b) blockList)) (seq dests))]
          (map #(seq (set (flatten (cons this-block %)))) (map get-dag blists))
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
        top-level-randomizable-blocks (.get partitioned-blocks true)
        nonrandomizable-blocks (.get partitioned-blocks false)
        dag (do (Collections/sort nonrandomizable-blocks) (get-dag nonrandomizable-blocks))
        ]
    (assert (coll? (first dag)) (type (first dag)))
    (when-not (empty? (flatten dag))
      (assert (= Block (type (ffirst dag))) (str (type (ffirst dag)) " " (.sourceName survey))))
    (map #(concat top-level-randomizable-blocks %) dag)
    )
  )


(defn -surveyEntropy
    [^IQCMetrics _ ^Survey s responses]
    (let [paths (set (get-paths s))
          path-map (make-frequencies-for-paths paths responses) ;; map from paths to survey responses
          total-responses (count responses)
          ]
        (->> (for [^Question q (remove #(.freetext %) (.questions s))]
                (for [^Component c (vals (.options q))]
                    (for [path paths]
                        (let [variants (get-equivalent-answer-variants q c) ;; all valid answers (survey.Component) in the answer set
                              responses-this-path (get path-map path) ;; list of survey responses
                              ans-this-path (map (fn [^Component cc]
                                                     (filter #(survey-response-contains-answer % cc) responses-this-path))
                                                 variants)
                              p (/ (count ans-this-path) total-responses)
                          ]
                            (* p (util/log2 p))))))
            (flatten)
            (reduce +)
            (* -1.0))))

(defn get-questions
  "Returns all questions in a block list (typically the topLevelBlocks of a Survey)."
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
  "Returns the maximum possible entropy for a single Question."
  [^Question q]
  (->> (count (.options q))
       (#(if (= 0 %) 0 (/ 1 %)))
       (#(if (= 0 %) 0 (util/log2 %)))
       (math/abs)
     )
  )

(defn max-entropy-qlist
  "Returns the total entropy for a list of Questions."
  [qlist]
  (reduce + (map max-entropy-one-question qlist))
  )

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
  "Returns a path length, according to the particular PathMetric."
  [^Survey survey ^PathMetric metric]
  ;; return a path that satisfies the metric
  (let [paths (get-paths survey)]
    (cond (= metric PathMetric/MAX) (first (filter #(= (count %) (apply max (map count paths))) paths))
          (= metric PathMetric/MIN) (first (filter #(= (count %) (apply min (map count paths))) paths))
          :else (throw (Exception. (str "Unknown path metric " metric)))
          )
    )
  )

(defn -minimumPathLength
  [^IQCMetrics _ ^Survey s]
  (let [minPath (pathLength s PathMetric/MIN)]
    (count (get-questions minPath))
    )
  )

(defn -maximumPathLength
  [^IQCMetrics _ ^Survey s]
  (let [maxPath (pathLength s PathMetric/MAX)]
    (count (get-questions maxPath))
    )
  )

(defn -averagePathLength
  [^IQCMetrics _ ^Survey s]
  (let [n 5000]
    (/ (reduce + (map #(count (get-true-responses (.response %))) (get-random-survey-responses s n))) n)
    )
  )

(defn -getBasePay
  [^IQCMetrics _ ^Survey s]
  (* (-minimumPathLength s) timePerQuestionInSeconds (/ FEDMINWAGE 3600))
  )

(defn get-prob
  "Returns the probability of this IQuestionResponse, given the input empirical probabilities."
  [^IQuestionResponse qr probabilities]
  (map (fn [^OptTuple c]
         (let [quid (.quid (.getQuestion ^IQuestionResponse qr))]
           (if (= quid AbstractParser/CUSTOM_ID)
               '(0.0)
               (get (get probabilities quid) (.getCid (.c c)))
             )
           )
         )
       (.getOpts qr)
    )
  )

(defn get-ll-for-response
  "Returns the eval metric for a particular response, based on the input probabilities"
  [^ISurveyResponse sr probabilities]
  (try
    (->> (get-true-responses sr)
         (map #(get-prob ^IQuestionResponse % probabilities))
         (flatten)
         (remove nil?)
         (reduce +)
         )
    (catch Exception e (do (.printStackTrace e)
                           (println sr)
                           (System/exit 1)))
    )
  )

(defn calculate-log-likelihoods
  "Returns the full set of eval metrics."
  [responses probabilities]
  (when responses
    (map #(get-ll-for-response % probabilities) responses)
    )
  )


(defn -calculateBonus
  [^IQCMetrics _ ^ISurveyResponse sr ^Record qc]
    (if (.contains (.validResponses qc) sr)
        (- (* 0.02 (count (get-true-responses sr))) 0.10)
        0.0)
    )

(defn truncate-responses
  "Returns the IQuestionResponses that only overlap with the Questions in input ISurveyResponse."
  [responses ^ISurveyResponse sr]
  (remove nil?
    (for [^ISurveyResponse r responses]
      (let [answered-questions (->> (.getResponses sr)
                                 (map #(.getQuestion %))
                                 (map #(.quid %))
                                 (remove #(= % AbstractParser/CUSTOM_ID))
                                 (set))
            targets-responses (->> (.getResponses r)
                                   (map #(.getQuestion %))
                                   (map get-variants)
                                   (map seq)
                                   (flatten)
                                   (remove nil?)
                                   (map #(.quid %))
                                   (set))]
        (when (clojure.set/subset? answered-questions targets-responses)
          (let [retval (SurveyResponse. (.workerId r))
                questionResponses (remove #(nil? (get-variant (.getQuestion %) sr)) (.getResponses r))]
            (.setResponses retval questionResponses)
            retval
            )
          )
        )
      )
    )
  )

(defn -entropyClassification
  [^IQCMetrics _ ^Survey survey ^ISurveyResponse s responses]
  (if (< 2 (count responses))
    (let [probabilities (make-probabilities survey (make-frequencies responses))
          lls (calculate-log-likelihoods (truncate-responses responses s) probabilities)]
      (if (> (count (set lls)) 5)
          (let [thisLL (get-ll-for-response s probabilities)
                bs-sample (incanter.stats/bootstrap lls incanter.stats/mean :size 2000)
                p-val (first (incanter.stats/quantile bs-sample :probs [@alpha]))
                ]
          ;        (println "bias: " (- (incanter.stats/mean bs-sample) (incanter.stats/mean lls)))
          ;        (println "CI: " (incanter.stats/quantile bs-sample :probs [(- 1 @alpha) @alpha]))
          ;        (println "hand-calculated CI:" (let [samp (sort bs-sample)
          ;                                             lower (math/floor (* @alpha (count samp)))
          ;                                             upper (math/floor (* (- 1 @alpha) (count samp)))]
          ;                                         [(nth samp lower) (nth samp upper)]))
          ;        (println "pval: " p-val "thisLL: " (float thisLL))

            (.setScore s thisLL)
            (.setThreshold s p-val)
            (< thisLL p-val)
            )
          false
        )
      )
    false
    )
  )

(defn -logLikelihoodClassification
  [^IQCMetrics _ ^Survey survey ^ISurveyResponse s responses]
    false
  )

(defn -lpoClassification
  [^IQCMetrics _ ^Survey survey ^ISurveyResponse s responses]
  false
  )

(defn -getBotThresholdForSurvey
    [^IQCMetrics _ ^Survey s]
    (@cutoffs (.sourceName s))
    )

(defn -getDag
  [^IQCMetrics _ ^List block-list]
  (get-dag block-list)
  )
