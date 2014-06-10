 (ns testCheckbox
     (:import (survey Survey Question Component)
              (interstitial Record BoxedBool AbstractResponseManager ISurveyResponse IQuestionResponse OptTuple)
              (system.localhost LocalLibrary Server)
              (system Runner)
              (system.generators HTML)
              (system.localhost.generators LocalHTML)
              (util Slurpie))
     (:use testLog)
     (:use clojure.test)
     (:use clj-webdriver.taxi)
     )


(deftest testMultiSelect
    (let [csv (str "QUESTION,OPTIONS,EXCLUSIVE\nbar,0,false\n" (clojure.string/join "\n" (map #(format ",%d," %) (range 1 20))))
          dummy-file-name "foo.csv"]
        (spit dummy-file-name csv)
        (let [^Survey survey (makeSurvey dummy-file-name ",")
              ^Question q (first (.questions survey))
              answers (take (rand-int (count (.options q))) (shuffle (vals (.options q))))
              ^Record record (Record. survey lib bt)
              ^String url (sm-get-url record)
              ^BoxedBool interrupt (BoxedBool.)
              runner (agent (fn [] (do (Runner/init bt) (Runner/run record))))
              response-getter (agent (Runner/makeResponseGetter survey))
              ]
            (AbstractResponseManager/putRecord survey record)
            (HTML/spitHTMLToFile (HTML/getHTMLString record (LocalHTML.)) survey)
            (assert (not= (count (Slurpie/slurp (.getHtmlFileName record))) 0))
            (Server/startServe)
            (send runner #(%))
            (send response-getter #(.start %))
            (Thread/sleep 2000)
            (let [driver (new-driver {:browser :firefox})]
                ; click around answers
                (to driver url)
                (try
                    (click driver "#continue")
                    (catch Exception e (println "No continue button?" (.getMessage e))))
                ; click on all the answers
                (doseq [^Component answer answers]
                    (select (find-element driver {:id (.getCid answer)}))
                    (is (find-element driver {:id SUBMIT_FINAL}))
                    )
                ; un-check all the answers
                (doseq [^Component answer answers]
                    (toggle (find-element driver {:id (.getCid answer)}))
                    )
                (is (not (find-element driver {:id SUBMIT_FINAL})))
                ; check the answers again and submit
                (doseq [^Component answer answers]
                    (select (find-element driver {:id (.getCid answer)}))
                    (is (find-element driver {:id SUBMIT_FINAL}))
                    )
                (submit driver (str "#" SUBMIT_FINAL))
                ; check that the answers are what we expect
                (while (empty? (.validResponses record))
                    (Thread/sleep 1000)
                    )
                (let [^ISurveyResponse response (first (.validResponses record))
                      ^IQuestionResponse ans (first (vals (.resultsAsMap response)))]
                    (println "checking responses...")
                    (doseq [^Component c (map #(.c ^OptTuple %) (.getOpts ans))]
                        (is (contains? (set answers) c))
                        )
                    )
                (quit driver)
                (send response-getter #(.join %))
                (shutdown-agents)
                (Server/endServe)
                (clojure.java.io/delete-file dummy-file-name)
                )
            )
        )
    )