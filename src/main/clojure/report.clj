(ns report
  (:gen-class
    :name Report)
  (:use util)
  (:import (qc IQCMetrics Metrics)
           (survey Question Survey)
           (input.csv CSVParser CSVLexer)
           (system SurveyResponse JobManager)
           (system.generators JS)
           (input.json JSONParser)
           (java.io FileReader)
           (interstitial ISurveyResponse ITask Library AbstractResponseManager BackendType Record)
           (system.localhost LocalResponseManager LocalLibrary LocalTask)
           (system.mturk MturkResponseManager MturkLibrary MturkTask)
           (net.sourceforge.argparse4j ArgumentParsers)
           (net.sourceforge.argparse4j.inf ArgumentParser Namespace Argument)
           (util ArgReader Slurpie)
           (java.util Map))
  (:require [qc.analyses :exclude '[-main]])
  )

(def custom-headers (atom nil))
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

(def correlation-filename (atom "corr.csv"))

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
  (or (= q1 q2)
    (some identity
      (map #(and (contains? (set %) q1) (contains? (set %) q2))
        (vals (.correlationMap survey)))
      )
    )
  )

(defn dynamicAnalyses
  [^Record qc]
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

(defmethod staticAnalyses Record [qc]
           (staticAnalyses (.survey qc)))

(defn printStaticAnalyses
  []
  (printf "Custom headers provided: %s\n" (clojure.string/join "," @custom-headers))
  (printf "Average path length: %f\n" @avgPathLength)
  (printf "Minimum path length without breakoff: %d\n" @minPathLength)
  (printf "Maximum path length without breakoff: %d\n" @maxPathLength)
  (printf "Max possible bits to represent this survey: %f\n" @staticMaxEntropy)
  (printf "Calculated price per completed survey using strategy %s : %f\n" @strategy @basePrice)
  (flush)
  )

(defn print-breakoff
  []
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
        (printf "\t\t%s\t%s\t%d\n" (.quid q) q ct)))
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
      (printf "\t\tposition %d\t%d\n" pos ct)))
  (flush)
  )

(defn print-correlations
  [^Record qc]
  (printf "Correlations with a coefficient > %f\n" @correlationThreshhold)
  (spit @correlation-filename "q1,ct1,q2,ct2,coeff,val,expected\r\n")
  (flush)
  (doseq [{[^Question q1 ct1] :q1&ct [^Question q2 ct2] :q2&ct {coeff :coeff val :val :as corr} :corr :as entry} @correlations]
    (when (qc.analyses/correlation-applies? q1 q2)
      (let [expected (boolean (expectedCorrelation (.survey qc) q1 q2))]
        (spit @correlation-filename
          (str (clojure.string/join ","
                 [(.quid q1) ct1 (.quid q2) ct2 coeff (or val 0) expected]) "\n") :append true)
        (when (and val expected (< (Math/abs val) @correlationThreshhold))
          (printf "Did not detect expected correlation between %s (%s) and %s (%s)\n"
                (.toString q1) (.quid q1)
                (.toString q2) (.quid q2))
          (flush))
        (when (and (not expected) val (> (Math/abs val) @correlationThreshhold) (not= q1 q2) (> ct1 5) (> ct2 5))
          (printf "Question 1: %s (%s) ct: %d\nQuestion 2: %s (%s) ct: %d
                       \tcoeffcient type : %s
                       \tother data : %s\n\n"
            (.toString q1) (.quid q1) ct1
            (.toString q2) (.quid q2) ct2
            coeff corr))
        (flush)
        )
      )
    )
  )

(defn print-order-bias
  []
  (printf "Order biases with p-value < %f\n" @alpha)
  (doseq [{q1 :q1 q2 :q2 num1 :numq1First num2 :numq2First {stat :stat val :val} :order} @orderBiases]
    (when (and val (< (val :p-value)  @alpha) (> num1 5) (> num2 5))
      (printf "Question 1: %s (%s) count q1 first:%d\nQuestion 2: %s (%s) count q2 first:%d\n
                     stat type : %s\n
                     other data : %s\n"
              (.toString q1) (.quid q1) num1
              (.toString q2) (.quid q2) num2
              stat
              val
              )
      (flush)
      )
    )
  )

(defn print-wording-bias
  []
  (printf "Wording biases with p-value < %f\n" @alpha)
  (doseq [{[q1 ct1] :q1&ct [q2 ct2] :q2&ct {stat :stat val :val} :bias :as variant} (remove nil? (flatten @variants))]
    (when (and val (< (val :p-value) @alpha))
      (printf "Question 1: %s (%s) ct:%d\nQuestion 2: %s (%s) ct:%d
                     stat type : %s\n
                     other data : %s\n"
              (.toString q1) (.quid q1) ct1
              (.toString q2) (.quid q2) ct2
              stat
              val
              )
      (flush)
      )
    )
  )

(defn print-bonuses
  [^Record qc]
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
  (printf "Total bonus paid: %f\n" @bonus-paid)
  (flush)
  )

(defn print-bots
  []
  (doseq [^ISurveyResponse sr @botResponses]
    (let [workerid (.workerId sr)]
      (printf "\tWorker with id %s and score %f classified as bad actor with cutoff of %f; answered %d questions\n"
        workerid (.getScore sr) (.getThreshold sr) (count (qc.metrics/get-true-responses sr)))
      )
    )
  (flush)
  )

