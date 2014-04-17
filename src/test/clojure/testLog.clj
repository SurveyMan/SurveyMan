(ns testLog
    (:import (org.apache.log4j Logger FileAppender PatternLayout)
             (system Slurpie)
             (java.util.regex Pattern))
    (:require [clojure.string :as s]))

(def tests
    (map #(s/split % #"\\s+" )
          (s/split (Slurpie/slurp "test_data.txt") #"\\n\\r?")))

(def LOGGER (Logger/getLogger (str (ns-name *ns*))))

(let [^FileAppender txtHandler (FileAppender. (PatternLayout. "%d{dd MMM yyyy HH:mm:ss,SSS}\t%-5p [%t]: %m%n")
                                              (format "logs/%s.log" (str (ns-name *ns*))))]
    (.setEncoding txtHandler "UTF-8")
    (.setAppend txtHandler false)
    (.addAppender LOGGER txtHandler))