(ns testLog
    (:gen-class)
    (:import (util Slurpie))
    (:import (org.apache.log4j Logger FileAppender PatternLayout)
             (java.util.regex Pattern)
             (qc RandomRespondent RandomRespondent$AdversaryType Metrics)
             (input.csv CSVParser CSVLexer))
    (:require [clojure.string :as s]
              [qc.metrics]))


(def numResponses 250)
(def qcMetrics (Metrics.))


(defn generateNRandomResponses
    [survey]
    (map (fn [^RandomRespondent rr] (.response rr))
         (qc.metrics/getRandomSurveyResponses survey numResponses))
    )

(defn makeSurvey
    [filename sep]
    (->> (CSVLexer. filename sep)
         (CSVParser.)
         (.parse))
    )

(def tests
    (map #(s/split % #"\s+" )
          (s/split (Slurpie/slurp "test_data")
                   (re-pattern (System/getProperty "line.separator")))))

(def LOGGER (Logger/getLogger (str (ns-name *ns*))))

(let [^FileAppender txtHandler (FileAppender. (PatternLayout. "%d{dd MMM yyyy HH:mm:ss,SSS}\t%-5p [%t]: %m%n")
                                              (format "logs/%s.log" (str (ns-name *ns*))))]
    (.setEncoding txtHandler "UTF-8")
    (.setAppend txtHandler false)
    (.addAppender LOGGER txtHandler))


