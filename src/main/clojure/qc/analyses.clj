;; dynamic analyses for SurveyMan
(ns qc.analyses
  (:import (survey Survey Question Component))
  (:require (incanter core stats))
)

(defn- make-ans-map
    [surveyResponses]
    (let [answers (for [sr surveyResponses]
                      (apply merge (for [qr (. sr responses)]
                                       {(. qr q) (with-meta {:srid (. sr srid)} (. qr opts))})))]
        (loop [answerMaps answers
               retval {}]
            (if (empty? answers)
                retval
                (recur (rest answers)
                       (reduce #(if (contains? %)))
    )

(defn correlation
    [surveyResponses ^Survey survey]

    (for [^Question q1 (. survey questions)]
        (for [^Question q2 (. survey questions)]
            (letfn [(getAnswers [question] (map (fn [sr] (first (filter (fn [r] (= (. r q) question))))) surveyResponses))
                    (getOrderings [q]  (apply hash-map (range 1 (count (.getOptListByIndex q))) (.getOptListByIndex q)))
                   ]
                (let [ ans1 (getAnswers q1)
                       ans2 (getAnswers q2)
                       ]
                    ;; make sure order is retained
                    (assert (map #(vector %1 %2) ans1 ans2))
                (if (and (. q1 exclusive) (. q2 exclusive))
                    (if (and (. q1 ordered) (. q2 ordered))
                        (let [r1 (getOrderings q1)
                              r2 (getOrderings q2)

                              ]
                            { :q1&ct [q1 (count ans1)]
                              :q2&ct [q2 (count ans2)]
                              :rho (incanter.stats/spearmans-rho (map #(get r1 %) ans1) (map #(get r2 %) ans2))
                            }
                        )
                        (let [tab (matrix (getAnswers q1)))])