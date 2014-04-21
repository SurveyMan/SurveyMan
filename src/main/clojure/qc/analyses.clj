;; dynamic analyses for SurveyMan
(ns qc.analyses
    (:gen-class
        :name qc.analyses
        :methods [#^{:static true} [getCorrelations [java.util.List survey.Survey] java.util.List]])
    (:import (java.util List)
             (org.apache.log4j Logger)
             (org.apache.commons.math3.stat.inference MannWhitneyUTest)
             (survey Block$BranchParadigm SurveyResponse$OptTuple Block)
             (csv CSVLexer)
             (qc QCMetrics QC))
    (:import (survey Survey Question Component SurveyResponse SurveyResponse$QuestionResponse))
    (:require [incanter core stats]
              [clojure.math.numeric-tower :as math]
              [clojure.test :as test])
)

(def LOGGER (Logger/getLogger (str (ns-name *ns*))))

(defrecord Response [^String srid
                     ^List opts
                     ^Integer indexSeen])

(defn make-ans-map
    "Takes each question and returns a map from questions to a list of question responses.
     The survey response id is attached as metadata."
    [surveyResponses]
    (let [answers (for [^SurveyResponse sr surveyResponses]
                      (apply merge
                          (for [^SurveyResponse$QuestionResponse qr (.responses sr)]
                                           {(.q qr) (list (Response. (.srid sr)
                                                                     (map (fn [opt] (.c ^SurveyResponse$OptTuple opt)) (.opts qr))
                                                                     (.indexSeen qr)))}
                          )
                      )
                  )]
        (reduce #(merge-with concat %1 %2) {} answers)))

(defn convertToOrdered
    [q]
    "Returns a map of cids (String) to integers for use in ordered data."
    (into {} (zipmap (map #(.getCid %) (sort-by #(.getSourceRow %) (vals (.options q))))
                     (range 1 (inc (count (.options q))))))
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

(defn mann-whitney
    [x y]
    (try
        (let [ mw (MannWhitneyUTest.) ]
            { :stat 'mann-whitney
              :val { :U (.mannWhitneyU mw x y)
                     :p-value (.mannWhitneyUTest mw x y)
                     }
              }
            )
        (catch Exception e (.warn LOGGER (.getMessage e)))
        )
    )

(defn chi-squared
    [tab]
    (try
        { :stat 'chi-squared
          :val (incanter.stats/chisq-test :table tab)
        }
        (catch Exception e (.warn LOGGER (.getMessage e)))
        )
    )

(defn spearmans-rho
    [l1 l2]
    (try
        (incanter.stats/spearmans-rho l1 l2)
        (catch Exception e (.warn LOGGER (.getMessage e)))
        )
    )

(defn correlation
    [surveyResponses ^Survey survey]
    (let [ansMap (make-ans-map surveyResponses)]
        (for [^Question q1 (.questions survey) ^Question q2 (.questions survey)]
            (when-not (and (= (.block q1) (.block q2)) (= (.branchParadigm ^Block (.block q1)) Block$BranchParadigm/SAMPLE))
                (let [[ans1 ans2] (align-by-srid (ansMap q1) (ansMap q2))]
                    { :q1&ct [q1 (count ans1)]
                      :q2&ct [q2 (count ans2)]
                      :corr (if (and (.exclusive q1) (.exclusive q2) (not (.freetext q1)) (not (.freetext q2)))
                                (if (and (.ordered q1) (.ordered q2))
                                    (let [n (min (count ans1) (count ans2))]
                                        { :coeff 'rho
                                          :val (spearmans-rho (map #(getOrdered q1 (first (:opts %))) (take n ans1))
                                                              (map #(getOrdered q2 (first (:opts %))) (take n ans2)))
                                        })
                                    (let [tab (->> (for [opt1 (.getOptListByIndex q1) opt2 (.getOptListByIndex q2)]
                                                        ;; count the number of people who answer both opt1 and opt2
                                                        (let [answeredOpt1 (set (map #(:srid %) (flatten (filter #(= (:opts %) opt1) (ansMap q1)))))
                                                              answeredOpt2 (set (map #(:srid %) (flatten (filter #(= (:opts %) opt2) (ansMap q2)))))]
                                                            (count (clojure.set/intersection answeredOpt1 answeredOpt2))))
                                                    (partition (count (.getOptListByIndex q1)))
                                                    (incanter.core/matrix))

                                          {X-sq :val :as data} (chi-squared tab)
                                          N (reduce + (flatten tab))
                                          k (apply min (incanter.core/dim tab))]
                                        (when (> N 0) (> k 1)
                                            (merge data {:coeff 'V
                                                         :val (math/sqrt (/ X-sq (* N (dec k))))
                                                        })
                                            )
                                    )
                                )
                            )
                    }
                    )
                )
            )
        )
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
                (when (and (.exclusive q1) (not (.freetext q1)))
                    (let [[q1ans q2ans] (align-by-srid (ansMap q1) (ansMap q2))
                          tmp (map vector q1ans q2ans)
                          q1answersq1first (map first (filter (fn [pair] (< (:indexSeen (pair 0)) (:indexSeen (pair 1)))) tmp))
                          q1answersq2first (map first (filter (fn [pair] (> (:indexSeen (pair 0)) (:indexSeen (pair 1)))) tmp))
                         ]
                        { :q1 q1
                          :q2 q2
                          :numq1First (count q1answersq1first)
                          :numq2First (count q1answersq2first)
                          :order (if (.ordered q1)
                                     (let [x (into-array Double/TYPE (map #(double (getOrdered q1 (first (:opts %)))) q1answersq1first))
                                           y (into-array Double/TYPE (map #(double (getOrdered q1 (first (:opts %)))) q1answersq2first))
                                          ]
                                         (mann-whitney x y))
                                     (chi-squared (incanter.core/matrix (list (getCountsForContingencyTab q1 q1answersq1first)
                                                                              (getCountsForContingencyTab q1 q1answersq2first)))))
                        }
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
            (let [subblocks (.subBlocks ^Block (first blocks))]
                (recur (if (empty? subblocks)
                           (rest blocks)
                           (flatten (concat subblocks (rest blocks))))
                       (if (= (.branchParadigm ^Block (first blocks)) Block$BranchParadigm/SAMPLE)
                           (cons (.questions (first blocks)) retval)
                           retval
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
        (for [variants variantList]
            (for [^Question q1 variants ^Question q2 variants]
                (let [q1ans (ansMap q1)
                      q2ans (ansMap q2)]
                    { :q1&ct [q1 (count q1ans)]
                      :q2&ct [q2 (count q2ans)]
                      :order (if (.ordered q1)
                                 (let [x (into-array Double/TYPE (map #(double (getOrdered q1 (first (:opts %)))) q1ans))
                                       y (into-array Double/TYPE (map #(double (getOrdered q2 (first (:opts %)))) q2ans))]
                                     (mann-whitney x y)
                                     )
                                 (chi-squared (incanter.core/matrix (list (getCountsForContingencyTab q1 q1ans)
                                                                          (getCountsForContingencyTab q2 q2ans)))))
                      }
                    )
                )
            )
        )
    )

(defn classifyBots
    [surveyResponses ^Survey survey ^QC qc]
    ;; basic bot classification, using entropy
    ;; need to port more infrastructure over from python/julia; for now let's assume everyone's valid
    (let [surveyEntropy (QCMetrics/surveyEntropy survey surveyResponses)
          ]
        (merge-with concat (for [sr surveyResponses]
                               (do (.add (.validResponses qc) sr)
                                   {:not '(sr)})
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


