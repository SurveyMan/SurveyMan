(ns testJS

    (:import (qc RandomRespondent RandomRespondent$AdversaryType)
             (system.localhost LocalResponseManager LocalLibrary Server LocalSurveyPoster)
             (system.localhost.generators LocalHTML)
             (system BackendType Library Record Slurpie Runner$BoxedBool Runner)
             (survey SurveyResponse SurveyResponse$QuestionResponse Component Survey Question Block
                     Block$BranchParadigm SurveyResponse$OptTuple)
             (system.interfaces AbstractResponseManager)
             (system.generators HTML))
    (:use testLog)
    (:use clojure.test)
    (:use clj-webdriver.taxi)
    (:require (qc analyses)))

;; generate answer set
;; start up server
;; navigate server
;; make sure returned SR corresponds to the answer set

(def numQ (atom 1))

(defn getAltOpt [^Survey survey ^String qid ^String oid]
    oid
    )

(defn subsetOf
    [ansMapS ansMapB]
    (reduce #(and %1 %2) true (map #(= (get ansMapS %) (get ansMapB %)) (keys ansMapS)))
    )

(deftest soundness
    (doseq [[filename sep] tests]
        (println "\nanswerInvariant" filename)
        (let [^Survey survey (makeSurvey filename sep)
              ^LocalLibrary lib (LocalLibrary. survey)
              q2ansMap (-> (RandomRespondent. survey RandomRespondent$AdversaryType/UNIFORM)
                           (.response)
                           (.resultsAsMap))
              ^Record record (do (LocalResponseManager/putRecord survey lib BackendType/LOCALHOST)
                                 (LocalResponseManager/getRecord survey))
              ^String url ( -> record
                               (.getHtmlFileName)
                               (.split (Library/fileSep))
                               (->> (last)
                                    (format "http://localhost:%d/logs/%s" Server/frontPort)))
              ^Runner$BoxedBool interrupt (Runner$BoxedBool. false)
              ^Thread runner (Thread. (fn [] (Runner/run record interrupt BackendType/LOCALHOST)))
             ]
            (Runner/init)
            (HTML/spitHTMLToFile (HTML/getHTMLString survey (LocalHTML.)) survey)
            (assert (not= (count (Slurpie/slurp (.getHtmlFileName record))) 0))
            (Server/startServe)
            (.start (Runner/makeResponseGetter survey interrupt BackendType/LOCALHOST))
            (.start runner)
            (let [driver (new-driver {:browser :firefox})]
                (to driver url)
                (println (html (find-element driver {:tag :body})))
                (try
                    (click driver "#continue")
                    (catch Exception e (println (.getMessage e))))
                (while (or (not (find-element driver {:id "final_submit"}))
                           (attribute (find-element driver {:id "final_submit"}) :hidden))
                    (let [qid (-> (find-element driver {:class "question"}) ;{:id (str "ans" @numQ)})
                                  (attribute :name))
                          oids (map #(.getCid ^Component (.c ^SurveyResponse$OptTuple %)) (-> (.get q2ansMap qid) (.opts)))]
                        (doseq [oid oids]
                            (let [^Question q (.getQuestionById ^Survey survey qid)]
                                (println oid q (.getOptById q oid))
                                (if (= (.branchParadigm ^Block (.block q)) Block$BranchParadigm/ALL)
                                    (let [altoids (.getCid ^Component (getAltOpt survey qid oid))]
                                         (println (.source survey) "variants:" q)
                                    )
                                    (cond (.freetext q) (input-text driver {:id qid} "FOO")
                                          (empty? (.options q)) :noop
                                          :else (select (find-element driver {:id oid})))
                                    )
                                )
                            )
                        (click driver (str "input#next_" qid))
                        )
                    (swap! numQ inc)
                    )
                (submit driver "#final_submit")
                (quit driver)
                (while (empty? (.responses record)) (AbstractResponseManager/chill 2))
                (.setInterrupt interrupt true)
                (AbstractResponseManager/chill 5)
                (let [responses (.responses record)
                      responseMap (.resultsAsMap ^SurveyResponse (first responses))]
                    (is (= (count responses) 1))
                    (subsetOf responseMap q2ansMap))
                (Server/endServe)
                (reset! numQ 1)
                (AbstractResponseManager/chill 3)
                )
            )
        )
    )