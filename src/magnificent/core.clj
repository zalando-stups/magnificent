; contains magnificent bootstrapping and request handling
(ns magnificent.core
  (:require [org.httpkit.server :as httpkit]
            [cheshire.core :as json]
            [clojure.java.io :as io])
  (:gen-class))

(defn json-response [status message & {:keys [payload]}]
  {:status  status
   :headers {"Content-Type" "application/json"}
   :body    (json/generate-string
              {:message message
               :payload payload})})

(defn authorize
  "Processes each request and returns an authorization decision."
  [request rules]
  (let [rule-key [(:api request) (keyword (:http-method request)) (:http-path-key request)]
        rule (get rules rule-key)]
    (if rule
      (try
        (rule request)
        (catch Exception e
          ))
      (json-response 400 "Rule for API endpoint not defined."))))

(defn load-rules
  "Loads the ruleset with the given name and returns the initilized configuration object."
  [ruleset]
  (let [ruleset (symbol ruleset)]
    (require [ruleset])
    (symbol ruleset "rules")))

(defn ring-handler
  "Routes requests to this http endpoint to its correct implementation."
  [rules]
  (fn [request]
    (if (and (= :post (:request-method request))
             (= "/stups-api/authorize" (:uri request)))

      (-> request
          :body
          io/reader
          json/parse-stream
          (authorize rules))

      (json-response 404 "Unknown endpoint."))))

(defn -main
  "The application first loads the given 'ruleset' and starts the HTTP endpoint afterwards."
  [& args]
  (let [ruleset (or (System/getenv "RULESET") "radical-agility")
        rules (load-rules ruleset)]
    (httpkit/run-server
      (ring-handler rules)
      {:port 8080})
    (println "Listening on :8080 for requests on POST /stups-api/authorize using ruleset \"" ruleset "\"")))
