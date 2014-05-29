(ns testPackage
  (:use testLog)
  (:use clojure.test)
  (:use clojure.java.shell)
  )

(deftest testPackage
  ;; will need a more general version for future releases
  (println 'making-package)
  (sh "make" "package")
  ;(doseq [a [(agent (Thread. (fn [] (sh "java" "-jar" "target/surveyman-1.5-standalone.jar" "--backend=LOCALHOST" "data/tests/pick_randomly.csv"))))
  ;           (agent (Thread. (fn [] (sh "java" "-jar" "target/surveyman-1.5-standalone.jar" "--backend=MTURK" "data/tests/pick_randomly.csv"))))
  ;           (agent (Thread. (fn [] (sh "java" "-cp" "target/surveyman-1.5-standalone.jar" "Report" "--report=static" "data/tests/pick_randomly.csv"))))
  ;           (agent (Thread. (fn [] (sh "java" "-cp" "target/surveyman-1.5-standalone.jar" "Report" "--report=dynamic" "--results=data/results/prototypicality" "data/samples/prototypicality.csv"))))]]
  ;  (try
  ;    (send a #(.start %))
  ;    (Thread/sleep 10000)
  ;    (catch Exception e (println (.getMessage e)))
  ;    (finally (shutdown-agents))
  ;    )
  ;  (println 'success)
  ;  )
  )
