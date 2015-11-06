(ns edu.umass.cs.surveyman.static-analysis-server
    (:gen-class
        :name edu.umass.cs.surveyman.StaticAnalysisServer
        :main true
    )
      (:use ring.adapter.jetty)
      (:use ring.middleware.params)
      (:use ring.util.codec)
      (:use clojure.walk)
)

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
                               body :body
                               :as request}]
     (when query-string
       (println (keywordize-keys (form-decode query-string))))
     {:status 200
      :headers {"Content-Type" (if (= :get request-method)
                                             (get-content-type-for-request uri)
                                             "text/html")}
      :body
      }
        )

(defn -main )
