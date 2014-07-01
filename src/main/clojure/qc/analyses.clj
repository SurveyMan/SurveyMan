;; dynamic analyses for SurveyMan
(ns qc.analyses
  (:gen-class
    :name qc.Analyses
    :methods [#^{:static true} [getCorrelations [java.util.List survey.Survey] java.util.List]])
  (:import (interstitial IQuestionResponse ISurveyResponse OptTuple Record))
  (:import (qc IQCMetrics Metrics)
           (java.util List)
           (org.apache.log4j Logger)
           (org.apache.commons.math3.stat.inference MannWhitneyUTest)
           (survey Block$BranchParadigm Block Survey Question Component)
           (input.csv CSVLexer)
           (util Printer))
  (:require [incanter core stats]
            [qc.metrics]
            [clojure.math.numeric-tower :as math]
            [clojure.test :as test])
  )

(def LOGGER (Logger/getLogger (str (ns-name *ns*))))
(def qcMetrics ^IQCMetrics (qc.Metrics.))
(def repeat-workers (atom nil))

(defrecord Response [^String srid
                     ^List opts
                     ^Integer indexSeen])

(defn make-ans-map
  "Takes each question and returns a map from questions to a list of question responses.
   The survey response id is attached as metadata."
  [surveyResponses]
  (let [answers (for [^ISurveyResponse sr surveyResponses]
                  (apply merge
                    (for [^IQuestionResponse qr (.getResponses sr)]
                      {(.getQuestion qr) (list (Response. (.srid sr)
                                                          (map (fn [opt] (.c ^OptTuple opt)) (.getOpts qr))
                                                             (.getIndexSeen qr)))
                       }
                    )
                  )
                )]
      (reduce #(merge-with concat %1 %2) {} answers)))

(defn convertToOrdered
  [q]
  "Returns a map of cids (String) to integers for use in ordered data."
  (into {} (zipmap (map #(.getCid %) (sort-by #(.getSourceRow %) (vals (.options q))))
                   (iterate inc 1)))
                   ;;(range 1 (inc (count (.options q))))))
)

(defn getOrdered
  "Returns an integer corresponding to the ranked order of the option."
  [q opt]
  (let [m (convertToOrdered q)]
    (assert (contains? m (.getCid opt))
            (clojure.string/join "\n" (list (.getCid opt) m
                                            (into [] (map #(.getCid %) (vals (.options q)))))))
    (get m (.getCid opt))
    )
  )

(defn get-questions-with-variants
  [^Survey survey]
  (loop [blocks (vals (.blocks survey))
         retval '()]
    (if (empty? blocks)
      retval
      (let [subblocks (.subBlocks ^Block (first blocks))]
        (recur (if (empty? subblocks)
                   (rest blocks)
                   (flatten (concat subblocks (rest blocks))))
               (if (= (.branchParadigm ^Block (first blocks)) Block$BranchParadigm/ALL)
                   (cons (.questions (first blocks)) retval)
                   retval
                   )
               )
        )
      )
    )
  )

(defn find-first
  ;; there used to be a find-first in seq-utils, but I don't know where this went in newer versions of clojure
  [pred coll]
  (cond (empty? coll) nil
        (pred (first coll)) (first coll)
        :else (recur pred (rest coll))
      )
  )

(defn align-by-srid
  [l1 l2]
  (doall
    (loop [pointer l1
           l1sorted '()
           l2sorted '()]
      (if (empty? pointer)
          [l1sorted l2sorted]
          (let [matched (find-first #(= (:srid %) (:srid (first pointer))) l2)]
            (if (nil? matched)
                (recur (rest pointer) l1sorted l2sorted)
                (recur (rest pointer) (cons (first pointer) l1sorted) (cons matched l2sorted))
              )
            )
        )
      )
    )
  )

(defn mann-whitney
  [x y]
  ;(println (seq x) (seq y))
  (when (and (seq x) (seq y))
    (try
      (let [ mw (MannWhitneyUTest.) ]
        ;(println (.mannWhitneyU mw x y))
        ;(println (.mannWhitneyUTest mw x y))
        { :stat 'mann-whitney
          :val { :U (.mannWhitneyU mw x y)
                 :p-value (.mannWhitneyUTest mw x y)
                 }
          }
        )
      (catch Exception e (do (.warn LOGGER (str "mann-whitney" (.getMessage e)))
                           (.println (.getMessage e))
                           )
        )
      )
    )
  )

(defn chi-squared
    [tab]
    (when (seq tab)
        (try
            { :stat 'chi-squared
              :val (incanter.stats/chisq-test :table tab)
            }
            (catch Exception e (.warn LOGGER (str "chi-squared" (.getMessage e))))
            )
        )
    )

(defn spearmans-rho
    [l1 l2]
    (when (and (seq l1) (seq l2))
        (try
            (incanter.stats/spearmans-rho l1 l2)
            (catch Exception e (.warn LOGGER (str "spearmans-rho" (.getMessage e))))
            )
        )
    )

(defn comparison-applies?
  [^Question q1 ^Question q2]
  (and (.exclusive q1)
    (.exclusive q2)
    (not (.freetext q1))
    (not (.freetext q2))
    (not (= (.quid q1) Survey/CUSTOM_ID))
    (not (= (.quid q2) Survey/CUSTOM_ID))
    )
  )

(defn use-rho?
  [^Question q1 ^Question q2]
  (and (.ordered q1) (.ordered q2))
  )

(defn opt-list-by-index
  [^Question q]
  (sort #(< (.getSourceRow ^Component %1) (.getSourceRow ^Component %2)) (vals (.options q)))
  )

(defn get-ids-that-answered-option
  [ansMap ^Question q1 ^Component opt1]
  (->> (ansMap q1)
    (filter #(= opt1 (first (:opts %))))
       (flatten)
       (map #(:srid %))
       (set))
  )


(defn calculate-rho
  [^Question q1 ans1 ^Question q2 ans2]
  (let [n (min (count ans1) (count ans2))]
    (spearmans-rho
      (map #(getOrdered q1 (first (:opts %))) (take n ans1))
      (map #(getOrdered q2 (first (:opts %))) (take n ans2)))))

(defn calculate-V
  [ansMap ^Question q1 ^Question q2]
  (let [tab (->> (for [opt1 (opt-list-by-index q1) opt2 (opt-list-by-index q2)]
                   ;; count the number of people who answer both opt1 and opt2
                   (let [answeredOpt1 (get-ids-that-answered-option ansMap q1 opt1)
                         answeredOpt2 (get-ids-that-answered-option ansMap q2 opt2)]
                     (count (clojure.set/intersection answeredOpt1 answeredOpt2))))
              (partition (count (.options q1)))
              (incanter.core/matrix))
        {{X-sq :X-sq} :val :as data} (chi-squared tab)
        N (reduce + (flatten tab))
        k (apply min (incanter.core/dim tab))]
    (when (and X-sq (> N 0) (> k 1))
      (math/sqrt (/ X-sq (* N (dec k))))
      0)))

(defn correlation
  [surveyResponses ^Survey survey]
  (let [ansMap (make-ans-map surveyResponses)]
    (doall
      (for [^Question q1 (.questions survey) ^Question q2 (.questions survey)]
        ;(when-not (and (= (.block q1) (.block q2)) (= (.branchParadigm ^Block (.block q1)) Block$BranchParadigm/ALL))
        (let [[ans1 ans2] (align-by-srid (ansMap q1) (ansMap q2))]
          ;(println q1 q2 (correlation-applies? q1 q2))
          { :q1&ct [q1 (count ans1)]
            :q2&ct [q2 (count ans2)]
            :corr (cond (not (comparison-applies? q1 q2)) {:coeff 'NONE :val 0}
                        (use-rho? q1 q2) {:coeff 'rho :val (calculate-rho q1 ans1 q2 ans2)}
                        :else {:coeff 'V :val (calculate-V ansMap q1 q2)})
            }
          )
        )
      )
    )
  ;(System/exit 1)
  )

(defn getCountsForContingencyTab
  [q lst]
  (map (fn [^Component opt]
           (count (filter (fn [^Response r]
                              (= (.getCid (first (:opts r)))
                                 (.getCid opt)))
           lst)))
     (.getOptListByIndex q)
     )
  )


(defn orderBias
    [surveyResponses ^Survey survey]
  (let [ansMap (make-ans-map surveyResponses)]
    (remove nil?
      (for [^Question q1 (.questions survey) ^Question q2 (.questions survey)]
        (when (comparison-applies? q1 q2)
          (let [[q1ans q2ans] (align-by-srid (ansMap q1) (ansMap q2))
                tmp (map vector q1ans q2ans)
                q1answersq1first (map first (filter (fn [pair] (< (:indexSeen (pair 0)) (:indexSeen (pair 1)))) tmp))
                q1answersq2first (map first (filter (fn [pair] (> (:indexSeen (pair 0)) (:indexSeen (pair 1)))) tmp))
                numq1first (count q1answersq1first)
                numq2first (count q1answersq2first)]
            (when (and (> numq1first 0) (> numq2first 0))
              { :q1 q1
                :q2 q2
                :numq1First numq1first
                :numq2First numq2first
                :order (if (.ordered q1)
                          (let [x (into-array Double/TYPE (map #(double (getOrdered q1 (first (:opts %)))) q1answersq1first))
                                y (into-array Double/TYPE (map #(double (getOrdered q1 (first (:opts %)))) q1answersq2first))
                                retval (mann-whitney x y)]
                            ;(println retval)
                            retval
                            )
                         (let [retval (chi-squared (incanter.core/matrix (list (getCountsForContingencyTab q1 q1answersq1first)
                                                                               (getCountsForContingencyTab q1 q1answersq2first))))]
                           ;(println retval)
                           retval
                           )
                         )
                }
              )
            )
          )
        )
      )
    )
  )

(defn wordingBias
  [surveyResponses ^Survey survey]
  (let [ansMap (make-ans-map surveyResponses)
        variantList (get-questions-with-variants survey)]
    (for [variants (seq variantList)]
      (for [^Question q1 variants ^Question q2 (rest variants)]
          (let [q1ans (ansMap q1)
                q2ans (ansMap q2)]
            { :q1&ct [q1 (count q1ans)]
              :q2&ct [q2 (count q2ans)]
              :bias (if (.ordered q1)
                      (let [x (into-array Double/TYPE (map #(getOrdered q1 (first (:opts %))) q1ans))
                            y (into-array Double/TYPE (map #(getOrdered q2 (first (:opts %))) q2ans))
                            retval (mann-whitney x y)]
                        ;(println retval)
                        retval
                        )
                      (let [retval (chi-squared (incanter.core/matrix (list (getCountsForContingencyTab q1 q1ans)
                                                                (getCountsForContingencyTab q2 q2ans))))]
                        ;(println 'bar)
                        ;(println retval)
                        retval)
                      )
              }
            )
          )
        )
    )
  )

(defn valid-response?
  [^Survey survey responses ^ISurveyResponse sr classifier]
  (case classifier
    :entropy (.entropyClassification qcMetrics survey sr responses)
    :entropy-norm (.normalizedEntropyClassification qcMetrics survey sr responses)
    :all true
    :default (throw (Exception. (str "Unknown classifier : " classifier)))
    )
)

(defn remove-repeaters
    [surveyResponses]
    (let [workerids (map #(.workerId %) surveyResponses)
          repeaters (map first (filter #(> (second %) 1) (frequencies workerids)))]
        (loop [repeater (flatten repeaters)]
            (swap! repeat-workers conj repeater))
        (remove #(contains? (set repeaters) (.workerId %)) surveyResponses)
        )
    )

(defn classifyBots
    [surveyResponses ^Record qc classifier]
    ;; basic bot classification, using entropy
    ;; need to port more infrastructure over from python/julia; for now let's assume everyone's valid
    (let [sans-repeaters (remove-repeaters surveyResponses)
          retval (doall (merge-with concat
                          (pmap (fn [^ISurveyResponse sr]
                                        (if (valid-response? (.survey qc) surveyResponses sr classifier)
                                            (do (.add (.validResponses qc) sr)
                                                {:not (list sr)})
                                            (do (.add (.botResponses qc) sr)
                                                {:bot (list sr)})
                                            )
                                        )
                            sans-repeaters)))]
        (assert (= (+ (count (.botResponses qc)) (count (.validResponses qc)))
                  (count (remove-repeaters surveyResponses)))
            (format "num responses: %d num bots: %d num nots: %d\n"
              (count sans-repeaters)
              (count (.botResponses qc))
              (count (.validResponses qc)))
            )
        retval
        )
    )

(defn -getCorrelations
  [surveyResponses survey]
  (correlation surveyResponses survey)
  )

(defn get-last-q
    [^ISurveyResponse sr]
    (->> (.getResponses sr)
         (sort (fn [^IQuestionResponse qr1
                    ^IQuestionResponse qr2]
                   (> (.getIndexSeen qr1) (.getIndexSeen qr2))
                   )
               )
         (first)
         (.getQuestion)))

(defn top-half-breakoff-questions
    [srlist]
    (loop [freq-seq (sort #(> (%1 1) (%2 1)) (seq (frequencies (map get-last-q srlist))))
           total (reduce + (map second freq-seq))
           cumulative-total 0
           ret-val (transient [])
           ]
        (if (or (> cumulative-total (* 0.5 total)) (empty? freq-seq))
            (persistent! ret-val)
            (recur (rest freq-seq)
                   total
                   (+ cumulative-total (second (first freq-seq)))
                   (conj! ret-val (first freq-seq))
                   )
            )
        )
    )

(defn breakoffQuestions
    [valid-responses bot-responses]
    (let [breakoff-qs-valid-responses (top-half-breakoff-questions valid-responses)
          breakoff-qs-bot-responses (top-half-breakoff-questions bot-responses)
          all-breakoff-qs (top-half-breakoff-questions (concat valid-responses bot-responses))
          ]
        { :valid-responses breakoff-qs-valid-responses
          :bot-responses breakoff-qs-bot-responses
          :all all-breakoff-qs
          }
        )
    )

(defn top-half-breakoff-pos
    [srlist]
    (loop [freq-seq (sort #(> (%1 1) (%2 1))
                          (seq (frequencies (map #(count (qc.metrics/get-true-responses %)) srlist))))
           total (reduce + (map second freq-seq))
           cumulative-total 0
           ret-val (transient [])]
        (if (or (> cumulative-total (* 0.5 total)) (empty? freq-seq))
            (persistent! ret-val)
            (recur (rest freq-seq)
                   total
                   (+ cumulative-total (second (first freq-seq)))
                   (conj! ret-val (first freq-seq))
                   )
            )
        )
    )

(defn breakoffPositions
    [valid-responses bot-responses]
    (let [breakoff-pos-valid-responses (top-half-breakoff-pos valid-responses)
          breakoff-pos-bot-responses (top-half-breakoff-pos bot-responses)
          all-breakoff-pos (top-half-breakoff-pos (concat valid-responses bot-responses))]
        { :valid-responses breakoff-pos-valid-responses
          :bot-responses breakoff-pos-bot-responses
          :all all-breakoff-pos
          }
        )
    )

(defn -main
    [& args]
    ()
    )