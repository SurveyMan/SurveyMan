 (ns testDropdown
     (:import (survey Survey Question Component)
              (interstitial Record BoxedBool AbstractResponseManager ISurveyResponse OptTuple IQuestionResponse)
              (system.localhost LocalLibrary Server)
              (system Runner))
     (:use testLog)
     (:use clojure.test)
     (:use clj-webdriver.taxi))

(deftest testDropdown
    (let [csv (str "QUESTION,OPTIONS\nfoo,0\n," (clojure.string/join "\n," (range 1 20)))
          dummy-file-name "bar.csv"]
        ;(println csv)
        (spit dummy-file-name csv)
        (let [^Survey survey (makeSurvey dummy-file-name ",")
              ^Question q (first (.questions survey))
              [answer & others] (shuffle (vals (.options q)))
              ^Record record (Record. survey (LocalLibrary.) bt)
              ^String url (sm-get-url record)
              ^BoxedBool interrupt (BoxedBool. false)
              runner (agent (fn [] (do (Runner/init bt) (Runner/run record interrupt))))
              response-getter (agent (Runner/makeResponseGetter survey interrupt bt))
              ]
            ; start up survey
            (AbstractResponseManager/putRecord survey record)
            (Thread/sleep 2000)
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
                (doseq [^Component other-ans others]
                    (select (find-element driver {:id (.getCid other-ans)}))
                    (is (find-element driver {:id SUBMIT_FINAL}))
                    )
                ; select the CHOOSE ONE
                (select (find-element driver {:id DUMMY_ID}))
                (is (not (find-element driver {:id (str NEXT_PREFIX (.quid q))})))
                (is (not (find-element driver {:id (str SUBMIT_PREFIX (.quid q))})))
                (is (not (find-element driver {:id SUBMIT_FINAL})))
                ; select the actual answer
                (select (find-element driver {:id (.getCid answer)}))
                ; submit
                (submit driver (str "#" SUBMIT_FINAL))
                ; verify that the answer is the same
                (while (empty? (.responses record))
                    (println ".")
                    (Thread/sleep 1000)
                    )
                (.setInterrupt interrupt true "Finished test")
                (let [^ISurveyResponse response (first (.responses record))
                      ^Component ans (.c (first (.getOpts ^IQuestionResponse
                                                          (first (vals (.resultsAsMap response))))))]
                    (is (= ans answer))
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
