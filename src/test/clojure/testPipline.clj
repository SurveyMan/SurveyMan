(ns testPipeline
  (:import (survey Survey)
           (system Runner))
  (:use testLog)
  (:use clojure.test)
  )

(deftest testRunnerDefaults
  []
  (doseq [^Survey survey response-lookup]
    (let [runner (agent [(.source survey)])]
      (send runner Runner/main)
      (Thread/sleep 5000)
      (shutdown-agents)
      )
    )
  )