(defn print-nots
  []
  (doseq [^ISurveyResponse sr @validResponses]
    (let [workerid (.workerId sr)]
      (printf "\tWorker with id %s and score %f classified as a valid respondent with cutoff of %f; answered %d questions\n"
        workerid (.getScore sr) (.getThreshold sr) (count (qc.metrics/get-true-responses sr)))
      )
    )
  (flush)

  )

(defn print-debug-html
  [^Record record]
  (let [str-to-print (Slurpie/slurp "Debug.html")]
    ;(Printer/println str-to-print)
    (spit "report.html" str-to-print)
    )
  )

(defn print-separator
  []
  (println "-------------------------------------------------------------------------------------------------------"))

(defn printDynamicAnalyses
  [^Record qc]
  (printf "Total responses: %d\n" @total-responses)
  (printf "Total respondents: %d\n" (+ (count @botResponses) (count @validResponses)))
  (printf "Repeaters: %s\n" (set (deref qc.analyses/repeat-workers)))
  ;;(printf "Score cutoff for classifying bots: %s\n" (deref qc.metrics/cutoffs))
  (printf "Total number of classified bots: %d\n" (count @botResponses))
  (printf "Total number of valid responses: %d\n" (count @validResponses))
  ;;(printf "Bot classification threshold: %f\n" )
  ;; brekaoff goes here
  (print-separator)
  (print-breakoff)
  (print-separator)
  (print-correlations qc)
  (print-separator)
  (print-order-bias)
  (print-separator)
  (print-wording-bias)
  (print-separator)
  (print-bonuses qc)
  (print-separator)
  (print-bots)
  (print-separator)
  (print-nots)
  (print-separator)
  (print-debug-html qc)
  )

(defn get-library
  [^BackendType bt]
  (cond (= bt BackendType/LOCALHOST) (LocalLibrary. "")
        (= bt BackendType/MTURK) (MturkLibrary.)
        :else (throw (Exception. (str "Unknown backend " bt)))
        )
  )

(defn get-response-manager
  [^BackendType bt]
  (cond (= bt BackendType/LOCALHOST) (LocalResponseManager.)
        (= bt BackendType/MTURK) (MturkResponseManager. (get-library bt))
        :else (throw (Exception. (str "Unknown backend " bt)))
      )
  )

(defn add-survey-record
  [^Survey survey ^Library library ^BackendType backend]
  (AbstractResponseManager/putRecord survey (Record. survey library backend))
  )

(defn make-task-for-type
  [^BackendType bt ^Record r ^String taskid]
  (cond (= bt BackendType/LOCALHOST) (.makeTaskForId @responseManager r taskid)
        (= bt BackendType/MTURK) (.makeTaskForId @responseManager r taskid);; user surveyposter's service to get he hit for this id
        :else (throw (Exception. (str "Unknown backend " bt)))
        )
  )

(defn add-tasks
  [^BackendType backend ^Survey survey ^String stringOfHitIds]
  (when stringOfHitIds
    (let [record (AbstractResponseManager/getRecord survey)
          taskids (remove empty? (clojure.string/split stringOfHitIds #","))
          tasks (map #(make-task-for-type backend record %) taskids)]
      (println "num tasks made:" (count tasks))
      (doseq [task tasks]
        (.addNewTask record task))
      )
    )
  )


(defn -main
  [& args]
  (let [argument-parser (make-arg-parser "Report")]
    (try
      (let [^Namespace ns (try
                            (.parseArgs argument-parser args)
                            (catch Exception e (do ;;(.printStackTrace e)
                                                 (.parseArgs argument-parser (into-array String args)))))
            reportType (.getString ns "report")
            filename (.getString ns "survey")
            sep (.getString ns "separator")
            survey (cond (.endsWith filename ".csv") (-> (CSVLexer. filename sep) (CSVParser.) (.parse))
                         (.endsWith filename ".json") (.parse (JSONParser/makeParser filename))
                         :else (throw (Exception. (str "Unknown file type" (last (clojure.string/split filename #"\."))))))
            backend (BackendType/valueOf (.getString ns "backend"))
            library (get-library backend)]
        (reset! custom-headers (.otherHeaders survey))
        (reset! alpha (read-string (.getString ns "alpha")))
        (reset! pay-bonuses (read-string (.getString ns "payBonus")))
        (reset! responseManager (get-response-manager backend))
        (add-survey-record survey library backend)
        (add-tasks backend survey (.getString ns "hits"))
        (condp = reportType
          "static" (do (staticAnalyses survey)
                       (printStaticAnalyses))
          "dynamic" (let [resultFile (.getString ns "results")
                          responses (-> (SurveyResponse. "") (.readSurveyResponses survey (FileReader. resultFile)))
                          record (Record. survey library backend)]
                      (reset! total-responses (count responses))
                      (qc.analyses/classifyBots responses record (keyword (.getString ns "classifier"))) ;;may want to put this in a dynamic analyses multimethod
                      (dynamicAnalyses record)
                      (printDynamicAnalyses record)
                      )
          )
        )
      (catch Exception e (do (println (.getMessage e))
                           (.printStackTrace e)
                           ;(.printHelp argument-parser)
                           ;;(throw e)
                           )
        )
      )
    )
  )