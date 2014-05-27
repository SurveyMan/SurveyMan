(ns util
  (:import (util ArgReader)
           (net.sourceforge.argparse4j ArgumentParsers)
           (net.sourceforge.argparse4j.inf ArgumentParser Argument)
           (java.util Map))
  )

(defn make-arg-parser
  [^String clz]
  (let [^ArgumentParser argument-parser (-> (ArgumentParsers/newArgumentParser clz true "-")
                                            (.description "Performs static and dynamic analyses on result files."))]
    (-> argument-parser
        (.addArgument (into-array String ["survey"]))
        (.required true))
    (doseq [[arg _] (seq (.entrySet ^Map (ArgReader/getMandatoryAndDefault (Class/forName clz))))]
      (-> argument-parser
          (.addArgument (into-array String [(str "--" arg)]))
          (.required true)
          (.help (ArgReader/getDescription arg)))
      )
    (doseq [[arg defVal] (seq (.entrySet ^Map (ArgReader/getOptionalAndDefault (Class/forName clz))))]
      (-> argument-parser
          (.addArgument (into-array String [(str "--" arg)]))
          (.required false)
          (.setDefault defVal)
          (.help (ArgReader/getDescription arg)))
      )
    argument-parser
    )
  )
