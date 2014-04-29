(ns qc.report
    (:gen-class
        :name qc.report
    )
    (:import (qc QC IQCMetrics Metrics)
             (survey Question Survey)
             (input.csv CSVParser CSVLexer)
             (system Library SurveyResponse)
             (system.generators JS)
             (input.json JSONParser)
             (java.io FileReader))
    (:require [qc.analyses :exclude '[-main]])
    )

(def validResponses (atom nil))
(def botResponses (atom nil))
(def breakoffQuestions (atom nil))
(def orderBiases (atom nil))
(def variants (atom nil))
(def staticMaxEntropy (atom 0.0))
(def avgPathLength (atom 0.0))
(def maxPathLength (atom 0))
(def minPathLength (atom 0))
(def correlations (atom nil))
(def correlationThreshhold (atom 0.5))
(def basePrice (atom 0.10))
(def strategy (atom :average-length))
(def ^IQCMetrics qcMetrics (qc.Metrics.))

(defn costPerQuestion
    []
    (* Library/timePerQuestionInSeconds (/ Library/FEDMINWAGE 60 60))
    )

(defn calculateBasePrice []
    (condp = @strategy
        :average-length (* @avgPathLength (costPerQuestion))
        :max-length (* @maxPathLength (costPerQuestion))
        :min-length (* @maxPathLength (costPerQuestion))
        (throw (Exception. (str "Unknown strategy" strategy)))
    )
)

(defn calculateBonuses [^Survey survey]

)

(defn expectedCorrelation
    [^Survey survey ^Question q1 ^Question q2]
    (some?
        (map #(and (contains? q1 (set %)) (contains? q2 (set %)))
              (vals (.correlationMap survey)))
    )
)

(defn dynamicAnalyses
    [^QC qc]
    (qc.analyses/classifyBots)
    (reset! validResponses (.validResponses qc))
    (reset! botResponses (.botResponses qc))
    (reset! correlations (qc.analyses/correlation @validResponses (.survey qc)))
    (reset! orderBiases (qc.analyses/orderBias @validResponses (.survey qc)))
    (reset! variants (qc.analyses/wordingBias @validResponses (.survey qc)))
)

(defmulti staticAnalyses #(type %))

(defmethod staticAnalyses Survey [survey]
           (reset! staticMaxEntropy (.getMaxPossibleEntropy qcMetrics survey))
           (reset! avgPathLength (.averagePathLength qcMetrics survey))
           (reset! maxPathLength (.maximumPathLength qcMetrics survey))
           (reset! minPathLength (.minimumPathLength qcMetrics survey))
           (reset! basePrice (calculateBasePrice)))

(defmethod staticAnalyses QC [qc]
           (staticAnalyses (.survey qc)))

(defn printStaticAnalyses
    []
    (printf "Average path length: %f\n" @avgPathLength)
    (printf "Max possible bits to represent this survey: %f\n" @staticMaxEntropy)
    (printf "Calculated base price using strategy %s : %f\n" @strategy @basePrice)
)

(defn printDynamicAnalyses
    [^Survey survey]
    (printf "Total number of classified bots : %d\n" (count @botResponses))
    ;;  (printf "Bot classification threshold: %f\n" )
    (printf "Correlations with a coefficient above %f" @correlationThreshhold)
    (doseq [{[^Question q1 ct1] :q1&ct [^Question q2 ct2] :q2&ct {coeff :coeff val :val :as corr} :corr} @correlations]
        (when (and val (expectedCorrelation survey q1 q2) (<= val @correlationThreshhold))
            (printf "Did not detect expected correlation between %s (%s) and %s (%s)\n"
                    (.toString q1) (.quid q1)
                    (.toString q2) (.quid q2)))
        (when val (> val @correlationThreshhold)
            (printf "Question 1: %s (%s)\t Question 2: %s (%s)\ncoeffcient type : %s\nexpected?%s\nother data : %s\n"
                    (.toString q1) (.quid q1)
                    (.toString q2) (.quid q2)
                    coeff
                    (expectedCorrelation survey q1 q2)
                    corr)
            )
        )
    (doseq [{q1 :q1 q2 :q2 num1 :numq1First num2 :numq2First {stat :stat val :val} :order} @orderBiases]
        (when val (> (val :p-value)  @correlationThreshhold)
            (printf "Question 1: %s (%s)\t Question 2: %s (%s)\nstat type : %sother data : %s\n"
                    (.toString q1) (.quid q1)
                    (.toString q2) (.quid q2)
                    stat
                    val
                    )
            )
        )
    (doseq [{q1 :q1 q2 :q2 num1 :numq1First num2 :numq2First {stat :stat val :val} :order} @variants]
        (when val (> (val :p-value) @correlationThreshhold)
            (printf "Question 1: %s (%s)\t Question 2: %s (%s)\nstat type : %sother data : %s\n"
                    (.toString q1) (.quid q1)
                    (.toString q2) (.quid q2)
                    stat
                    val
                    )
            )
        )
    )

(def validArgMap
    (str "USAGE:\n"
        "\t--report\tTakes values 'static' or 'dynamic'\n"
         "\t--surveySource\tThe path to the survey source file (csv)\n"
         "\t--surveySep\tThe character used for separating entries in the csv; default is ','\n"
         "\t--resultFile\tThe path to the survey's result file\n"
    )
)

(defn -main
    [& args]
    (let [argmap (into {} (map #(clojure.string/split % #"=") args))]
        (if-let [reportType (argmap "--report")]
            (if-let [filename (argmap "--surveySource")]
                (let [survey (cond (.endsWith filename ".csv") (-> (CSVLexer. filename (argmap "--surveySep" ","))
                                                                   (CSVParser.)
                                                                   (.parse))
                                   (.endsWith filename ".json") (.parse (JSONParser/makeParser filename))
                                   :else (throw Exception e (str "Unknown file type" (last (clojure.string/split filename #"\.")))))
                      ]
                    (condp = reportType
                        "static" (do (staticAnalyses survey)
                                     (printStaticAnalyses))
                        "dynamic" (if-let [resultFile (argmap "--resultFile")]
                                      (let [responses (-> (SurveyResponse. "")
                                                          (.readSurveyResponses survey (FileReader. resultFile)))
                                            qc (QC. survey)]
                                          (qc.analyses/classifyBots responses survey qc) ;;may want to put this in a dynamic analyses multimethod
                                          (dynamicAnalyses qc)
                                          (printDynamicAnalyses survey)
                                          )
                                      (println validArgMap))
                        :else (do (println (str "Unknown report type " reportType))
                                  (println validArgMap))
                        )
                    )
                (println validArgMap))
            (println validArgMap))
        )
    )
