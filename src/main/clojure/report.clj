(ns report
    (:gen-class
        :name Report
    )
    (:import (qc QC IQCMetrics Metrics)
             (survey Question Survey)
             (input.csv CSVParser CSVLexer)
             (system SurveyResponse)
             (system.generators JS)
             (input.json JSONParser)
             (java.io FileReader)
             (interstitial ISurveyResponse ITask Library AbstractResponseManager))
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
(def correlationThreshhold (atom 0.6))
(def alpha (atom 0.05))
(def basePrice (atom 0.10))
(def strategy (atom :average-length))
(def pay-bonuses (atom false))
(def bonus-paid (atom 0.0))
(def ^AbstractResponseManager responseManager (atom nil))
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

(defn expectedCorrelation
    [^Survey survey ^Question q1 ^Question q2]
    (some identity
        (map #(and (contains? (set %) q1) (contains? (set %) q2))
              (vals (.correlationMap survey)))
        )
    )

(defn dynamicAnalyses
    [^QC qc]
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
    (flush)
    )

(defn printDynamicAnalyses
    [^QC qc]
    (printf "Total number of classified bots : %d\n" (count @botResponses))
    (printf "Total number of vaid responses: %d\n" (count @validResponses))
    ;;  (printf "Bot classification threshold: %f\n" )
    (printf "Correlations with a coefficient > %f\n" @correlationThreshhold)
    (flush)
    (doseq [{[^Question q1 ct1] :q1&ct [^Question q2 ct2] :q2&ct {coeff :coeff val :val :as corr} :corr} @correlations]
        (when (and val (expectedCorrelation (.survey qc) q1 q2) (<= val @correlationThreshhold))
            (printf "\tDid not detect expected correlation between %s (%s) and %s (%s)\n"
                    (.toString q1) (.quid q1)
                    (.toString q2) (.quid q2))
            (flush)
            )
        (when (and val (> val @correlationThreshhold) (not= q1 q2) (> ct1 5) (> ct2 5))
            (printf "Question 1: %s (%s) ct: %d\n
                     Question 2: %s (%s)ct: %d\n
                     \tcoeffcient type : %s\n
                     \texpected?%s\n
                     \tother data : %s\n"
                    (.toString q1) (.quid q1) ct1
                    (.toString q2) (.quid q2) ct2
                    coeff
                    (expectedCorrelation (.survey qc) q1 q2)
                    corr)
            (flush)
            )
        )
    (printf "Order biases with p-value < %f\n" @alpha)
    (doseq [{q1 :q1 q2 :q2 num1 :numq1First num2 :numq2First {stat :stat val :val} :order} @orderBiases]
        (when (and val (< (val :p-value)  @alpha) (> num1 5) (> num2 5))
            (printf "Question 1: %s (%s)\n
                     Question 2: %s (%s)\n
                     \tstat type : %s\n
                     \tother data : %s\n"
                    (.toString q1) (.quid q1)
                    (.toString q2) (.quid q2)
                    stat
                    val
                    )
            (flush)
            )
        )
    (printf "Wording biases with p-value < %f\n" @alpha)
    (doseq [{q1 :q1 q2 :q2 num1 :numq1First num2 :numq2First {stat :stat val :val} :order} @variants]
        (when (and val (< (val :p-value)  @alpha))
            (printf "Question 1: %s (%s)\n
                     Question 2: %s (%s)\n
                     \tstat type : %s\n
                     \tother data : %s\n"
                    (.toString q1) (.quid q1)
                    (.toString q2) (.quid q2)
                    stat
                    val
                    )
            (flush)
            )
        )
    (printf "Bonuses:\n")
    (doseq [^ISurveyResponse sr @validResponses]
        (let [workerid (.workerId sr)
              bonus (.calculateBonus qcMetrics sr qc)
              ^Survey survey (.survey qc)
              ]
            (printf "\tWorker with id %s and score %f recieves bonus of %f\n" workerid (.getScore sr) bonus)
            (when @pay-bonuses (.awardBonus @responseManager bonus sr survey))
            )
        )
    (doseq [^ISurveyResponse sr @botResponses]
        (let [workerid (.workerId sr)
              bonus (* 0.01 (count (.getResponses sr)));;(.calculateBonus qcMetrics sr qc)
              ^Survey survey (.survey qc)
              ]
            (printf "\tWorker with id %s and score %f classified as bot; would recieve bonus of %f\n" workerid (.getScore sr) bonus)
            (when @pay-bonuses (.awardBonus @responseManager bonus sr survey))
            )
        )
    (flush)
    )

(def validArgMap
    (str "USAGE:\n"
        "\t--report\tTakes values 'static' or 'dynamic'\n"
         "\t--surveySource\tThe path to the survey source file (csv)\n"
         "\t--surveySep\tThe character used for separating entries in the csv; default is ','\n"
         "\t--resultFile\tThe path to the survey's result file\n"
         "\t--payBonus\tBoolean for paying bonuses to workers."
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
                                          (qc.analyses/classifyBots responses qc :entropy) ;;may want to put this in a dynamic analyses multimethod
                                          (dynamicAnalyses qc)
                                          (printDynamicAnalyses qc)
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
