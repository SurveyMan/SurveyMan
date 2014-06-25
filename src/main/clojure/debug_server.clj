(ns debug-server
  (:gen-class
    :name DebugServer)
  (:import (system.generators JS))
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

(defn handler [{request-method :request-method
                query-string :query-string
                uri :uri
                params :params
                :as request}]
  ;(println request)
  (println (format "request:%s\tquery:%s\turi:%s\tparams:%s\n" request-method query-string uri params))
  (when query-string
    (println (keywordize-keys (form-decode query-string))))
  {:status 200
   :headers {"Content-Type" (if (= :get request-method) (get-content-type-for-request uri) "text/html")
             }
   :body (condp = request-method
     :get (if query-string
            (let [{s :survey r :report local :local data :data} (keywordize-keys (form-decode query-string))
                   nomen (first (clojure.string/split (last (clojure.string/split s #"/")) #"\."))
                   results (clojure.string/replace
                              (try
                                (when (read-string local)
                                  (println 'DATA data)
                                  (spit s data))
                                (with-out-str (report/-main
                                            "--origin=debugger"
                                            ^String (str "--report=" r)
                                            ^String (str "--results=data/results/" nomen "_results.csv") ;; results are all in teh same place right now
                                            ^String s))
                                (catch Exception e (str "ERROR: " (.getMessage e))))
                              "\n"
                              "<br/>")]
              (println 'foo results)
              (if (.endsWith uri "sm")
                (JS/jsonizeSurvey (->> (CSVLexer. r) (CSVParser.) (.parse)))
                results
                )
              )
            (Slurpie/slurp (clojure.string/join "" (rest uri)))
            )
    )
   }
  )

(defn -main
  [& args]
  (ring.adapter.jetty/run-jetty
    handler
    {:port port :join? false})
  )

