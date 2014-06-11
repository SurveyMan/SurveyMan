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

(defn handler [{request-method :request-method
                query-string :query-string
                uri :uri
                params :params
                :as request}]
  ;(println request)
  (println (format "request:%s\tquery:%s\turi:%s\tparams:%s\n" request-method query-string uri params))
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (condp = request-method
     :get (if query-string
            (let [{s :survey r :report } (keywordize-keys (form-decode query-string))
                   nomen (first (clojure.string/split (last (clojure.string/split s #"/")) #"\."))
                   results (clojure.string/replace
                              (try
                                (with-out-str (report/-main
                                            ^String (str "--report=" r)
                                            ^String (str "--results=data/results/" nomen "_results.csv") ;; results are all in teh same place right now
                                            ^String s))
                                (catch Exception e (str "ERROR: " (.getMessage e))))
                              "\n"
                              "<br/>")]
              (if (.endsWith uri "sm")
                (JS/jsonizeSurvey (->> (CSVLexer. r) (CSVParser.) (.parse)))
                results
                )
              )
            (Slurpie/slurp (str "." uri))
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

