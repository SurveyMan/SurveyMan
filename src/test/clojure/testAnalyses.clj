(ns testAnalyses
    (:import (qc RandomRespondent RandomRespondent$AdversaryType analyses)
             (csv CSVLexer CSVParser))
    (:use clojure.test)
    (:use testLog)
    )

(defn- getRandomSurveyResponses
    [survey n]
    (repeat n (RandomRespondent. survey RandomRespondent$AdversaryType/UNIFORM))
)

(deftest correlation
    (doseq [[filename sep] tests]
        (println filename)
        (let [survey (->> (CSVLexer. filename sep)
                          (CSVParser.)
                          (.parse))
              responses (map (fn [^RandomRespondent rr] (.response rr))
                             (getRandomSurveyResponses survey 100))]
            (try
                (print (first (qc.analyses/correlation responses survey)))
                (catch Exception e (.getMessage e)))
        )
    )
)