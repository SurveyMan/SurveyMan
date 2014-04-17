(ns testAnalyses
    (:import (qc RandomRespondent RandomRespondent$AdversaryType analyses)
             (csv CSVLexer CSVParser))
    (:use clojure.test)
    (:use testLog)
    )

(def correlationThreshhold 0.5)
(def falseCorrelations (atom 0))

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
                             (getRandomSurveyResponses survey 1000))
              correlations (qc.analyses/correlation responses survey)]
            (doseq [{[q1 ct1] :q1&ct [q2 ct2] :q2&ct {coeff :coeff val :val} :corr} correlations]
                (when (and coeff val)
                    (if (= q1 q2)
                        (assert (= 1.0 val))
                        (when (> val correlationThreshhold)
                            (.warn LOGGER (format (str "Random respondent generated a correlation %s = %f > %f for questions"
                                                        "%s (quid : %s, ct : %d, numOpts : %d) and "
                                                        "%s (quid : %s, ct : %d, numOpts : %d)\n")
                                                  coeff val correlationThreshhold
                                                  q1 (.quid q1) ct1 (count (.options q1))
                                                  q2 (.quid q2) ct2 (count (.options q2))))
                            (swap! falseCorrelations inc)
                        )
                    )
                )
            )
            (printf "Number false correlations for %s: %d\n" filename @falseCorrelations)
        )
        (reset! falseCorrelations 0)
    )
)