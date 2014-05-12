(ns system.mturk.response-converter
    (:require [clojure.java.io :as io])
    (:require [clojure.data.json :as json])
    (:import (java.io FileWriter File FileReader)
             (input.csv CSVLexer CSVParser)
             (survey Survey Question Component)))

(def mturk-headers '(HitId HitTitle Annotation AssignmentId WorkerId Status AcceptTime SubmitTime))
(def output-headers '(responseid workerid surveyid questionid questiontext questionpos optionid optiontext optionpos))
(def workerid-index 4)
(def srid (atom 0))
;; utility to take the csv output of mturk and convert it to our csv output

(defn print-headers
    [^FileWriter w filename]
    (when-not (and (.exists (File. filename))
                   (not (empty? (line-seq (io/reader filename)))))
        (.write w (str (clojure.string/join "," output-headers) "\n"))
        )
    )

(defn -main
    [& args]
    (let [filename (first args)
          ^Survey s (-> (second args)
                     (CSVLexer. ",")
                     (CSVParser.)
                     (.parse))
          output-filename "results.csv"
          ]
        (with-open [w (io/writer output-filename)]
            (print-headers w output-filename)
            (with-open [r (io/reader filename)]
                (doseq [line (rest (line-seq r))]
                    (let [entry (clojure.string/split line #",")
                          mth (take (count mturk-headers) entry)
                          answers (try (json/read-str (str "[" (clojure.string/replace
                                                                   (->> entry
                                                                        (drop (count mturk-headers))
                                                                        (remove #(let [thing (read-string %)]
                                                                                    (and (string? thing)
                                                                                         (.endsWith thing "csv"))))

                                                                        (clojure.string/join ","))
                                                              "\"\"" "'")
                                                    "]"))
                                                      (catch Exception e (do (println entry)
                                                                             (.printStackTrace e)
                                                                             (System/exit 1)))

                                                      )
                          srid (str "sr" (swap! srid inc))
                          ]
                        (doseq [resp answers]
                            (if (string? resp)
                                (doseq [ans (clojure.string/split resp #"\|")]
                                    (let [qmap (json/read-str (clojure.string/replace ans "'" "\""))
                                          questionid (qmap "quid")
                                          ^Question q (.getQuestionById s questionid)
                                          questiontext (.data q)
                                          questionpos (qmap "qpos")
                                          optionid (qmap "oid")
                                          ^Component o (.getOptById q (qmap "oid"))
                                          optiontext (.data o)
                                          optionpos (qmap "opos")
                                          write-me (format "%s,%s,,%s,%s,%s,%s,%s,%s\n"
                                                           srid
                                                           (nth mth workerid-index)
                                                           questionid questiontext questionpos optionid optiontext optionpos)]
                                        (println write-me)
                                        (.write w write-me)
                                        )
                                    )
                                (let [write-me (format "%s,%s,,%s,%s,%s,%s,%s,%s\n"
                                                       srid
                                                       (nth mth workerid-index)
                                                       "q_-1_-1" (str resp) "-1" "comp_-1_-1" (str resp) "-1")]
                                    (println write-me)
                                    (.write w write-me)
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    )