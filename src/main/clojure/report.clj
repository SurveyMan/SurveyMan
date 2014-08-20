(ns report
  (:gen-class
    :name Report)
  (:use util)
  (:import (qc IQCMetrics Metrics)
           (survey Question Survey)
           (input.csv CSVParser CSVLexer)
           (system SurveyResponse JobManager QuestionResponse)
           (system.generators JS)
           (input.json JSONParser)
           (java.io FileReader)
           (interstitial ISurveyResponse ITask Library AbstractResponseManager BackendType Record OptTuple)
           (system.localhost LocalResponseManager LocalLibrary LocalTask)
           (system.mturk MturkResponseManager MturkLibrary MturkTask)
           (net.sourceforge.argparse4j ArgumentParsers)
           (net.sourceforge.argparse4j.inf ArgumentParser Namespace Argument)
           (util ArgReader Slurpie)
           (java.util Map))
  (:require [qc.analyses :exclude '[-main]])
  (:require [clojure.data.json :as json])
  )

(def costPerQuestion (* Library/timePerQuestionInSeconds (/ Library/FEDMINWAGE 60 60)))

(defn calculateBasePrice
  [^IQCMetrics qcMetrics ^Survey survey strategy]
  (condp = strategy
      :average-length (* (.averagePathLength qcMetrics survey) costPerQuestion)
      :max-length (* (.maximumPathLength qcMetrics survey) costPerQuestion)
      :min-length (* (.minimumPathLength qcMetrics survey) costPerQuestion)
      (throw (Exception. (str "Unknown strategy" strategy)))
    )
  )

(defn expected-correlation
    [^Survey survey ^Question q1 ^Question q2]
  (or (= q1 q2)
    (some identity
      (map #(and (contains? (set %) q1) (contains? (set %) q2))
        (vals (.correlationMap survey)))
      )
    )
  )

(defn jsonize-correlations
  [^Survey s valid-responses]
  (json/write-str (for [{[q1 ct1] :q1&ct [q2 ct2] :q2&ct {coeff :coeff val :val} :corr}
                        (qc.analyses/correlation valid-responses s)]
                    {:q1 (.quid ^Question q1)
                     :ct1 ct1
                     :q2 (.quid ^Question q2)
                     :ct2 ct2
                     :coeff coeff
                     :val val
                     :expected (expected-correlation s q1 q2)}
                    )
    )
  )

(defn jsonize-variants
  [^Survey survey valid-responses]
  (json/write-str (for [variant-question
                        (qc.analyses/wordingBias valid-responses survey)]
                    (for [{[q1 ct1] :q1&ct [q2 ct2] :q2&ct {stat :stat val :val} :bias} variant-question]
                      {:q1 (.quid q1) :ct1 ct1
                       :q2 (.quid q2) :ct2 ct2
                       :test stat
                       :val (if (= 'chi-squared stat)
                              {:Xsq (:X-sq val)
                               :pvalue (:p-value val)
                               :df (:df val)
                               }
                              {:U (:U val)
                               :pvalue (:p-value val)
                               })
                       }
                      )
                    )
    )
  )

(defn jsonize-order
  [^Survey survey valid-responses]
  (json/write-str (for [{q1 :q1 q2 :q2 numq1First :numq1First numq2First :numq2First {stat :stat val :val} :order}
                        (qc.analyses/orderBias valid-responses survey)]
                      {:q1 (.quid q1)
                       :q2 (.quid q2)
                       :numq1First numq1First
                       :numq2First numq2First
                       :stat stat
                       :val (if (:X-sq val)
                              {:X-sq (:X-sq val)
                               :pvalue (:p-value val)
                               :df (:df val)
                               }
                              (dissoc (assoc val :pval (:p-value val)) :p-value))
                       })
    )
  )

(defn jsonize-response
  [questionResponses]
  (json/write-str (for [^QuestionResponse qr questionResponses]
                    {:q (.quid qr)
                     :qindex (.getIndexSeen qr)
                     :opts (for [^OptTuple tupe (.getOpts qr)]
                             {:o (.getCid (.c tupe))
                              :oindex (.i tupe)
                              }
                             )
                      }
                    )
    )
  )

(defn jsonize-responses
  [^Survey survey valid-responses invalid-responses]
  (json/write-str (let [{botList :bot notList :not :or {botList nil notList nil}}
                        (qc.analyses/classify-bots
                           (concat valid-responses invalid-responses)
                           (AbstractResponseManager/getRecord survey)
                            :entropy-norm)]
                    (concat
                      (for [sr botList]
                        {:score (.getScore sr)
                         :valid true
                         :response {:id (.srid sr)
                                    :pval (.getThreshold sr)
                                    :responses (jsonize-response (.getResponses sr))
                                    }
                         }
                        )
                      (for [sr notList]
                        {:score (.getScore sr)
                         :valid false
                         :response {:id (.srid sr)
                                    :pval (.getThreshold sr)
                                    :responses (jsonize-response (.getResponses sr))
                                    }
                         }
                        )
                      )
                    )
    )
  )

(defn jsonize-breakoffs
  [^Survey s valid-responses invalid-responses]
  (json/write-str
    (let [{valid-responses true invalid-responses false}
          (group-by #(:valid (% 0)) (qc.analyses/all-breakoff-data valid-responses invalid-responses))]
      (loop [v (map #(assoc (% 0) :ct (% 1)) valid-responses)
             i (map #(assoc (% 0) :ct (% 1)) invalid-responses)
             retval (transient [])]
        (if (empty? v)
          (let [return-me (concat (map #(dissoc (assoc % :ctInvalid (:ct %)) :ct) i) (persistent! retval))]
            (assert (= (count return-me) (count (set return-me))))
             return-me)
          (let [me (first v)
                you (first (filter #(and (= (:question %) (:question me)) (= (:position %) (:position me))) i))]
            ;;(println me you)
            (if you
              (recur (rest v)
                (remove #(and (= (:question %) (:question me)) (= (:position %) (:position me))) i)
                (conj! retval {:q (.quid (:question me)) :pos (:position me) :ctValid (:ct me) :ctInvalid (:ct you)}))
              (recur (rest v)
                i
                (conj! retval {:q (.quid (:question me)) :pos (:position me) :ctValid (:ct me) :ctInvalid 0}))
              )
            )
          )
        )
      )
    )
  )

(defn print-static-analysis
  [{custom-headers :custom-headers
    avg-path-length :avg-path-length
    min-path-length :min-path-length
    max-path-length :max-path-length
    max-ent :max-ent
    strategy :strategy
    base-price :base-price} static-analyses]
  (printf "Custom headers provided: %s\n" custom-headers)
  (printf "Average path length: %f\n" avg-path-length)
  (printf "Minimum path length without breakoff: %d\n" min-path-length)
  (printf "Maximum path length without breakoff: %d\n" max-path-length)
  (printf "Max possible bits to represent this survey: %f\n" max-ent)
  (printf "Calculated price per completed survey using strategy %s : %f\n" strategy base-price)
  (flush)
  )

(defn static-analysis
  [^IQCMetrics qcMetrics ^Survey survey strategy]
  { :custom-headers (clojure.string/join "," (.otherHeaders survey))
    :avg-path-length (.averagePathLength qcMetrics survey)
    :min-path-length (.minimumPathLength qcMetrics survey)
    :max-path-length (.maximumPathLength qcMetrics survey)
    :max-ent (.getMaxPossibleEntropy qcMetrics survey)
    :strategy strategy
    :base-price (calculateBasePrice qcMetrics survey strategy)
    :simulations (.simulations qcMetrics survey)
    }
  )


;(defn print-bonuses
;  [^Record qc]
;  (printf "Bonuses:\n")
;  (doseq [^ISurveyResponse sr @validResponses]
;    (let [workerid (.workerId sr)
;          bonus (.calculateBonus qcMetrics sr qc)
;          ^Survey survey (.survey qc)
;          ]
;      (when (and @pay-bonuses (not (JobManager/bonusPaid sr survey)))
;        (do
;          (swap! bonus-paid + bonus)
;          (.awardBonus @responseManager bonus sr survey)
;          (JobManager/recordBonus bonus sr survey)
;          (printf "\tWorker with id %s and score %f recieves bonus of %f\n" workerid (.getScore sr) bonus)
;          )
;        )
;      )
;    )
;  (printf "Total bonus paid: %f\n" @bonus-paid)
;  (flush)
;  )

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
  [^AbstractResponseManager response-manager ^BackendType bt ^Record r ^String taskid]
  (cond (= bt BackendType/LOCALHOST) (.makeTaskForId response-manager r taskid)
        (= bt BackendType/MTURK) (.makeTaskForId response-manager r taskid);; user surveyposter's service to get he hit for this id
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


(defn setup
  [argument-parser args]
  (let [^Namespace ns (try
                        (.parseArgs argument-parser args)
                        (catch Exception e (.parseArgs argument-parser (into-array String args))))]
    (try
      (let [reportType (.getString ns "report")
            filename (.getString ns "survey")
            sep (.getString ns "separator")
            strategy (keyword (.getString ns "strategy"))
            survey (cond (.endsWith filename ".csv") (-> (CSVLexer. filename sep) (CSVParser.) (.parse))
                         (.endsWith filename ".json") (.parse (JSONParser/makeParser filename))
                         :else (throw (Exception. (str "Unknown file type" (last (clojure.string/split filename #"\."))))))
            backend (BackendType/valueOf (.getString ns "backend"))
            library (get-library backend)
            record (Record. survey library backend)
            qcMetrics (qc.Metrics.)
            retval {:reportType reportType
                    :filename filename
                    :sep sep
                    :strategy strategy
                    :survey survey
                    :backend backend
                    :library library
                    :record record}]
        (add-survey-record survey library backend)
        (add-tasks backend survey (.getString ns "hits"))
        (condp = reportType
          "static" (assoc retval :analyses (static-analysis qcMetrics survey strategy))
          "dynamic" (let [resultFile (.getString ns "results")
                          responses (-> (SurveyResponse. "") (.readSurveyResponses survey (FileReader. resultFile)))
                          classifier (keyword (.getString ns "classifier"))
                          ]
                      (qc.analyses/classify-bots responses record classifier)
                      (assoc retval
                        :resultFile resultFile
                        :responses responses
                        :classifier classifier)
                      )
          )
        )
      (catch Exception e (condp = (.getString ns "origin")
                           "cmdline" (do
                                        (println (.getMessage e))
                                        (.printHelp argument-parser))
                           "debugger" (do
                                        (.printStackTrace e)
                                        (throw e))
                           )
        )
      )
    )
  )

(defn -main
  [& args]
  (let [argument-parser (make-arg-parser "Report")]
    (if (= 0 (count args))
      (.printHelp argument-parser)
      (println (setup argument-parser args))
      )
    )
  )