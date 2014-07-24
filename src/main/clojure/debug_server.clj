(ns debug-server
  (:gen-class
    :name DebugServer)
  (:import (system.generators JS)
           [system.mturk.generators MturkXML])
  (:import (input.csv CSVLexer CSVParser))
  (:use ring.adapter.jetty)
  (:use ring.middleware.params)
  (:use ring.util.codec)
  (:use clojure.walk)
  (:require report)
  (:import util.Slurpie)
  )

(def port 8001)

(defn get-content-type-for-request
  [uri]
  (condp = (last (clojure.string/split uri #"\\."))
    "js" "application/javascript"
    "css" "application/css"
    ""
    )
  )

(defn dynamic-analysis
  [{s :survey local :local survey-data :surveyData survey-results :surveyResults :as data}]
  (let [nomen (first (clojure.string/split (last (clojure.string/split s #"/")) #"\."))]
    (if (read-string local)
      'FOO
      (let [{responses :responses
             survey :survey}
            (report/setup
              "--origin=debugger"
              "--report=dynamic"
              "--classifier=all"
              (str "--results=data/results/" nomen "_results.csv") s)]
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
    )
  )

(defn static-analysis
  [{s :survey local :local survey-data :surveyData :as data}]
  (println data)
  (assert (<= (count survey-data) (MturkXML/maxQuestionXMLLength)))
  (when (read-string local)
    (println "creating local copy of " s)
    (spit s survey-data))
  (let [retval (clojure.string/replace
                  (try
                    (with-out-str (report/setup "--origin=debugger" "--report=static" s))
                    (catch Exception e (str "ERROR: " (.getMessage e))))
                "\n"
                "<br/>")]
    (when (read-string local)
      (println "deleting local copy of" s)
      (clojure.java.io/delete-file s))
    retval
    )
  )

(defn handle-post
  [uri {report :report :as body}]
  (println body)
  (condp = report
    "static" (static-analysis body)
    "dynamic" (dynamic-analysis body)
    (throw (Exception. (str "Unknown report type " report)))
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
    {:port port :join? false})
  )

