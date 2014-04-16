(defproject edu.umass/surveyman "1.5"
  :description "SurveyMan is a programming language and runtime system for designing, debugging, and deploying surveys on the web at scale."
  :url "http://surveyman.org"
  :repositories [["java.net" "http://download.java.net/maven/2"]]
  :dependencies [[incanter "1.5.4"]]
  :source-paths ["src/main/clojure"]
  :java-source-paths ["src/main/java" "lib/aws-mturk-dataschema.jar" "lib/aws-mturk-wsdl.jar" "lib/java-aws-mturk.jar"]
  :test-paths ["src/main/test"]
  :resource-paths ["src/main/resource"]
  )