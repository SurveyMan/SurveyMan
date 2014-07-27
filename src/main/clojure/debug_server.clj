(ns debug-server
  (:gen-class
    :name DebugServer)
  (:import [system.generators JS]
           [system.mturk.generators MturkXML]
           [qc Metrics IQCMetrics])
  (:import (input.csv CSVLexer CSVParser))
  (:use ring.adapter.jetty)
  (:use ring.middleware.params)
  (:use ring.util.codec)
  (:use clojure.walk)
  (:require report)
  (:import util.Slurpie)
  )

(def PORT 8001)
(def RESULTS-SUFFIX "_results.csv")

(defn nomen
  [url]
  (first (clojure.string/split (last (clojure.string/split url #"/")) #"\."))
  )

(defn get-content-type-for-request
  [uri]
  (condp = (last (clojure.string/split uri #"\\."))
    "js" "application/javascript"
    "css" "application/css"
    ""
    )
  )

(defn dynamic-analysis
  [{s :survey local :local local-survey :local-survey local-results :local-results survey-data :surveyData survey-results :surveyResults :as data}]
  (let [is-local (read-string local)
        {responses :responses
        survey :survey} (report/setup
                            "--origin=debugger"
                            "--report=dynamic"
                            "--classifier=all"
                            (str "--results=" (if is-local local-results (str "data/results/" (nomen s) RESULTS-SUFFIX)))
                            (if is-local local-survey s))]
    (format (str "{ \"sm\" : %s,"
                   "\"corrs\" : %s,"
                   "\"variants\": %s,"
                   "\"order\": %s,"
                   "\"responses\": %s,"
                   "\"bkoffs\" : %s}")
      (JS/jsonizeSurvey survey)
      (report/jsonize-correlations survey responses)
      (report/jsonize-variants survey responses)
      (report/jsonize-order survey responses)
      (report/jsonize-responses survey responses '())
      (report/jsonize-breakoffs survey responses '())
      )
    )
  )

(defn static-analysis
  [{s :survey local :local survey-data :surveyData :as data}]
  (assert (<= (count survey-data) (MturkXML/maxQuestionXMLLength)))
  (let [retval (try
                (let [{survey :survey
                       strategy :strategy} (report/setup "--origin=debugger" "--report=static" s)
                      qcMetrics (qc.Metrics.)]
                  (println strategy)
                  (println qcMetrics)
                  (str (format "Custom headers provided: %s<br/>" (clojure.string/join "," (.otherHeaders survey)))
                    (format "Average path length: %f<br/>" (.averagePathLength qcMetrics survey))
                    (format "Minimum path length without breakoff: %d<br/>" (.minimumPathLength qcMetrics survey))
                    (format "Maximum path length without breakoff: %d<br/>" (.maximumPathLength qcMetrics survey))
                    (format "Max possible bits to represent this survey: %f<br/>" (.getMaxPossibleEntropy qcMetrics survey))
                    (format "Calculated price per completed survey using strategy %s : %f<br/>" strategy (report/calculateBasePrice qcMetrics survey strategy))))
                  (catch Exception e
                    (do
                      (.printStackTrace e)
                      (str "ERROR: " (.getMessage e)))))]
    retval
    )
  )

(defn handle-post
  [uri {s :survey local :local report :report a :survey survey-data :surveyData survey-results :resultsData :as body}]
  (let [local-name (gensym "survey_")
        is-local (read-string local)
        body (if is-local (assoc body :local-survey (str local-name ".csv") :local-results (str local-name RESULTS-SUFFIX)) body)
        ]
    (when is-local
      (println "creating local copy of" local-name)
      (spit (str local-name ".csv") survey-data)
      (when (read-string survey-results)
        (println "creating local copy of" local-name "results")
        (spit (str local-name RESULTS-SUFFIX) survey-results)
        )
      )
    (let [retval (condp = report
                    "static" (static-analysis body)
                    "dynamic" (dynamic-analysis body)
                    (throw (Exception. (str "Unknown report type " report)))
                    )]
      (when (read-string local)
        (println "deleting local copy of" local-name)
        (clojure.java.io/delete-file (str local-name ".csv"))
        (when survey-results
          (println "deleting local copy of" local-name "results")
          (clojure.java.io/delete-file (str local-name RESULTS-SUFFIX))
          )
        )
      retval)
    )
  )

(defn handler [{request-method :request-method
                query-string :query-string
                uri :uri
                params :params
                body :body
                :as request}]
  (println (format "request:%s\tquery:%s\turi:%s\tparams:%s\n" request-method query-string uri params))
  (when query-string
    (println (keywordize-keys (form-decode query-string))))
  {:status 200
   :headers {"Content-Type" (if (= :get request-method) (get-content-type-for-request uri) "text/html")
             }
   :body (condp = request-method
     :get (Slurpie/slurp (clojure.string/join "" (rest uri)))
     :post (handle-post uri (keywordize-keys (form-decode (slurp body))))
    )
   }
  )

(defn -main
  [& args]
  (ring.adapter.jetty/run-jetty
    handler
    {:port PORT :join? false})
  )

