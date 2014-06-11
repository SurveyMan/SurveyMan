 (ns testDropdown
     (:import (survey Survey Question Component)
              (interstitial Record BoxedBool AbstractResponseManager ISurveyResponse OptTuple IQuestionResponse)
              (system.localhost LocalLibrary Server)
              (system Runner)
              (util Slurpie)
              (system.localhost.generators LocalHTML)
              (system.generators HTML))
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
          ^Record record (Record. survey lib bt)
          ^String url (sm-get-url record)
          ^BoxedBool interrupt (BoxedBool.)
          runner (Thread. (fn [] (do (Runner/init bt) (Runner/run record))))
          response-getter (Runner/makeResponseGetter survey)
          ]
      ; start up survey
      (AbstractResponseManager/putRecord survey record)
      (HTML/spitHTMLToFile (HTML/getHTMLString record (LocalHTML.)) survey)
      (assert (not= (count (Slurpie/slurp (.getHtmlFileName record))) 0))
      (Server/startServe)
      (.start runner)
      (.start response-getter)
      (while (= 0 (count (clojure.string/trim (Slurpie/slurp (.getHtmlFileName record)))))
        (Thread/sleep 2000))
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
        (while (empty? (.validResponses record))
          (print ".")
          (Thread/sleep 100)
          )
        (.setInterrupt interrupt true "Finished test")
        (let [^ISurveyResponse response (first (.validResponses record))
              ^Component ans (.c (first (.getOpts ^IQuestionResponse
                                 (first (vals (.resultsAsMap response))))))]
          (is (= ans answer)))
        (quit driver)
        (.join runner)
        (.join response-getter)
        (Server/endServe)
        (clojure.java.io/delete-file dummy-file-name)
        )
      )
    )
  )
