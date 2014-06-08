(ns testVariants
  (:import (util Printer))
  (:use clojure.test)
  (:use testLog)
  (:require (qc analyses))
  )

(let [falseBias (atom 0)
      totalTested (atom 0)]
  (deftest test-variantBias
    ;; we only have one survey with variants right now
    (let [[survey responses] (get-survey-and-responses-by-filename "data/samples/prototypicality.csv")]
      (let [variantsList (flatten (qc.analyses/wordingBias responses survey))]
        (doseq [variants variantsList]
          (let [{[q1 ct1] :q1&ct [q2 ct2] :q2&ct {stat :stat val :val} :bias} variants]
            (when (and val (< (val :p-value) alpha))
              (let [msg (format (str "Random respondent generated variant bias (%s = %s)\nat the %f signficance level between\n"
                                  "\t%s (quid : %s, ct : %d, numOpts : %d) and\n"
                                  "\t%s (quid : %s, ct : %d, numOpts : %d)\n")
                          stat val alpha
                          q1 (.quid q1) ct1 (count (.options q1))
                          q2 (.quid q2) ct2 (count (.options q2)))]
                (.warn LOGGER msg)
                (Printer/println msg))
              (swap! falseBias inc))
            (swap! totalTested inc))))
      (printf "Number false variant bias for %s : %d\n" (.sourceName survey) @falseBias)
      (printf "\tTotal comparisons : %d\n" @totalTested)
      (flush)
      )
    )
  )
