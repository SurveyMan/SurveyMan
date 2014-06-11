(ns testOrderBias
  (:import (util Printer))
  (:use clojure.test)
  (:use testLog)
  (:require (qc analyses))
  )


(let [falseOrderBias (atom 0)
      totalTested (atom 0)]
  (deftest test-orderBias
    (println 'test-orderBias)
    (doseq [[survey responses] (seq @response-lookup)]
      (let [ob (qc.analyses/orderBias responses survey)]
        (doseq [{q1 :q1 q2 :q2 num1 :numq1First num2 :numq2First {stat :stat val :val} :order} ob]
          (when (and val (< (val :p-value) alpha))
            (let [msg (format (str "Random respondent generated order bias (%s = %s)\nat the %f significance level between\n"
                                "\t%s (quid : %s, ct : %d, numOpts : %d) and\n"
                                "\t%s (quid : %s, ct : %d, numOpts : %d)\n")
                        stat val alpha
                        q1 (.quid q1) num1 (count (.options q1))
                        q2 (.quid q2) num2 (count (.options q2)))]
              (Printer/println msg)
              (.warn LOGGER msg)
              (swap! falseOrderBias inc)
              )
            )
          (swap! totalTested inc)
          )
        )
      (printf "Number false order bias for %s : %d\n" (.sourceName survey) @falseOrderBias)
      (printf "\tTotal comparisons : %d\n" @totalTested)
      (flush)
      )
    )
  )