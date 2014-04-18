;; dynamic analyses for SurveyMan
(ns qc.analyses
    (:gen-class
        :name qc.analyses
        :methods [#^{:static true} [getCorrelations [java.util.List survey.Survey] java.util.List]])
    (:import (java.util List)
             (org.apache.log4j Logger)
             (org.apache.commons.math3.stat.inference MannWhitneyUTest)
             (survey Block$BranchParadigm SurveyResponse$OptTuple))
    (:import (survey Survey Question Component SurveyResponse SurveyResponse$QuestionResponse))
    (:require [incanter core stats]
              [clojure.math.numeric-tower :as math])
)

(def LOGGER (Logger/getLogger (str (ns-name *ns*))))
(def srid 0)
(def opts 1)
(def indexSeen 2)

(defn map-to-seq
    [m]
    (map #(vector % (m %)) (keys m))
    )

(defn- make-ans-map
    "Takes each question and returns a map from questions to a list of question responses.
     The survey response id is attached as metadata."
    [surveyResponses]
    (let [answers (for [^SurveyResponse sr surveyResponses]
                      (apply merge (for [^SurveyResponse$QuestionResponse qr (.responses sr)]
                                       {(.q qr) [(.srid sr) (.c ^SurveyResponse$OptTuple (first (.opts qr))) (.indexSeen qr)]})))]
        (assert (every? identity
                        (map (fn [tupe]
                                 (let [[q [_ cid _]] tupe]
                                     (println q cid)
                                     (map #(contains? (set (map (fn [opt] (.getCid opt)))) cid)
                                          (.options q))))
                             (map map-to-seq answers))))
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

(defn- convertToOrdered
    [q]
    (into {} (zipmap (map (fn [comp] (.getCid comp)) (.getOptListByIndex q))
                     (range 1 (count (.getOptListByIndex q)))))
    )

(defn- getOrdered
    [q opt]
    (let [m (convertToOrdered q)
          retval (get m (.getCid opt))]
        (println (str "retval: " retval (.getCid opt) m))
        (assert (contains? m (.getCid opt)) 'foo)
        retval
    )
)

(defn- makeContingencyTable
    [^Question q1 ^Question q2 ansMap]
        (->> (for [opt1 (.getOptListByIndex q1) opt2 (.getOptListByIndex q2)]
             ;; count the number of people who answer both opt1 and opt2
             (let [answeredOpt1 (set (map #(% srid) (flatten (filter #(= (% opts) opt1) (ansMap q1)))))
                   answeredOpt2 (set (map #(% srid) (flatten (filter #(= (% opts) opt2) (ansMap q2)))))]
                 (count (clojure.set/intersection answeredOpt1 answeredOpt2))))
         (partition (count (.getOptListByIndex q1)))
         (incanter.core/matrix))
    )

(defn find-first
    [pred coll]
    (cond (empty? coll) nil
          (pred (first coll)) (first coll)
          :else (recur pred (rest coll))
        )
    )

(defn align
    [cmp l1 l2]
    (loop [foo l1
           bar '()
           baz '()]
        (if (empty? foo)
            [bar baz]
            (let [matched (find-first #(= (cmp %) (cmp (first foo))) l2)]
                (if (nil? matched)
                    (recur (rest foo) bar baz)
                    (recur (rest foo) (cons (first foo) bar) (cons matched baz))
                    )
                )
            )
        )
    )

(defn correlation
    [surveyResponses ^Survey survey]
    (let [ansMap (make-ans-map surveyResponses)]
        (for [^Question q1 (.questions survey) ^Question q2 (.questions survey)]
            (let [[ans1 ans2] (align #(% srid) (ansMap q1) (ansMap q2))]
                ;; make sure order is retained
                (assert (every? identity (map #(= (%1 srid) (%2 srid)) ans1 ans2)))
                { :q1&ct [q1 (count ans1)]
                  :q2&ct [q2 (count ans2)]
                  :corr (if (and (.exclusive q1) (.exclusive q2))
                            (try
                                (if (and (.ordered q1) (.ordered q2))
                                    (let [n (min (count ans1) (count ans2))]
                                        { :coeff 'rho
                                          :val (incanter.stats/spearmans-rho (map #(getOrdered q1 %) (take n ans1))
                                                                             (map #(getOrdered q2 %) (take n ans2)))
                                        }
                                    )
                                    (let [tab (makeContingencyTable q1 q2 ansMap)
                                          {X-sq :X-sq :as data} (incanter.stats/chisq-test :table tab)
                                          N (reduce + (flatten tab))
                                          k (min (tab :cols) (tab :rows))]
                                            (merge data {:coeff 'V
                                                         :val (math/sqrt (/ X-sq (* N (dec k))))
                                                        }
                                            )
                                    )
                                )
                            (catch Exception e
                                (.warn LOGGER (.getMessage e)))))
                      }
                )
        )
    )
)

(defn difference-of-distr
    [^Question q1 ^Question q2 x y ansMap]
    (if (.exclusive q1)
        (if (.ordered q1)
            (let [ mw (MannWhitneyUTest.) ]
                { :stat 'mann-whitney
                  :val { :U (.mannWhitneyU mw x y)
                         :p-value (.mannWhitneyUTest mw x y)
                         }
                  }
                )
            (let [tab (makeContingencyTable q1 q2 ansMap)]
                { :stat 'chi-squared
                  :val (incanter.stats/chisq-test :table tab)
                  }
                )
            )
        )
    )

(defn orderBias
    [surveyResponses ^Survey survey]
    (let [ansMap (make-ans-map surveyResponses)]
        (for [^Question q1 (.questions survey) ^Question q2 (.questions survey)]
            (do (println q1 q2)
            (let [[q1ans q2ans] (align #(% srid) (ansMap q1) (ansMap q2))
                  { q1answersq1first true
                    q1answersq2first false} (group-by (fn [pair]
                                                          (< ((pair 0) indexSeen) ((pair 1) indexSeen)))
                                                      (map vector q1ans q2ans))
                  ]
                  (assert (every? identity (map #(contains? (set (map (fn [opt] (.getCid opt)) (.getOptListByIndex q1))) (.getCid (% opts))) q1ans)))
                  (assert (every? identity (map #(contains? (set (map (fn [opt] (.getCid opt)) (.getOptListByIndex q2))) (.getCid (% opts))) q2ans)))
                  (assert (every? identity (map #(contains? (set (map (fn [opt] (.getCid opt)) (.getOptListByIndex q1))) (.getCid (% opts))) (map first q1answersq1first))))
                  (assert (every? identity (map #(contains? (set (map (fn [opt] (.getCid opt)) (.getOptListByIndex q1))) (.getCid (% opts))) (map first q1answersq2first))))
                  (let [  x (into-array Double/TYPE (map #(double (getOrdered q1 (% opts))) (map first q1answersq1first)))
                          y (into-array Double/TYPE (map #(double (getOrdered q1 (% opts))) (map first q1answersq2first)))
                          ]
                              { :q1 q1
                                :q2 q2
                                :numq1First (count q1answersq1first)
                                :numq2First (count q1answersq2first)
                                :order (difference-of-distr q1 q2 x y ansMap)
                              })
            ))
        )
    )
)

(defn get-questions-with-variants
    [^Survey survey]
    (loop [blocks (vals (.blocks survey))
           retval '()]
        (if (empty? blocks)
            retval
            (recur (rest blocks)
                   (if (= (.branchParadigm (first blocks)) Block$BranchParadigm/SAMPLE)
                       (cons (.questions (first blocks)) retval)
                       retval)
                   )
            )
    )
)

(defn wordingBias
    [surveyResponses ^Survey survey]
    (let [ansMap (make-ans-map surveyResponses)
          variantList (get-questions-with-variants survey)]
        (for [variants variantList]
            (for [^Question q1 variants ^Question q2 variants]
                (let [q1ans (ansMap q1)
                      q2ans (ansMap q2)
                      x (into-array Double/TYPE (map #(double (getOrdered q1 (% opts))) q1ans))
                      y (into-array Double/TYPE (map #(double (getOrdered q2 (% opts))) q2ans))]
                    { :q1&ct [q1 (count q1ans)]
                      :q2&ct [q2 (count q2ans)]
                      :order (difference-of-distr q1 q2 x y ansMap)
                      }
                    )
                )
            )
        )
    )

(defn -getCorrelations
    [surveyResponses survey]
    (correlation surveyResponses survey)
)

(defn -main
    [& args]
    ()
    )


