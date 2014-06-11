(ns testCorrelation
  (:import (survey Question))
  (:use testLog)
  (:use clojure.test)
  (:require (qc analyses)))


(let [correlationThreshhold 0.5
      falseCorrelations (atom 0)
      totalTested (atom 0)
      epsilon 0.05]
  (deftest test-correlation
    (doall
      (doseq [[survey responses] (take 5 (shuffle (seq @response-lookup)))]
        (doseq [{[^Question q1 ct1] :q1&ct [^Question q2 ct2] :q2&ct {coeff :coeff val :val} :corr :as corrs} (qc.analyses/correlation responses survey)]
          ;(println corrs)
          (when (and val (> val 0) (qc.analyses/comparison-applies? q1 q2))
            (if (= q1 q2)
                (do
;                  (if-not (and (> val (- 1 epsilon)) (< val (+ 1 epsilon)))
;                    (println q1 (count (.options q1)) q2 (count (.options q2)) val)
;                    )
                  (is (and (> val (- 1 epsilon)) (< val (+ 1 epsilon)))))
                (when (> val correlationThreshhold)
                  (.warn LOGGER (format (str "Random respondents generated a correlation %s = %f > %f for questions"
                                          "%s (quid : %s, ct : %d, numOpts : %d) and "
                                          "%s (quid : %s, ct : %d, numOpts : %d)\n")
                                  coeff val correlationThreshhold
                                  q1 (.quid q1) ct1 (count (.options q1))
                                  q2 (.quid q2) ct2 (count (.options q2))))
                  (swap! falseCorrelations inc)
                  )
                )
              (swap! totalTested inc)
              )
            )
        (printf "\nNumber false correlations for %s: %d\n" (.sourceName survey) @falseCorrelations)
        (printf "\tTotal comparisons : %d\n" @totalTested)
        (flush)
        (reset! totalTested 0)
        (reset! falseCorrelations 0)
        )
      )
    )
  )


