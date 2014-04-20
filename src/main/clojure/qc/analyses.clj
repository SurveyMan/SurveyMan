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

(defn make-ans-map
    "Takes each question and returns a map from questions to a list of question responses.
     The survey response id is attached as metadata."
    [surveyResponses]
    (let [answers (for [^SurveyResponse sr surveyResponses]
                      (apply merge
                          (for [^SurveyResponse$QuestionResponse qr (.responses sr)]
                                           {(.q qr) (list [(.srid sr)
                                                        (map (fn [opt] (.c ^SurveyResponse$OptTuple opt))
                                                              (.opts qr))
                                                        (.indexSeen qr)])})))]
        (reduce #(merge-with concat %1 %2) answers)))

(defn- convertToOrdered
    [q]
    (into {} (zipmap (map (fn [opt] (.getCid opt)) (.getOptListByIndex q))
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

(defn align-by-srid
    [l1 l2]
    (loop [pointer l1
           l1sorted '()
           l2sorted '()]
        (if (empty? pointer)
            [l1sorted l2sorted]
            (let [matched (find-first #(= (% srid) ((first pointer) srid)) l2)]
                (if (nil? matched)
                    (recur (rest pointer) l1sorted l2sorted)
                    (recur (rest pointer) (cons (first pointer) l1sorted) (cons matched l2sorted))
                    )
                )
            )
        )
    )

(defn correlation
    [surveyResponses ^Survey survey]
    (let [ansMap (make-ans-map surveyResponses)]
        (assert (every? identity (flatten (for [k (keys ansMap)]
                                              (map (fn [optlist]
                                                       (map #(contains? (set (keys (.options k))) (% opts))
                                                            optlist)
                                                       )
                                                   (map opts (ansMap k))
                                                   )
                                              )
                                          )
                        )
                )
        (for [^Question q1 (.questions survey) ^Question q2 (.questions survey)]
            (let [[ans1 ans2] (align-by-srid (ansMap q1) (ansMap q2))]
                ;; make sure order is retained
                (assert (every? identity (map #(= (%1 srid) (%2 srid)) ans1 ans2)))
                { :q1&ct [q1 (count ans1)]
                  :q2&ct [q2 (count ans2)]
                  :corr (if (and (.exclusive q1) (.exclusive q2))
                            (try
                                (if (and (.ordered q1) (.ordered q2))
                                    (let [n (min (count ans1) (count ans2))]
                                        { :coeff 'rho
                                          :val (incanter.stats/spearmans-rho (map #(getOrdered q1 (first %)) (take n ans1))
                                                                             (map #(getOrdered q2 (first %)) (take n ans2)))
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
        (assert (every? identity (flatten (for [k (keys ansMap)]
                                              (map (fn [optlist]
                                                       (map #(contains? (set (keys (.options k))) (.getCid %))
                                                            optlist)
                                                       )
                                                   (map #(% opts) (ansMap k))
                                                   )
                                              )
                                          )
                        )
                )
        (remove nil?
            (for [^Question q1 (.questions survey) ^Question q2 (.questions survey)]
                (when (not= q1 q2)
                    (let [[q1ans q2ans] (align-by-srid (ansMap q1) (ansMap q2))
                          tmp (map vector q1ans q2ans)
                          q1answersq1first (filter (fn [pair] (< ((pair 0) indexSeen) ((pair 1) indexSeen))) tmp)
                          q1answersq2first (filter (fn [pair] (> ((pair 0) indexSeen) ((pair 1) indexSeen))) tmp)
                         ]
                        (println q1answersq1first)
                          (let [  x (into-array Double/TYPE (map #(double (getOrdered q1 (first (% opts)))) (map first q1answersq1first)))
                                  y (into-array Double/TYPE (map #(double (getOrdered q1 (first (% opts)))) (map first q1answersq2first)))
                                  ]
                                      { :q1 q1
                                        :q2 q2
                                        :numq1First (count q1answersq1first)
                                        :numq2First (count q1answersq2first)
                                        :order (difference-of-distr q1 q2 x y ansMap)
                                      }
                          )
                    )
                )
            )
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


