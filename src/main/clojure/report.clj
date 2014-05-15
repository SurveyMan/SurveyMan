(ns report
    (:gen-class
        :name Report
    )
    (:import (qc QC IQCMetrics Metrics)
             (survey Question Survey)
             (input.csv CSVParser CSVLexer)
             (system SurveyResponse JobManager)
             (system.generators JS)
             (input.json JSONParser)
             (java.io FileReader)
             (interstitial ISurveyResponse ITask Library AbstractResponseManager BackendType Record)
             (system.localhost LocalResponseManager LocalLibrary LocalTask)
             (system.mturk MturkResponseManager MturkLibrary MturkTask))
    (:require [qc.analyses :exclude '[-main]])
    )

(def validResponses (atom nil))
(def botResponses (atom nil))
(def breakoffQuestions (atom nil))
(def breakoffPositions (atom nil))
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
(def total-responses (atom 0))
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
    (reset! breakoffQuestions (qc.analyses/breakoffQuestions @validResponses @botResponses))
    (reset! breakoffPositions (qc.analyses/breakoffPositions @validResponses @botResponses))
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
    (printf "Total respondents: %d\n" (+ (count @botResponses) (count @validResponses)))
    (printf "Repeaters: %s\n" (deref qc.analyses/repeat-workers))
    (printf "Score cutoff for classifying bots: %s\n" (deref qc.metrics/cutoffs))
    (printf "Total number of classified bots: %d\n" (count @botResponses))
    (printf "Total number of vaid responses: %d\n" (count @validResponses))
    ;;  (printf "Bot classification threshold: %f\n" )
    ;; brekaoff goes here
    (printf "Top half breakoff questions\n")
    (let [{qlist1 :valid-responses qlist2 :bot-responses qlist3 :all} (qc.analyses/breakoffQuestions @validResponses @botResponses)]
        (println "\n\tAmong valid responses:\n")
        (doseq [[q ct] qlist1]
            (printf "\t\t%s\t%s\t%d\n" (.quid q) q ct))
        (println "\n\tAmong invalid responses:\n")
        (doseq [[q ct] qlist2]
            (printf "\t\t%s\t%s\t%d\n" (.quid q) q ct))
        (println "\n\tAmong all responses:\n")
        (doseq [[q ct] qlist3]
            (printf "\t\t%s\t%s\t%d\n" (.quid q) q ct))
        )
    (printf "\nTop half breakoff positions\n")
    (let [{qlist1 :valid-responses qlist2 :bot-responses qlist3 :all} (qc.analyses/breakoffPositions @validResponses @botResponses)]
        (println "\n\tAmong valid responses:\n")
        (doseq [[pos ct] qlist1]
            (printf "\t\tposition %d\t%d\n" pos ct))
        (println "\n\tAmong invalid responses:\n")
        (doseq [[pos ct] qlist2]
            (printf "\t\tposition %d\t%d\n" pos ct))
        (println "\n\tAmong all responses:\n")
        (doseq [[pos ct] qlist3]
            (printf "\t\tposition %d\t%d\n" pos ct))
        )
    (flush)
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
            (printf "\tQuestion 1: %s (%s) ct: %d
                     Question 2: %s (%s) ct: %d
                     \tcoeffcient type : %s
                     \texpected? %s
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
            (when (and @pay-bonuses (not (JobManager/bonusPaid sr survey)))
                (do
                    (swap! bonus-paid + bonus)
                    (.awardBonus @responseManager bonus sr survey)
                    (JobManager/recordBonus bonus sr survey)
                    (printf "\tWorker with id %s and score %f recieves bonus of %f\n" workerid (.getScore sr) bonus)
                    )
                )
            )
        )
    (flush)
    (doseq [^ISurveyResponse sr @botResponses]
        (let [workerid (.workerId sr)
              bonus (* 0.01 (count (.getResponses sr)));;(.calculateBonus qcMetrics sr qc)
              ^Survey survey (.survey qc)
              ]
            (printf "\tWorker with id %s and score %f classified as bot; would recieve bonus of %f\n" workerid (.getScore sr) bonus)
            )
        )
    (printf "Total bonus paid: %f\n" @bonus-paid)
    (flush)
    )

(defn get-response-manager
    [^BackendType bt]
    (cond (= bt BackendType/LOCALHOST) (LocalResponseManager.)
          (= bt BackendType/MTURK) (MturkResponseManager.)
          :else (throw (Exception. (str "Unknown backend " bt)))
        )
    )

(defn add-survey-record
    [^Survey survey ^Library library ^BackendType backend]
    (AbstractResponseManager/putRecord survey (Record. survey library backend))
    )

(defn make-task-for-type
    [^BackendType bt ^Record r ^String taskid]
    (cond (= bt BackendType/LOCALHOST) (LocalTask. r)
          (= bt BackendType/MTURK) (MturkTask. (.hit (.getTask @responseManager taskid)) r);; user surveyposter's service to get he hit for this id
          :else (throw (Exception. (str "Unknown backend " bt)))
          )
    )

(defn add-tasks
    [^BackendType backend ^Survey survey ^String stringOfHitIds]
    (let [record (AbstractResponseManager/getRecord survey)
          taskids (remove empty? (clojure.string/split stringOfHitIds #","))
          tasks (map #(make-task-for-type backend record %) taskids)]
        (doseq [task tasks]
            (.addNewTask record task)
            )
        )
    )

(defn get-library
    [^BackendType bt]
    (cond (= bt BackendType/LOCALHOST) (LocalLibrary.)
          (= bt BackendType/MTURK) (MturkLibrary.)
          :else (throw (Exception. (str "Unknown backend " bt)))
          )
    )

(def validArgMap
    (str "USAGE:\n"
        "\t--report\tTakes values 'static' or 'dynamic'\n"
         "\t--surveySource\tThe path to the survey source file (csv)\n"
         "\t--surveySep\tThe character used for separating entries in the csv; default is ','\n"
         "\t--resultFile\tThe path to the survey's result file\n"
         "\t--payBonus\tBoolean for paying bonuses to workers\n"
         "\t--backend\tMTURK or LOCALHOST\n"
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
                      backend (BackendType/valueOf (clojure.string/upper-case (argmap "--backend" "localhost")))
                      library (get-library backend)
                      ]
                    (reset! pay-bonuses (read-string (argmap "--payBonus" "false")))
                    (reset! responseManager (get-response-manager backend))
                    (add-survey-record survey library backend)
                    (add-tasks backend survey (argmap "--hits" ""))
                    (condp = reportType
                        "static" (do (staticAnalyses survey)
                                     (printStaticAnalyses))
                        "dynamic" (if-let [resultFile (argmap "--resultFile")]
                                      (let [responses (-> (SurveyResponse. "")
                                                          (.readSurveyResponses survey (FileReader. resultFile)))
                                            qc (QC. survey)]
                                          (reset! total-responses (count responses))
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