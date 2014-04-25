(ns testJS

    (:import (qc RandomRespondent RandomRespondent$AdversaryType)
             (system.localhost LocalResponseManager LocalLibrary Server)
             (system.localhost.generators LocalHTML)
             (system BackendType Library Record Slurpie)
             (survey SurveyResponse SurveyResponse$QuestionResponse Component Survey Question Block
                     Block$BranchParadigm)
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

(defn getAltOpt [^Survey survey ^String qid ^String oid]
    oid
    )

(deftest answerInvariant
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
             ]
            (HTML/spitHTMLToFile (HTML/getHTMLString survey (LocalHTML.)) survey)
            (Server/startServe)
            (AbstractResponseManager/chill 2)
            (assert (not= (count (Slurpie/slurp (.getHtmlFileName record))) 0))
            (let [driver (new-driver {:browser :firefox})]
                (to driver url)
                (println (html driver (find-element driver {:tag :body})))
                (try
                    (click driver "a#continue")
                    (catch Exception e (.getMessage e))
                    )
                (println (find-elements-under driver "div[name=question" {:tag :input}))
                (while (empty? (find-elements driver {:id "final_submit"}))
                    (let [qid (-> (find-elements-under "div[name=question]" {:tag :input})
                                  (first)
                                  (attribute :id))
                          oids (map #(.getCid ^Component %) (-> (.get q2ansMap qid) (.opts)))]
                        (doseq [oid oids]
                            (let [^Question q (.getQuestionById ^Survey survey qid)]
                                (if (= (.branchParadigm ^Block (.block q)) Block$BranchParadigm/ALL)
                                    (let [altoids (.getCid ^Component (getAltOpt survey qid oid))]
                                         (println (.source survey) q)
                                    )
                                    (select (find-element {:id oid})))
                                (click (find-element (str "id^=\"next\"_" qid)))
                                )
                            )
                        )
                    )
                (submit (find-elements {:id "final_submit"}))
                (while (empty? (.responses record)) (AbstractResponseManager/chill 2))
                (let [responses (.responses record)
                      responseMap (.resultsAsMap ^SurveyResponse (first responses))]
                    (is (= (count responses) 1))
                    (is (= responseMap q2ansMap)))
                (quit driver)
                (Server/endServe)
                )
            )
        )
    )