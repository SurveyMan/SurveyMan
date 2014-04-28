(ns testJS
    (:import (qc RandomRespondent RandomRespondent$AdversaryType)
             (system.localhost LocalResponseManager LocalLibrary Server LocalSurveyPoster)
             (system.localhost.generators LocalHTML)
             (system BackendType Library Record Runner$BoxedBool Runner)
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


(defn sampling?
    [^Question q]
    (= (.branchParadigm (.block q)) Block$BranchParadigm/ALL)
    )


(defn compute-offset
    [^String qid ^String oid]
    (let [a (read-string ((clojure.string/split qid #"_") 1))
          b (read-string ((clojure.string/split oid #"_") 1))]
        (- b a)
        )
    )

(defn getAltOid
    [^Question q ^String qid ^String oid]
    (let [offset (compute-offset qid oid)
          dat (clojure.string/split (.quid q) #"_")
          optcol ((clojure.string/split oid #"_") 2)]
        (if (sampling? q)
            (format "comp_%d_%s" (+ offset (read-string (dat 1))) optcol)
            oid
            )
        )
    )

(defn resolve-variant
    [^String qid ansMap ^Survey survey]
    ;; qid will be the question displayed in the html
    (let [variants (.getVariantSet survey (.getQuestionById survey qid))]
        (if (sampling? (.getQuestionById survey qid))
            (first (filter #(contains? ansMap (.quid ^Question %)) variants))
            (.q ^SurveyResponse$QuestionResponse (.get ansMap qid))
            )
        )
    )

(defn compare-answers
    [^SurveyResponse$QuestionResponse qr1 ^SurveyResponse$QuestionResponse qr2]
    (println "question responses:" qr1 "\n" qr2)
    (if (and (sampling? (.q qr1))
             (sampling? (.q qr2))
             (= (.block (.q qr1)) (.block (.q qr2))))
        (doseq [[opt1 opt2] (map vector (.opts qr1) (.opts qr2))]
            (let [offset1 (compute-offset (.quid (.q qr1)) (.getCid (.c opt1)))
                  offset2 (compute-offset (.quid (.q qr2)) (.getCid (.c opt2)))]
                (is (= offset1 offset2))
                )
            )
        (= qr1 qr2)
        )
    )

(defn subsetOf
    [ansMapS ansMapB s]
    (reduce #(and %1 %2) true
            (map #(let [q (.getQuestionById s %)]
                     (or (.freetext q)
                         (empty? (.options q))
                         (compare-answers (get ansMapS %) (get ansMapB (.quid (resolve-variant % ansMapB s))))
                         )
                     )
                 (keys ansMapS)
                 )
            )
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
                    (let [qid (attribute (find-element driver {:class "question"}) :name) ;{:id (str "ans" @numQ)})
                          qseen (.getQuestionById survey qid)
                          q (resolve-variant qid q2ansMap survey) ;; this is the q that's in the answer map
                          oids (map #(.getCid ^Component (.c ^SurveyResponse$OptTuple %)) (.opts (get q2ansMap (.quid q))))
                         ]
                        (doseq [oid oids]
                            (let [oidseen (getAltOid qseen (.quid q) oid)]
                                (println qseen ":" (.getOptById qseen oidseen) "\n" q ":" (.getOptById q oid))
                                (cond (.freetext qseen) (input-text driver {:id qid} "FOO")
                                      (empty? (.options qseen)) :noop
                                      :else (select (find-element driver {:id oidseen})))
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
                    (subsetOf responseMap q2ansMap survey))
                (Server/endServe)
                (reset! numQ 1)
                (AbstractResponseManager/chill 3)
                )
            )
        )
    )