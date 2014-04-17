(ns testAnalyses
    (:import (qc RandomRespondent RandomRespondent$AdversaryType)
             (csv CSVLexer CSVParser))
    (:require clojure.test)
    (:use testLog)
    )

(defn- getRandomSurveyResponses
    [survey n]
    (repeat n (RandomRespondent. survey RandomRespondent$AdversaryType/UNIFORM))
)

(deftest correlation
    (doseq [[filename sep] tests]
        (let [survey (->> (CSVLexer. filename sep)
                          (CSVParser.)
                          (.parse))
              responses (getRandomSurveyResponses survey 100)]
            (print (first (correlation survey responses)))
        )
    )
)